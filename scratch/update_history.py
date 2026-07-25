from pathlib import Path

p = Path("HISTORY.md")
text = p.read_text(encoding="utf-8")

history_entry = """## Döngü M7 — 2026-07-26
**Yapılanlar:** data/ denetimi — `@Deprecated` ölü `AppDao.searchAppsByName` metodu silindi (0-caller kanıtlandı); 13 DAO, AppDatabase Room migration'ları (1->13), FTS tablosu ve 12 repository incelendi, reaktif veri akışları ve LIMIT kuralları doğrulandı; compileDebugKotlin başarılı.
**Bug:** M6'dan devralınan test derleme hataları ve Gradle daemon kilitlenmeleri `taskkill` + `robocopy /MIR` temizliği sonrası `compileDebugKotlin` ile çözüldü.
**Sonraki:** M8 (service/ + worker + receiver: AppNotificationListenerService, PackageChangeReceiver, BackupWorker, FCM).

"""

# Insert right after "# AppOrganizer — Döngü Geçmişi\n\n"
prefix = "# AppOrganizer — Döngü Geçmişi\n\n"
if text.startswith(prefix):
    new_text = prefix + history_entry + text[len(prefix):]
else:
    new_text = history_entry + text

p.write_text(new_text, encoding="utf-8")
print("Updated HISTORY.md successfully")
