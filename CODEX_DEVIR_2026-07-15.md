# Codex Devir Notu — AI Denetim Roadmap'i (2026-07-15)

> Claude oturumu kota nedeniyle burada durdu. Kalan işler bu dokümanla Codex'te devam edecek.
> Ana iş listesi: `ROADMAP_AI_AUDIT_2026-07-14.md` (madde detayları orada, durumlar güncel).

## Mevcut Durum (main = b755690, push'lu, temiz)

### ✅ Tamamlananlar (8/26)
| Madde | Commit | Özet |
|---|---|---|
| P0.1 | bb46943 | Klasör içi kategori değiştirme — picker state ekran köküne taşındı |
| P0.2 | 43b54bb | `ClassificationAttentionPolicy` — sayaç/liste/dashboard tek kaynak, 6 nedenli enum |
| P0.3 | 684879d | `FileIndexState` sealed modeli — izin/indeks durumu ayarlar+aramada görünür |
| P0.4 | a7ad7de | Kapsam seçimli sıfırlama sihirbazı (`StatsResetService`) |
| P0.5 | ffbb7cb | Okunmamış bildirim modeli (`NotificationReadPrefs` + `UnreadNotificationModel`) |
| P0.6 | d4705c8 | `ClassificationMode` enum (4 mod) + migration; insertApps toggle bug'ı fix |
| P0.7 | cec03d6 | `CategorySuggestionEngine` — keyword→vendor→benzer paket önerisi |
| P1.3 | b755690 | Kişi öneri altyapısı (`ContactActionPrefs` + `ContactSuggestionEngine` + `LauncherViewModel.suggestedContacts`) — UI'sız, P1.2 tüketecek |

Sürüm: v1.3.26 (versionCode 49). Sprint 1-2 APK'ları Telegram'da.

### ⏳ Kalanlar (18 madde) — önerilen sıra
1. **P1.1 — Tam ekran arama** (DİKKAT: bir agent denemesi content-filter kesintisiyle yarıda kaldı; yarım iş ATILDI, temiz başla). Plan: `FullScreenSearchOverlay.kt` overlay (AllAppsDrawer pattern'i), mevcut `searchResultsSection` veri kaynaklarını yeniden kullan, `KEY_FULLSCREEN_SEARCH` toggle, kök BackHandler kuralını bozma (LEARNINGS D272).
2. **P1.2 — Bağlamsal sıfır durum**: P1.1 overlay'inin boş-sorgu ekranı. Hazır girdiler: `LauncherViewModel.suggestedContacts` (P1.3), `UsageStatsHelper.getCurrentSlotTopApps` (saat bazlı 5 uygulama), arama geçmişi için yeni küçük prefs (sınırlı+temizlenebilir).
3. P1.4-P1.6 (görev V2 Room'a taşıma, skor katkı sınırı ±10, kart ayrıştırma)
4. P1.7-P1.10 (hava durumu, katalog cache/olay bazlı yenileme, TXT tanılama raporu, bildirim önizleme)
5. P2.1-P2.9 (varsayılanlar, blur kaldırma, 5'li dock, rapor sadeleştirme, öneri kanalı, animasyon V2, ana ekran hiyerarşisi)

## Çalışma Kuralları (CLAUDE.md'den kritik özet)
- **Kalite kapısı (her maddede zorunlu):**
  `.\gradlew testDebugUnitTest -PskipGoogleServices` + `compileDebugKotlin` + `assembleDebug` — geçmeden "tamamlandı" deme.
- **Her build'de versiyon bump:** `app/build.gradle.kts` versionCode +1, versionName PATCH +1.
- Tüm kullanıcı metinleri `values/strings.xml` + `values-en/strings.xml` (TR+EN). UTF-8, curly quote yasak.
- Room migration'dan kaçın — kalıcılık için SharedPreferences pattern'leri tercih (örnekler: `MissionPrefs`, `NotificationReadPrefs`, `ContactActionPrefs`).
- Ayar eklerken: `AppPrefs.kt` KEY + getter/setter → Settings toggle → `BackupManager` export/import.
- Her madde: bağımsız commit + HISTORY.md döngü girişi + Telegram raporu (`.env` → `TELEGRAM_BOT_TOKEN`/`TELEGRAM_CHAT_ID`, sendMessage; sprint sonu APK sendDocument).

## Ortam Tuzakları (bu oturumda yaşananlar)
- **Build kilidi:** `AccessDeniedException`/`Unable to delete` → `Get-Process java | Stop-Process -Force` + 3 sn + `cmd /c "rmdir /s /q app\build"` (seçici silme YASAK). Fail genelde VSCode redhat.java LS. Defender exclusion'ları 2026-07-14'te doğru yollara eklendi.
- **Pre-commit hook:** `check_duplicates.py` cp1254 konsolda emoji print'te çöküyor → commit öncesi `$env:PYTHONIOENCODING='utf-8'`. (Script fix backlog'da.)
- **local.properties worktree'lerde yok** → ana repodakini AYNEN kopyala (elle yazma — escape hatası).
- **Türkçe MD dosyalarına PowerShell Add-Content yazma** — Python `write_text(encoding='utf-8')` kullan.
- **KeywordDatabase substring eşleşmesi agresif** (P0.7 bulgusu) — classifier işlerinde kısa keyword false-positive'lerine dikkat.

## Doğrulama Borçları (Codex veya sonraki oturum)
- Sprint 1-2 değişikliklerinin emülatör smoke'u yapılmadı (unit test + compile + APK kapısı geçti; cihaz doğrulaması bekliyor).
- P1.3 kişi önerileri UI'sız — P1.2 ile görünür olacak.
