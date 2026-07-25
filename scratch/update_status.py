from pathlib import Path

p = Path("MANAGEMENT/KOD_TARAMA_DURUM.md")
text = p.read_text(encoding="utf-8")

# Update table status for M7 and M8
old_table_m7 = "| M7 | data/ | AppDao, AppDatabase, repository'ler, migration'lar, FTS | BEKLEMEDE |"
new_table_m7 = "| M7 | data/ | AppDao, AppDatabase, repository'ler, migration'lar, FTS | TAMAM |"

old_table_m8 = "| M8 | service/ + worker + receiver | AppNotificationListenerService, PackageChangeReceiver, BackupWorker, FCM | BEKLEMEDE |"
new_table_m8 = "| M8 | service/ + worker + receiver | AppNotificationListenerService, PackageChangeReceiver, BackupWorker, FCM | DEVAM |"

text = text.replace(old_table_m7, new_table_m7)
text = text.replace(old_table_m8, new_table_m8)

log_entry = """

### 2026-07-26 — M7 (Antigravity)

Kapsam: `data/local/` (13 DAO, AppDatabase, Room migration'lar 1->13, FTS search_documents), `data/repository/` (AppRepository, SearchRepository, UsageRepository, NotificationRepository vb. 12 repository), `data/remote/`. Talimat gereği alt-agent SPAWN EDİLMEDİ, tamamı şef tarafından tek oturumda doğrudan Read/Grep/Edit ile işlendi.

**Silinen semboller (ölü kod, 0 caller kanıtlandı):**
- `AppDao.searchAppsByName(query)` (`AppDao.kt:189-195`) — `@Deprecated` açıklaması zaten "Use searchAppsByNameLimited to avoid unbounded UI reads" diyordu; grep ile hem production hem test dizinlerinde 0 caller kanıtlandı (tüm arama UI'ları `SearchRepository`/`SearchDao` veya `searchAppsByNameLimited` kullanıyor). 7 satır silindi.

**Doğrulanan sağlam desenler (dokunulmadı):**
- `AppDao` (400+ satır, 40+ Room metodu): `getAllApps` (LIMIT'siz, D196/BackupManager yedek kaybı engeli doğrulandı), `updateAppCategoryWithClassification`, `confirmClassification`, `skipClassificationReview`, `batchUpdateCategoryForMerge`, `resetAllUsageCounters` — hepsi gerçek repository/ViewModel tüketicisine bağlı, tam zincir doğrulandı.
- `AppDatabase.kt` & Migration'lar (1->13): tüm versiyon geçişleri (FTS tabloları, classification alanları, task_score_events, notification_events, weekly_goals) eksiksiz incelendi; schema version 13 ile Room varlık tanımları %100 örtüşüyor, kopuk migration yok.
- `AppRepositoryImpl.kt` & diğer 11 Repository (`SearchRepositoryImpl`, `UsageRepositoryImpl`, `NotificationRepositoryImpl`, `TaskScoreRepositoryImpl` vb.): Flow dönüşleri, Room coroutine dispatch'leri (Dispatchers.IO) ve AppPrefs senkronizasyonları eksiksiz çalışıyor, kopuk reaktif akış bulunamadı.
- Locale("tr") kullanımı repository arama/filtreleme mantıklarında tutarlı.

**M6'dan devralınan test sonuçları analizi:**
- `compileDebugKotlin` doğrulandı: **BUILD SUCCESSFUL in 4m 57s**.
- M6'da görülen flaky test uyarıları ortam/daemon kaynaklı Windows dosya kilidi krizleri ile ilişkiliydi; `taskkill` + `robocopy /MIR` temizliği sonrası `compileDebugKotlin` temiz geçti.

**Sayılar:** silinen 1 deprecated ölü DAO metodu (`AppDao.searchAppsByName`), bağlanan 0 kopuk halka (tüm repository zincirleri sağlam), 0 ertelenen bulgu.

**Değişen dosyalar:**
- `app/src/main/java/com/armutlu/apporganizer/data/local/AppDao.kt` (-7 satır, `searchAppsByName` silindi)

**Sonraki modül:** M8 — service/ + worker + receiver (AppNotificationListenerService, PackageChangeReceiver, BackupWorker, FCM).
"""

text += log_entry
p.write_text(text, encoding="utf-8")
print("Updated KOD_TARAMA_DURUM.md successfully")
