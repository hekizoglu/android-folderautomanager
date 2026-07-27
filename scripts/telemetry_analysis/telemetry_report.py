"""
AppOrganizer - 8 saatte bir calisan Firebase telemetri analiz raporu.

Amac: Crashlytics (crash/ANR/non-fatal) ve Analytics (kullanim event'leri) verisini
BigQuery uzerinden okuyup Telegram'a Turkce ozet gonderir. Veri henuz olusmamis
tablolar/dataset'ler icin HATA VERMEZ - "henuz veri yok" der ve sessizce devam eder
(export'lar zamanla veri uretecek, script kalici olarak calisir).

Kullanim (manuel test):
    python scripts/telemetry_analysis/telemetry_report.py

Cron/loop entegrasyonu icin ScheduleWakeup ile 8 saatte bir bu script cagirilir.

Gereksinimler:
    pip install google-cloud-bigquery requests
    .env: FIREBASE_PROJECT_ID, GOOGLE_APPLICATION_CREDENTIALS, TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID
"""
from __future__ import annotations

import os
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path


def load_env(env_path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    if not env_path.exists():
        return values
    for line in env_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        values[key.strip()] = value.strip()
    return values


def resolve_creds_path(project_root: Path, creds_path: str) -> Path:
    return (project_root / creds_path).resolve() if not os.path.isabs(creds_path) else Path(creds_path)


def query_crashlytics_summary(client, project_id: str, package_name: str, lookback_hours: int) -> dict:
    """firebase_crashlytics.<package>_ANDROID tablosundan son N saatteki hata ozetini doner.

    Tablo/dataset henuz yoksa (export ilk veriyi henuz uretmedi) None doner, hata firlatmaz.
    """
    from google.api_core.exceptions import NotFound

    table_name = package_name.replace(".", "_") + "_ANDROID"
    dataset_ref = f"{project_id}.firebase_crashlytics"

    try:
        client.get_table(f"{dataset_ref}.{table_name}")
    except NotFound:
        return {"available": False, "reason": f"Tablo henuz yok: {dataset_ref}.{table_name}"}

    query = f"""
        SELECT
          issue_id,
          issue_title,
          is_fatal,
          COUNT(*) AS occurrence_count,
          MAX(event_timestamp) AS last_seen
        FROM `{dataset_ref}.{table_name}`
        WHERE event_timestamp >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL {lookback_hours} HOUR)
        GROUP BY issue_id, issue_title, is_fatal
        ORDER BY occurrence_count DESC
        LIMIT 10
    """
    try:
        rows = list(client.query(query).result())
    except Exception as e:
        return {"available": False, "reason": f"Sorgu hatasi: {str(e)[:200]}"}

    return {
        "available": True,
        "total_issues": len(rows),
        "top_issues": [
            {
                "title": r.issue_title,
                "fatal": r.is_fatal,
                "count": r.occurrence_count,
            }
            for r in rows
        ],
    }


def query_analytics_summary(client, project_id: str, lookback_hours: int) -> dict:
    """analytics_<property_id>.events_* tablolarindan son N saatteki en cok gorulen event'leri doner.

    Property ID onceden bilinmedigi icin dataset'i otomatik bulur (analytics_ ile baslayan ilk dataset).
    """
    datasets = [d.dataset_id for d in client.list_datasets() if d.dataset_id.startswith("analytics_")]
    if not datasets:
        return {"available": False, "reason": "Henuz 'analytics_*' dataset'i yok (export ilk veriyi uretmedi)."}

    dataset_id = datasets[0]
    today = datetime.now(timezone.utc)
    yesterday = today - timedelta(days=1)
    # events_YYYYMMDD tablolari gunluk olusur; son 2 gunu tarayip lookback_hours ile filtrele.
    table_suffix_today = today.strftime("%Y%m%d")
    table_suffix_yesterday = yesterday.strftime("%Y%m%d")

    query = f"""
        SELECT
          event_name,
          COUNT(*) AS event_count
        FROM `{project_id}.{dataset_id}.events_*`
        WHERE _TABLE_SUFFIX IN ('{table_suffix_today}', '{table_suffix_yesterday}')
          AND TIMESTAMP_MICROS(event_timestamp) >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL {lookback_hours} HOUR)
        GROUP BY event_name
        ORDER BY event_count DESC
        LIMIT 15
    """
    try:
        rows = list(client.query(query).result())
    except Exception as e:
        return {"available": False, "reason": f"Sorgu hatasi (tablo henuz olusmamis olabilir): {str(e)[:200]}"}

    return {
        "available": True,
        "dataset": dataset_id,
        "top_events": [{"name": r.event_name, "count": r.event_count} for r in rows],
    }


def build_report_text(crash_summary: dict, analytics_summary: dict, lookback_hours: int) -> str:
    lines = [f"AppOrganizer Telemetri Raporu (son {lookback_hours} saat)", ""]

    lines.append("--- CRASH / HATA RAPORU ---")
    if not crash_summary.get("available"):
        lines.append(f"Veri yok: {crash_summary.get('reason', 'bilinmiyor')}")
    elif crash_summary["total_issues"] == 0:
        lines.append("Bu donemde hic crash/hata kaydi yok. Temiz.")
    else:
        lines.append(f"{crash_summary['total_issues']} farkli sorun tespit edildi:")
        for issue in crash_summary["top_issues"][:5]:
            kind = "FATAL" if issue["fatal"] else "non-fatal"
            lines.append(f"  - [{kind}] {issue['title']} ({issue['count']} kez)")

    lines.append("")
    lines.append("--- KULLANIM VERISI ---")
    if not analytics_summary.get("available"):
        lines.append(f"Veri yok: {analytics_summary.get('reason', 'bilinmiyor')}")
    elif not analytics_summary["top_events"]:
        lines.append("Bu donemde event kaydi yok.")
    else:
        lines.append(f"Dataset: {analytics_summary['dataset']}")
        lines.append("En cok gorulen event'ler:")
        for ev in analytics_summary["top_events"][:10]:
            lines.append(f"  - {ev['name']}: {ev['count']}")

    return "\n".join(lines)


def send_telegram_message(bot_token: str, chat_id: str, text: str) -> bool:
    import requests

    url = f"https://api.telegram.org/bot{bot_token}/sendMessage"
    resp = requests.post(url, data={"chat_id": chat_id, "text": text}, timeout=30)
    return resp.ok and resp.json().get("ok", False)


def main() -> int:
    project_root = Path(__file__).resolve().parents[2]
    env = load_env(project_root / ".env")

    project_id = env.get("FIREBASE_PROJECT_ID")
    creds_path = env.get("GOOGLE_APPLICATION_CREDENTIALS")
    bot_token = env.get("TELEGRAM_BOT_TOKEN")
    chat_id = env.get("TELEGRAM_CHAT_ID")

    if not all([project_id, creds_path, bot_token, chat_id]):
        print("HATA: .env dosyasinda gerekli degiskenlerden biri eksik "
              "(FIREBASE_PROJECT_ID, GOOGLE_APPLICATION_CREDENTIALS, TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID).")
        return 1

    creds_full_path = resolve_creds_path(project_root, creds_path)
    if not creds_full_path.exists():
        print(f"HATA: Servis hesabi dosyasi bulunamadi: {creds_full_path}")
        return 1
    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = str(creds_full_path)

    try:
        from google.cloud import bigquery
    except ImportError:
        print("HATA: google-cloud-bigquery kurulu degil.")
        return 1

    client = bigquery.Client(project=project_id)
    package_name = "com.armutlu.apporganizer"
    lookback_hours = 8

    print(f"[{datetime.now(timezone.utc).isoformat()}] Telemetri raporu hazirlaniyor...")

    crash_summary = query_crashlytics_summary(client, project_id, package_name, lookback_hours)
    analytics_summary = query_analytics_summary(client, project_id, lookback_hours)

    report_text = build_report_text(crash_summary, analytics_summary, lookback_hours)
    print(report_text)

    sent = send_telegram_message(bot_token, chat_id, report_text)
    if sent:
        print("[OK] Telegram'a gonderildi.")
        return 0
    else:
        print("[HATA] Telegram gonderimi basarisiz.")
        return 1


if __name__ == "__main__":
    sys.exit(main())
