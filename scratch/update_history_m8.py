from pathlib import Path

p = Path("HISTORY.md")
text = p.read_text(encoding="utf-8")

history_entry = """## Döngü M8 — 2026-07-26
**Yapılanlar:** service/ + worker + receiver denetimi — AppNotificationListenerService (reaktif badge/preview), PackageChangeReceiver (EX01 backoff fix), BackupWorker (SAF/Drive kopyalama), MissionSettlementWorker (zincirleme döngü) ve 6 diğer Worker incelendi; FCM servisinin önceden temizlendiği doğrulandı; Manifest bildirimleri eksiksiz.
**Bug:** Yok — tüm servis ve arka plan işçileri aktif tüketim zincirlerine sahip, compileDebugKotlin yeşil.
**Sonraki:** M9 (Aktiviteler + navigasyon: MainActivity, LauncherActivity, Routes, onboarding).

"""

prefix = "# AppOrganizer — Döngü Geçmişi\n\n"
if text.startswith(prefix):
    new_text = prefix + history_entry + text[len(prefix):]
else:
    new_text = history_entry + text

p.write_text(new_text, encoding="utf-8")
print("Updated HISTORY.md for M8 successfully")
