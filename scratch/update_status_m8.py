from pathlib import Path

p = Path("MANAGEMENT/KOD_TARAMA_DURUM.md")
text = p.read_text(encoding="utf-8")

# Update table status for M8 and M9
old_table_m8 = "| M8 | service/ + worker + receiver | AppNotificationListenerService, PackageChangeReceiver, BackupWorker, FCM | DEVAM |"
new_table_m8 = "| M8 | service/ + worker + receiver | AppNotificationListenerService, PackageChangeReceiver, BackupWorker, FCM | TAMAM |"

old_table_m9 = "| M9 | Aktiviteler + navigasyon | MainActivity, LauncherActivity, Routes, onboarding | BEKLEMEDE |"
new_table_m9 = "| M9 | Aktiviteler + navigasyon | MainActivity, LauncherActivity, Routes, onboarding | DEVAM |"

text = text.replace(old_table_m8, new_table_m8)
text = text.replace(old_table_m9, new_table_m9)

log_entry = """

### 2026-07-26 — M8 (Antigravity)

Kapsam: `service/` (`AppNotificationListenerService`, `NotificationPreviewStore`), `workers/` (7 WorkManager worker'ı: `BackupWorker`, `CategoryDbUpdateWorker`, `MissionSettlementWorker`, `SmartInsightWorker`, `SuggestionNotificationWorker`, `TickerHistoryCleanupWorker`, `WeeklyDigestWorker`), `receivers/` (`PackageChangeReceiver`, `AppUpdateReceiver`), `data/remote/BackupSyncService.kt` ve `AndroidManifest.xml` bileşen bildirimleri. Talimat gereği alt-agent SPAWN EDİLMEDİ, tamamı şef tarafından tek oturumda doğrudan Read/Grep/Edit ile işlendi.

**Silinen semboller (ölü kod):**
- Bu modülde ölü service/worker/receiver bulunamadı. FCM servisinin önceden projeden tamamen kaldırıldığı (D-S6 kararı) ve yerine `CategoryDbUpdateWorker` periyodik haftalık güncellemesinin çalıştığı doğrulandı.

**Doğrulanan sağlam desenler (dokunulmadı):**
- `AppNotificationListenerService.kt`: `onNotificationPosted`/`onNotificationRemoved` ve `rebuildCounts()` / `updatePreviewState()` reaktif `StateFlow` zinciri tam incelendi; `AppPrefs.isNotificationTextEnabled` ve `getNotificationPreviewBlockedPackages` gizlilik filtreleri tutarlı çalışıyor.
- `PackageChangeReceiver.kt`: `goAsync()` + `Dispatchers.IO` kullanımı, `isReplacing` (güncelleme vs ilk yükleme) ayrımı ve `NewAppNotifier` bildirimi doğrulandı. `onPackageAdded` içindeki 3-denemeli backoff ile `getAppInfo` null-race engelleyici (EX01 bugı) doğrulandı.
- `BackupWorker.kt`: 7 günlük periyodik çalışma, SAF/Drive Uri kopyalama (`copyBackupToDrive`) ve `WorkerTelemetryPrefs` metrik takibi sağlam.
- `MissionSettlementWorker.kt`: Gece yarısı/hafta başında tek seferlik çalışıp zincirleme olarak `MissionWorkScheduler.scheduleNext()` ile bir sonraki döneme kendini planlama mantığı doğrulandı.
- `CategoryDbUpdateWorker`, `SmartInsightWorker`, `SuggestionNotificationWorker`, `WeeklyDigestWorker`, `TickerHistoryCleanupWorker`: `AppOrganizerApp.onCreate()` ve `BackupManager`/UI tetiklemeleriyle doğru planlanıyor.
- `BackupSyncService.kt`: `START_NOT_STICKY` + `stopSelf()` ile servis çökme engeli korundu.
- `AndroidManifest.xml`: Tüm servis ve receiver bildirimleri (BIND_NOTIFICATION_LISTENER_SERVICE, PACKAGE_ADDED vb. intent-filter'lar) tam eşleşiyor.

**Build:**
- `compileDebugKotlin` doğrulandı: **BUILD SUCCESSFUL in 44s**, 0 hata.

**Sayılar:** silinen 0 sembol (tüm servis/worker/receiver bileşenleri aktif ve gerekçeli), bağlanan 0 kopuk halka, 0 ertelenen bulgu.

**Sonraki modül:** M9 — Aktiviteler + navigasyon (MainActivity, LauncherActivity, Routes, onboarding).
"""

text += log_entry
p.write_text(text, encoding="utf-8")
print("Updated KOD_TARAMA_DURUM.md for M8 successfully")
