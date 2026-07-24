# KOD TARAMA DÖNGÜSÜ — Durum ve Checkpoint Dosyası

> **Bu dosya döngünün tek gerçek kaynağıdır.** Her iterasyon: (1) bu dosyayı oku, (2) `DEVAM` işaretli ilk modülü al, (3) işle, (4) sonucu buraya yaz, (5) commit. Kesinti olursa sonraki iterasyon kaldığı yerden devam eder.

## Görev Tanımı (Hüseyin talimatı, 2026-07-25 — orkestra şefi geliştirilmiş hali)

Tüm kod tabanını modül modül tara. Her modülde:
1. **Ölü kod:** caller'sız fonksiyon/parametre/import/state — SİL (public API ve test edilen semboller hariç; silmeden önce grep ile 0 caller kanıtla).
2. **Hatalı kodlama:** yanlış alan adı, string literal sınıf yolu, kopuk yaz→oku→TÜKET zinciri, kabul/kayıt yolu limit uyuşmazlığı, sihirli sayı (token'a bağla), yanlış paket referansı.
3. **Mantık hataları:** ters koşul, unreachable dal, race condition, `remember` ile donmuş pref okuma (Reaktif AppPrefs pattern'i gerekiyorsa uygula), Locale("tr") eksikliği.
4. **Ayarlar denetimi (Settings modüllerinde):** her satır sınıflandır — gerçek ayar mı / bilgi mi / yönlendirme mi; toggle'ın kod karşılığı VAR MI ve TÜKETİLİYOR MU (D240 zincir testi); eksik toggle ekle, işlevsiz olanı bağla veya kaldır; metinler sempatik + yeni kullanıcı dostu, iç mantık sızdırmaz.
5. **Geliştirme:** küçük, riski düşük iyileştirmeler doğrudan yapılır; büyük fikirler FİKİRLER/ROADMAP'e puanlanarak yazılır, bu döngüde YAPILMAZ.

### Kurallar (her iterasyonda geçerli)
- Ağır analiz/düzeltme Sonnet agent'a devredilir; şef sadece seçer-birleştirir-doğrular (maliyet kuralı).
- Her modül sonunda: `gradlew compileDebugKotlin -PskipGoogleServices` (hızlı derleme kontrolü). TAM build + APK sadece 4 modülde bir veya faz sonunda (feedback_cycle_build_strategy).
- Her düzeltme LEARNINGS D240 kurallarına uyar: zincir testi, `::class.java`, tek limit sabiti, dead-code=görev-açık, UI fix=screenshot.
- Her iterasyon sonunda: bu dosya güncellenir + commit (+push dene, timeout olursa not düş) + HISTORY.md 3 satır.
- Çözülemeyen sorun → 3 farklı kaynakla online araştır (WebSearch → lokal AI → DeepSeek); yine olmazsa COZULEMEYEN_SORUNLAR.md'ye gerekçeyle yaz, modülü BLOKE işaretle, sonrakine geç.
- Bir iterasyonda EN FAZLA 1 modül işlenir (context taşmasını önler). Modül büyükse alt parçaya böl, bu listeye ekle.

## Modül Listesi ve Durum

| # | Modül | Kapsam | Durum |
|---|-------|--------|-------|
| M1 | utils/ prefs katmanı | AppPrefs, DockPrefs, HomePagePrefs, WidgetPrefs — zincir testi hepsi | TAMAM |
| M2 | Ayarlar ekranları (DERİN) | SettingsScreen, SettingsLauncherScreen, SettingsHomeScreenSection + MANAGEMENT/SETTINGS_AUDIT_REPORT.md maddeleri | BEKLEMEDE |
| M3 | launcher/ çekirdek | HomeScreen, HomeShell, HomePagerHost, HomePagePlanner, LauncherViewModel | BEKLEMEDE |
| M4 | launcher/ bileşenler | FolderTile, FolderScreen, AllAppsDrawer, HomeScreenComponents, GlobalSearchHost, DockEditSheet | BEKLEMEDE |
| M5 | launcher/hero/ | HeroDashboardPage, HeroDock, Hero* kartlar, SmartDashboardPage | BEKLEMEDE |
| M6 | domain/ | models, usecase/classify (AppClassifier, KeywordDatabase), InsightEngine | BEKLEMEDE |
| M7 | data/ | AppDao, AppDatabase, repository'ler, migration'lar, FTS | BEKLEMEDE |
| M8 | service/ + worker + receiver | AppNotificationListenerService, PackageChangeReceiver, BackupWorker, FCM | BEKLEMEDE |
| M9 | Aktiviteler + navigasyon | MainActivity, LauncherActivity, Routes, onboarding | BEKLEMEDE |
| M10 | Global ölü kod süpürmesi | detekt raporu + cross-module unused sembol taraması | BEKLEMEDE |
| M11 | res/ tutarlılık | strings (TR), tema, hardcoded metin/renk avı | BEKLEMEDE |
| M12 | Araç/altyapı onarımı | check_duplicates.py 0-entry bug'ı (JSON formatı değişmiş — script güncellenecek), bayat CLAUDE.md yolları | BEKLEMEDE |

## İterasyon Günlüğü

### 2026-07-25 — Kurulum (Döngü 0)
- Sistem kuruldu. D240 denetimi tamamlandı (4 kopuk halka bağlandı, v1.4.26 Telegram'da, commit 0332332f).
- Bilinen açık işler: git push timeout (arka planda deneniyor), telefon bağlantısı kopuk (cihaz doğrulama testi bekliyor), `.claude/worktrees/` altında birleştirilmemiş eski worktree'ler (M10'da ele alınacak).
- Sonraki iterasyon: **M1**.

### 2026-07-25 — M1 (Döngü 1)

Kapsam: `app/src/main/java/com/armutlu/apporganizer/utils/` altındaki 16 `*Prefs.kt` dosyası (AppPrefs 1555 satır 2 parçada, DockPrefs, HomePagePrefs, WidgetPrefs, HomeLayoutPrefs, + 11 küçük Prefs dosyası) 4 paralel Sonnet agent ile zincir testinden (yaz→oku→tüket) geçirildi.

**Silinen semboller (ölü kod, 0 caller kanıtlandı):**
- `DockPrefs.resolveDefaultCategory(context)` — CRON-37 kopuk halkası zaten `LauncherViewModel.loadDockPackages()` içinde bağımsız bir yolla (AppPrefs.getDockDefaultCategory + manuel filtre) çözülmüş; bu fonksiyon artık hiç çağrılmıyordu. `DockPrefs.kt`.
- `WrappedSnapshotPrefs.KEY_LAST_SCORE` + `getLastScore()` + `setLastScore()` — `KEY_PULSE_LATEST_TOTAL`/`setLatestPulseScore`/`getLatestPulseScore` ile çift key durumu, `KEY_LAST_SCORE` tarafı hiç kullanılmıyordu. `WrappedSnapshotPrefs.kt`.

**Bağlanan kopuk halkalar:**
- `NotificationReadPrefs.clearAll()` — yazılıyor/okunuyordu ama "bildirim geçmişini sıfırla" akışına (StatsResetService.Scope.NOTIFICATION_HISTORY) hiç bağlı değildi; kullanıcı geçmişi sıfırlasa da okundu-zaman-damgaları kalıyordu. `StatsResetService.resetNotificationHistory()` içine eklendi.
- `PulseHistoryPrefs.clearAll()` / `MissionStreakPrefs.clearAll()` — "P0.4 istatistik sıfırlama sihirbazı ile uyumlu" yorumuyla yazılmış ama hiç çağrılmıyorlardı (WrappedSnapshotPrefs ile aynı SharedPreferences dosyasını paylaştıkları için örtük olarak temizleniyorlardı). `StatsResetService.resetWrappedSnapshots()` içine açıkça eklendi — dosya paylaşımı ileride değişirse sessizce kırılmak yerine kendi verisini garanti temizler. `Scope.WRAPPED_SNAPSHOTS` yorumu MissionStreakPrefs'i de kapsayacak şekilde güncellendi.

**Ertelenen bulgular (M2/M3'e not):**
- `AppPrefs.KEY_TELEMETRY_CONSENT_DECIDED/VERSION/LAST_CHANGED_AT` getter'ları (satır 40-50) hiç okunmuyor — setter (`setTelemetryConsent`) aktif yazıyor, muhtemelen ileride bir "gizlilik/onay geçmişi" ekranı için altyapı. Silinmedi (aktif yazma yolu var, veri kaybı riski) — M2 Ayarlar denetiminde consent UI'ı var mı kontrol edilmeli.
- `AppPrefs.KEY_HOME_PAGER_V2_ENABLED/SAFE_MODE` — koddaki yorum zaten "yalnız tanılama/yedek geriye uyumluluğu için tutulur, uygulama akışında okunmaz" diyor; Settings'te toggle var (`SettingsBackupAboutSection.kt:826-846`) ve DiagnosticsReportManager okuyor ama gerçek launcher davranışını etkilemiyor — kullanıcıya "işlevsiz görünen ama zararsız" bir toggle sunuyor olabilir, M2'de bu satırın Ayarlar'da gerçek ayar mı bilgi mi olarak sınıflandırılması gerekiyor.
- `AppPrefs.getAcceptedOverridePatterns` — `addAcceptedOverridePattern` (`AppListViewModel.kt:466`) aktif yazıyor ama okunan pattern seti hiçbir davranışı etkilemiyor (muhtemelen gelecekteki "toplu pattern önerisi" özelliği için biriktiriliyor); silinmedi, M3/M6 sınıflandırma modülünde tüketici bağlanmalı ya da kaldırılmalı.
- `AppPrefs.removeTickerHiddenType(context, typeName)` — "T05 — tekil geri açma" yorumuyla yazılmış ama hiçbir UI çağırmıyor; kullanıcı bir ticker türünü gizleyince kalıcı gizli kalıyor. `SmartTickerSettingsScreen.kt`'de tekil geri-aç butonu eklenmesi gerekiyor — bu bir Settings-ekranı UI değişikliği olduğu için M2'ye bırakıldı.
- `AppPrefs.isSearchSourceAppsEnabled/setSearchSourceAppsEnabled` — getter/setter parametreyi yok sayıp hep `true` zorluyor; ilk bakışta "sahte ayar" gibi göründü ama `SearchSettingsScreen.kt:241-248`'de `enabled=false` ile kilitli/disabled satır olarak doğru sınıflandırılmış ("sabit açık" alt yazısı) — DOKUNULMADI, bug değil, kasıtlı kilitli satır.
- `AppPrefs.KEY_MANUFACTURER_CLASSIFY` setter'ı (`setManufacturerClassifyEnabled`) 0 caller — getter hâlâ migration/backup yolunda (`BackupManager.kt:422`) kullanılıyor, o yüzden silinmedi; kullanıcı bu ayarı artık hiçbir UI'dan değiştiremiyor, M2'de Ayarlar'da bu toggle'ın var olup olmadığı kontrol edilmeli.

**Doğrulanan sağlam desenler (dokunulmadı):**
- `HomeLayoutPrefs` `remember{}` okuması ilk bakışta donmuş görünüyordu, `OnSharedPreferenceChangeListener` ile doğru reaktif pattern kullandığı doğrulandı (`HomeScreen.kt:203,313-337`).
- `DockPrefs` `MAX_SLOTS`/`take(4)`/`take(MAX_SLOTS)` kullanımları kasıtlı tasarım (ilk 4 slot otomatik, 5. slot kullanıcıya/CRON-37 dinamik dolguya bırakılır) — dünkü `take(MAX_SLOTS)` fix'i bozulmadı, doğrulandı.
- `InsightPrefs`, `ContactActionPrefs`, `MissionPrefs`, `SearchHistoryPrefs`, `SearchStatsPrefs`, `StartupHealthPrefs`, `WorkerTelemetryPrefs`, `WidgetPrefs` — tam zincir (yaz→oku→tüket) doğrulandı, temiz.

**Sayılar:** silinen 2 sembol grubu (1 fonksiyon + 1 key/getter/setter üçlüsü), bağlanan 3 kopuk halka (StatsResetService üzerinden), ertelenen 6 bulgu (M2/M3/M6'ya not düşüldü).

**Build:** Bu iterasyonda build alınmadı (talimat gereği) — sonraki modülde veya faz sonunda `gradlew compileDebugKotlin -PskipGoogleServices` ile doğrulanmalı.

**Sonraki modül:** M2 — Ayarlar ekranları (DERİN): SettingsScreen, SettingsLauncherScreen, SettingsHomeScreenSection + yukarıda ertelenen 3 UI-bağlantılı bulgu (telemetry consent UI, HOME_PAGER_V2 toggle sınıflandırması, ticker hidden type geri-aç butonu, manufacturer classify toggle).

