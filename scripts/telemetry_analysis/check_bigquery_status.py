"""
BigQuery telemetri veri durumu kontrolu (P0 - baglanti/veri varligi dogrulamasi).

Amac: Firebase Analytics + Crashlytics BigQuery export'unun canli veri uretip
uretmedigini kontrol eder. Veri henuz olusmamis olabilir (export sonrasi ilk 24
saat), bu NORMAL bir durumdur - script hata vermeden "veri yok, X saat sonra
tekrar dene" der.

Kullanim:
    python scripts/telemetry_analysis/check_bigquery_status.py

Gereksinimler:
    pip install google-cloud-bigquery
    .env dosyasinda GOOGLE_APPLICATION_CREDENTIALS + FIREBASE_PROJECT_ID tanimli olmali.
"""
from __future__ import annotations

import os
import sys
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


def main() -> int:
    project_root = Path(__file__).resolve().parents[2]
    env = load_env(project_root / ".env")

    project_id = env.get("FIREBASE_PROJECT_ID")
    creds_path = env.get("GOOGLE_APPLICATION_CREDENTIALS")
    if not project_id or not creds_path:
        print("HATA: .env dosyasinda FIREBASE_PROJECT_ID veya GOOGLE_APPLICATION_CREDENTIALS eksik.")
        return 1

    # Relative path ise proje kokune gore coz.
    creds_full_path = (project_root / creds_path).resolve() if not os.path.isabs(creds_path) else Path(creds_path)
    if not creds_full_path.exists():
        print(f"HATA: Servis hesabi dosyasi bulunamadi: {creds_full_path}")
        return 1

    os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = str(creds_full_path)

    try:
        from google.cloud import bigquery
        from google.api_core.exceptions import Forbidden, GoogleAPIError
    except ImportError:
        print("HATA: google-cloud-bigquery kurulu degil. Calistir: pip install google-cloud-bigquery")
        return 1

    client = bigquery.Client(project=project_id)
    print(f"[OK] BigQuery client olusturuldu (proje: {project_id})")

    # 1. Sorgu calistirma izni testi (bigquery.jobs.create).
    try:
        result = client.query("SELECT 1 AS ping").result()
        list(result)
        print("[OK] Sorgu calistirma izni (bigquery.jobs.create) dogrulandi.")
    except Forbidden as e:
        print("[HATA] Sorgu calistirma izni YOK (bigquery.jobs.create).")
        print(f"       Detay: {str(e)[:300]}")
        print("       Cozum: Cloud Console > IAM & Admin > IAM > servis hesabina")
        print("       'BigQuery Job User' rolu ekle.")
        return 1

    # 2. Dataset listeleme (Analytics/Crashlytics export'lari genelde
    #    'analytics_<property_id>' ve 'firebase_crashlytics' gibi isimlerle gelir).
    datasets = list(client.list_datasets())
    if not datasets:
        print("[BILGI] Henuz hicbir BigQuery dataset'i yok.")
        print("        Bu NORMAL olabilir - Analytics/Crashlytics export'u ilk 24 saat")
        print("        icinde veri uretmeyebilir. Export'u ne zaman actiysan o zamandan")
        print("        itibaren 24 saat bekleyip tekrar calistir.")
        print()
        print("[SONUC] Baglanti/izin SAGLIKLI, veri HENUZ YOK. Sistem calisir durumda.")
        return 0

    print(f"[OK] {len(datasets)} dataset bulundu:")
    analytics_datasets = []
    crashlytics_datasets = []
    other_datasets = []
    for ds in datasets:
        name = ds.dataset_id
        print(f"     - {name}")
        if name.startswith("analytics_"):
            analytics_datasets.append(name)
        elif "crashlytics" in name.lower():
            crashlytics_datasets.append(name)
        else:
            other_datasets.append(name)

    print()
    if analytics_datasets:
        ds_id = analytics_datasets[0]
        tables = list(client.list_tables(f"{project_id}.{ds_id}"))
        print(f"[OK] Analytics dataset'i bulundu: {ds_id} ({len(tables)} tablo)")
        for t in tables[:5]:
            print(f"     - {t.table_id}")
    else:
        print("[BILGI] Analytics dataset'i ('analytics_*') henuz yok.")

    if crashlytics_datasets:
        ds_id = crashlytics_datasets[0]
        tables = list(client.list_tables(f"{project_id}.{ds_id}"))
        print(f"[OK] Crashlytics dataset'i bulundu: {ds_id} ({len(tables)} tablo)")
        for t in tables[:5]:
            print(f"     - {t.table_id}")
    else:
        print("[BILGI] Crashlytics dataset'i henuz yok.")

    print()
    print("[SONUC] Baglanti/izin SAGLIKLI. Yukaridaki dataset/tablo listesine gore devam edilebilir.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
