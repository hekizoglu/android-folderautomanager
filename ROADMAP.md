# ROADMAP.md - AppOrganizer Aktif Yol Haritasi

> Son guncelleme: 2026-07-14.
> Bu dosya aktif yapilacaklar icin tek kaynak olarak kullanilir.
> Tamamlanan isler HISTORY.md'ye, yerelde cozulmeyen/dis aksiyon gerektirenler COZULEMEYEN_SORUNLAR.md'ye tasinir.
> **Vizyon (2026-07-14, Hüseyin):** Pixel Launcher klonu değiliz — kendi kimliği olan harika bir uygulama. Karar ölçütü: kullanıcı için en iyisi.

---

## ⭐ Yüksek Puanlı - Tek Elle Kullanım: Arama Çubuğu Alta (17p, 2026-07-14 Hüseyin onaylı)

| Puan | Görev | Durum |
|---|---|---|
| 17p | Ana ekran arama çubuğunu en alta (dock bölgesi) taşıma seçeneği + Ayarlar > Ana Ekran'da "Arama çubuğu konumu: Üstte/Altta" toggle'ı. Çözülmesi gerekenler: dock ile yerleşim kararı, AllAppsDrawer yukarı-swipe jest çakışması, sistem gesture bar `WindowInsets` payı, klavye açıkken sonuç listesinin klavye üstünde büyümesi. Varsayılan konum kararı uygulamada netleşecek (mevcut kullanıcıyı şaşırtmama vs. ergonomi). | Bekliyor — onaylı, sıradaki döngü adayı |

---

## ⭐ Yüksek Puanlı - Pulse Clock / Dijital Nabız (D244 devamı)

Yerel Pulse/Rapor detayları HISTORY.md'dedir. Aktif kalan tek kapı cihaz/emülatör doğrulamasıdır:

| Puan | Görev | Durum |
|---|---|---|
| — | Emülatörde/tablette manuel doğrulama: Pulse Clock 3 stil, skor/içgörü toggle'ları, uzun basma → yönetim ekranı, kompakt ekranda grid kaybolmuyor, klasör fihristi alt konumda grid üstüne binmiyor. 2026-07-13 kanıt: `Pixel6_API33` emülatörde `connectedDebugAndroidTest` 15 test / 0 failure geçti; AllApps/search/double-tap ve telefon `LauncherActivity` smoke crashsiz geçti. Simüle tablet AllApps/search crashsiz geçti; simüle tablet `LauncherActivity` screenshot denemesi ADB bağlantısını düşürdüğü için gerçek tablet/stabil tablet AVD görsel smoke gerekir. | Kısmen tamam - tablet Launcher görsel smoke bekliyor |

---

## Hedef

Play Store yayini icin Production AAB v1.0.0 hazir.

Kalan ana kapilar:
- Play Console formlari ve beyanlari
- Release imza ve final AAB
- Magaza gorselleri
- Gercek cihaz QA

## Guncel Kalan Is Listesi ve Uygulama Plani (2026-07-13)

| Sira | Is | Plan | Durum |
|---|---|---|---|
| 1 | Play Console beyanlari | QUERY_ALL_PACKAGES, Data Safety, Content Rating ve Privacy Policy URL alanlari kod gercegine gore doldurulacak. | Bekliyor - kullanici/Play Console |
| 2 | Release imza | `scripts/create_release_keystore.ps1` ile kalici key uretilecek, `keystore.properties` git disinda saklanacak, final AAB temiz committen alinacak. | Bekliyor - kullanici aksiyonu |
| 3 | Magaza screenshot seti | Home, All Apps search, Folder detail, Search settings, Privacy/permissions, Dashboard/report, Customization, Backup/restore ekranlari kisisel veri olmadan cekilecek. | Bekliyor - cihaz/emulator |
| 4 | Gercek cihaz QA paketi | NotificationListener, backup/restore, SmartInsightWorker, BackupWorker, blur/API26 ve OEM kategori smoke tek pakette kosulacak. AllApps double-tap Pixel6_API33 emülatörde 2026-07-13 crashsiz geçti; fiziksel cihazda genel QA parçası olarak tekrar edilebilir. | Bekliyor - gercek cihaz |
| 5 | `cycle.ps1` gercek tur | Temiz dal ve push hazirliginda bir kez uctan uca kosulup Telegram/commit/push kaniti alinacak. | Bekliyor - gercek tur |
| 6 | Wrapped Phase 2 dis dogrulama | UsageEvents oturum altyapisi API 28/29+, split-screen, kilit/ac, reboot ve grant/revoke olaylariyla cihazda kanitlanacak. | Bekliyor - dis dogrulama |
| 7 | Uzun vade backlog | Kendi sunucu API'si, Wear OS companion app ve widget ekran genisletme ayri sprintte ele alinacak. | Bekliyor |

---

## Kritik - Play Store ve Release Kapisi

| Gorev | Minimum cozum | Durum |
|---|---|---|
| QUERY_ALL_PACKAGES Play Store beyani | Launcher core functionality gerekcesiyle Play Console declaration doldurulacak. Metin ozeti: AppOrganizer tum yuklu uygulamalari organize etmek, app drawer/search gostermek ve uygulama baslatmak icin paket gorunurlugune ihtiyac duyar; paket/ad/kategori tercihleri cihazda kalir. | Bekliyor - dis aksiyon |
| Data Safety formu | Privacy policy, Firebase/Crashlytics/Analytics, optional contacts/files, NotificationListener, backup ve optional AI davranisi Play Console formuna kod gercegiyle uyumlu girilecek. Manifestte Accessibility servisi yok; servis geri eklenmedikce beyan edilmeyecek. | Bekliyor - dis aksiyon |
| Content rating | Play Console content rating anketi doldurulacak. | Bekliyor - dis aksiyon |
| Privacy Policy URL | GitHub Pages URL'i Play Console'a girilecek; policy dosyasi ve manifest URL'i ayni hikayeyi anlatmali. | Bekliyor - dis aksiyon |
| Release keystore | `scripts/create_release_keystore.ps1` hazir. Kullanici scripti calistirip kalici release key'i guvenli saklayacak; final AAB temiz committen imzalanacak. | Bekliyor - kullanici aksiyonu |
| Screenshot seti | Light/dark phone screenshot seti alinacak: Home, All Apps search, Folder detail, Search settings, Privacy/permissions, Dashboard/report, Customization, Backup/restore. Kisisel veri gorunmeyecek. | Bekliyor - cihaz/emulator |

---

## Kritik - Gercek Cihaz QA

| Gorev | Minimum cozum | Durum |
|---|---|---|
| Android 14 NotificationListener testi | Notification access ac/kapa, event yazma, rapor gorunumu ve reboot/permission lifecycle gercek cihazda kanitlanacak. | Bekliyor - gercek cihaz |
| Play oncesi gercek cihaz QA paketi | NotificationListener, backup/restore, SmartInsightWorker, BackupWorker, blur/API26, AllApps double-tap, OEM kategori ve screenshot smoke tek pakette kosulacak. Accessibility servisi manifestte olmadigi icin bu paketten cikarildi. | Bekliyor - gercek cihaz |
| BLUR-4/API26 testi | Blur/fallback performansi API 26+ temsilci cihazlarda kontrol edilecek. | Bekliyor - gercek cihaz |
| Backup/restore kaniti | SAF export/import, Drive klasor secimi, missingPackages dialogu ve restore sonrasi ayar devamligi kanitlanacak. | Bekliyor - cihaz/hesap |
| AllApps double-tap testi | Pixel6_API33 emülatörde arama alanı odak + `app` sorgusu + hızlı çift dokunma koşturuldu; app focus korundu, temiz logcat `FATAL EXCEPTION=0`. | Tamamlandi - emulator |
| Uretici kategori testi | Samsung/Xiaomi/Google tarzinda farkli OEM app setlerinde kategori eslesmeleri kontrol edilecek. | Bekliyor - cihaz |

## Orta Oncelik - Build, Surec ve Token Maliyeti

| Gorev | Minimum cozum | Durum |
|---|---|---|
| `cycle.ps1` uctan uca test | Kod incelemesiyle dogrulandi: encoding tarama -> duplicate kontrol -> ritimli build -> git add+commit+push -> Telegram bildirimi sirasiyla calisan orchestrator. Gercek uctan uca calistirilmadi. | Bekliyor - gercek tur denenecek |

---

## Yuksek Puanli - Wrapped Phase 2 (Fable analizi, Dongu 230-232)

| Puan | Gorev | Durum |
|---|---|---|
| 15p | UsageEvents oturum altyapisi gercek cihaz/OEM dogrulamasi - API 28/29+, split-screen, kilit/ac, reboot ve grant/revoke olaylari fiziksel cihazda kanitlanacak | Bekliyor - dis dogrulama gerekli |

Diger Phase 2 adaylari (gizlilik analizi 14p, AI kocu 13p, hedef sistemi 13p, kilit sayaci 12p) uzun vadeli backlog olarak izlenecek.

---

## Dusuk Oncelik ve Uzun Vade

| Gorev | Alan | Durum |
|---|---|---|
| Kendi sunucu API'si | `packageName -> category` endpoint; DeepSeek fallback alternatifi | Bekliyor |
| Wear OS companion app | Uzun vade companion deneyimi | Bekliyor |
| Widget ekran genisletme | Launcher disi hizli gorunum | Bekliyor |
| Coklu cihaz senkronizasyonu (Firebase Auth+Firestore+E2EE, 9 faz) | v1.0 Play Store yayini SONRASI ele alinacak — 11p fizibilite puani, 3-6 ay efor, Blaze plan + Play beyan hikayesi yeniden yazimi gerektiriyor. Detay: asagidaki "Analiz — Coklu Cihaz Senkronizasyonu" bolumu. Kullanici karariyla (2026-07-13) uzun vadeye tasindi; ara adim olarak "Yedekle ve Yedekten Don" (Drive/SAF) guclendirilmesi tercih edildi. | Bekliyor - uzun vade |

---

## Analiz — Çoklu Cihaz Senkronizasyonu (Fable fizibilite analizi, Dongu 247)

> Hüseyin'in 9 fazlı "AppOrganizer Sync Cloud" önerisi (Firebase Auth + Firestore + Cloud Functions + E2EE + QR eşleştirme) gerçek kod tabanına karşı doğrulandı. KOD YAZILMADI — sadece analiz.

### Kısa Özet

Öneri mimari olarak sağlam (yıldız topolojisi, outbox pattern, tombstone, HMAC belge ID, field-level merge — hepsi endüstri standardı doğru kararlar). Kod tabanı varsayımlarının çoğu DOĞRU çıktı. Ancak: (a) toplam efor tek geliştirici için 3-6 ay, (b) Cloud Functions için Firebase Blaze (ücretli) plana geçiş ZORUNLU, (c) minSdk 26 ile Keystore ECDH imkânsız (API 31+ gerekir — Android 8-11'de yazılımsal anahtar fallback'i şart, "private key Keystore'dan hiç çıkmaz" garantisi kısmen bozulur), (d) mevcut Play Store konumlandırması ("paket/ad/kategori tercihleri cihazda kalır" — bu metin QUERY_ALL_PACKAGES beyan taslağında bile var) kökten değişir: Data Safety formu, privacy policy ve beyan hikayesi yeniden yazılmalı. E2EE olsa bile Play politikasına göre cihazdan çıkan veri "collected" olarak beyan edilir.

### Doğrulanan / Yanlışlanan Varsayımlar

| Varsayım | Sonuç |
|---|---|
| `AppInfo.packageName` primary key | ✅ Doğru (`AppInfo.kt:21-22`) |
| Kategoriler sabit string ID (`CAT_SOCIAL="social"` vb.) | ✅ Doğru — 32 kategori + 9 üretici kategorisi, sync için stabil anahtar (`Category.kt`) |
| Backup kullanım/bildirim verisi de taşıyor | ✅ Doğru — BackupManager v4 `usageCount`(ms)/`launchCount`/`lastUsedTimestamp`/`notificationCount` export ediyor; önerinin DEVICE-kapsamı ihlali tespiti isabetli (`BackupManager.kt:41-44`) |
| Firebase zaten entegre | ✅ Kısmen — `google-services.json` mevcut, BOM 33.7.0, Analytics+Crashlytics+FCM aktif ve null-guard'lı. Ama Auth/Firestore/Functions bağımlılığı YOK; eklemek kolay, işletmek değil |
| Dock/klasör özelleştirme PROFILE kapsamıyla örtüşür | ✅ Örtüşür AMA kritik uyumsuzluk: bu veriler Room'da değil SharedPreferences'ta (`DockPrefs`, `AppPrefs` map'leri). "Her aksiyon Room transaction içinde outbox'a yazılır" varsayımı bu veriler için ÇALIŞMAZ — ya prefs→Room taşıma ya da atomiklikten feragat gerekir |
| Room migration kolay | ✅ Şu an v16 (CLAUDE.md'deki v12 bayat), `schemaLocation` + şablon mevcut; 7 yeni sync tablosu additive v16→v17 ile eklenebilir |
| Kullanım/bildirim verisi zaten bulutta değil | ✅ Doğru — `WrappedSnapshotPrefs` sadece cihaz-yerel agregat; hiçbir kullanım verisi cihaz dışına çıkmıyor (JSON backup hariç) |
| Keystore ECDH P-256 | ❌ EKSİK — `PURPOSE_AGREE_KEY` API 31+; minSdk 26'da Android 8-11 için yazılımsal ECDH + Keystore AES-wrap fallback tasarlanmalı |

### Faz Puanlama (KV: Kullanıcı Değeri · U: Uygulanabilirlik · BR: Bağımlılık Riski, 5=düşük risk · EA: Etki Alanı)

| Faz | İçerik | KV | U | BR | EA | Toplam | Durum |
|---|---|---|---|---|---|---|---|
| F1 | Veri sözleşmesi + sync entity modelleri (Room v17: SyncOutbox/Conflict/Device/Profile/Tombstone/State/EntityMeta) | 1 | 4 | 4 | 2 | **11** | Beklet |
| F2 | Firebase Auth + Credential Manager + cihaz UUID kaydı (Blaze plan kapısı burada açılır) | 2 | 3 | 2 | 2 | **9** | Beklet |
| F3 | E2EE (workspace key + key envelope) + QR eşleştirme (Keystore ECDH API 31+ sorunu burada) | 2 | 2 | 2 | 3 | **9** | Beklet |
| F4 | Tek yönlü ilk sync (upload + read-only preview) | 3 | 3 | 2 | 3 | **11** | Beklet |
| F5 | Çift yönlü sync (outbox + SyncWorker + mutation + listener) — asıl kullanıcı değeri burada | 4 | 2 | 2 | 4 | **12** | Beklet |
| F6 | Conflict engine (öncelik zinciri + fieldVersions merge + kullanıcıya sorma UI) | 3 | 2 | 2 | 3 | **10** | Beklet |
| F7 | Profil sistemi (Ortak/Telefon/Tablet) | 3 | 3 | 3 | 3 | **12** | Beklet |
| F8 | Güvenlik sertleştirme (App Check, Play Integrity, Security Rules, rate limit) | 2 | 2 | 1 | 2 | **7** | Beklet |
| F9 | Üretim QA (fiziksel çoklu cihaz + OEM matrisi) — tek geliştiricide cihaz parkı yok | 3 | 1 | 1 | 2 | **7** | Beklet |
| **Genel** | **Tüm proje** | **4** | **2** | **1** | **4** | **11** | **ERTELE — v1.0 sonrası** |

### Fizibilite Notları

- **Süre:** 9 faz, tek geliştirici + Claude döngüleriyle gerçekçi tahmin 3-6 takvim ayı (Cloud Functions TS toolchain'i, Rules testleri ve çoklu cihaz QA dahil). Play Store v1.0 kapısındaki işlerin (beyanlar, keystore, QA) tamamı bundan kısa.
- **Maliyet:** Cloud Functions deploy = Blaze plan ZORUNLU (Spark'ta Functions deploy edilemez). Firestore free kotası (50k okuma/20k yazma/gün) erken dönem için yeter; entity-başına-belge + realtime listener modeli kullanıcı başına ilk sync ~500-700 okuma üretir, 5-10k aktif kullanıcıya kadar maliyet önemsiz, sonrası lineer büyür. Asıl maliyet para değil bakım karmaşıklığı.
- **Kurtarma kodu UX riski:** XXXX-XXXX-XXXX-XXXX kodunu kullanıcıların büyük kısmı kaybeder; "tüm cihazlar kaybedilirse veri gider" destek yükü yaratır. Sync edilen verinin çoğu (kategori kararları, özelleştirme) yeniden üretilebilir olduğundan tam E2EE yerine "Google hesabına bağlı, sunucu tarafı şifreli, kurtarma kodu opsiyonel" modeli düşünülmeli — ya da E2EE kalacaksa veri kaybı kabul edilebilir çünkü felaket senaryosunda kullanıcı sadece özelleştirmelerini kaybeder.
- **Konumlandırma gerilimi:** YÜKSEK. Mevcut Play beyan taslağı ve privacy policy "cihazda kalır" hikayesi anlatıyor; sync bunu "opsiyonel bulut, E2EE" hikayesine çevirir. Bu değişiklik Play Store İLK yayından önce yapılırsa beyan süreci sıfırdan karmaşıklaşır — yayın SONRASI ayrı release olarak eklenmesi süreç açısından da doğru.

### Tavsiye

**ERTELE + KÜÇÜLT.** v1.0 Play Store yayını tamamlanana kadar başlanmasın. Ara adım olarak çok daha küçük MVP: mevcut BackupManager v4 JSON'unu kullanıcının seçtiği Drive/SAF klasörüne otomatik yükleyen + ikinci cihazda "yedekten kur" akışı sunan "fakir adamın sync'i" (~1-2 döngü, sıfır sunucu, sıfır plan değişikliği, değerin ~%60-70'i). Bu MVP yapılmadan önce backup'taki DEVICE-kapsam sızıntısı (usage/notification alanları) opsiyonel hale getirilmeli — FİKİRLER.md'de zaten 11p kayıtlı ("Yedekte usage verisini dahil etme secenegi"). Tam sync'e ilerideki bir sprintte dönülürse fazlar F1→F4→F5 çekirdeği önce, F3 E2EE en sona alınmalı (ilk sürüm sunucu tarafı şifreleme ile çıkabilir).

### Uyarlanmış Faz Planı (ileride başlanırsa — gerçek dosya haritası)

1. **F0 (ÖN KOŞUL — öneride yok):** SharedPreferences→Room taşıma kararı: `DockPrefs`, `AppPrefs` klasör özelleştirme/gesture/tema map'leri için ya `sync_prefs_mirror` tablosu ya da prefs-listener tabanlı outbox köprüsü. Bu çözülmeden F5 outbox atomikliği kurulamaz.
2. **F1:** `domain/models/sync/` altına SyncOutboxEntity, SyncTombstoneEntity, SyncEntityMeta, SyncStateEntity, SyncDeviceEntity, SyncProfileEntity, SyncConflictEntity; `AppDatabase` v16→v17 (şablon: MIGRATION_15_16); DAO'lar + `schemas/17.json`.
3. **F2:** `firebase-auth` + `firebase-firestore` bağımlılıkları (BOM zaten var), Credential Manager sign-in, `SyncIdentityManager` (cihaz UUID, `AppPrefs` değil ayrı `sync_identity_prefs`); SettingsScreen'e "Cihazlar Arası Eşitleme (Deneysel)" toggle (CLAUDE.md Yeni Özellik=Ayarlar kuralı).
4. **F3:** `SyncCryptoManager` — API 31+ Keystore ECDH, API 26-30 yazılımsal ECDH + Keystore AES-GCM wrap fallback; QR üretim/okuma (`zxing` veya ML Kit — yeni bağımlılık, WebSearch şart); pairing UI.
5. **F4:** `SyncUploader` — Room→Firestore ilk yükleme (HMAC belge ID), ikinci cihazda read-only önizleme ekranı.
6. **F5:** `SyncWorker` (WorkManager, mevcut `BackupWorker` pattern'i), outbox draining, Firestore listener→Room applier; Cloud Functions repo'su (`functions/` TS): applySyncMutations, finalizePairing vb.
7. **F6:** `ConflictResolver` (öncelik zinciri; `classificationSource`/`isCategoryLocked` alanları D246'da eklendi — manuel karar tespiti için hazır altyapı MEVCUT, önerinin lehine) + çakışma soru UI'ı.
8. **F7:** Profil sistemi — `SyncProfileEntity` + Settings profil seçici; PROFILE kapsam alanları BackupManager v4 settings bloğuyla birebir örtüşüyor (hazır envanter).
9. **F8-F9:** App Check + Play Integrity + Security Rules emulator testleri; fiziksel cihaz matrisi (dış aksiyon — COZULEMEYEN_SORUNLAR adayı).

---

## Dis Aksiyon Kayitlari

Detayli engel kaydi icin:
- COZULEMEYEN_SORUNLAR.md -> CS-3, CS-5, CS-6, CS-7

Tamamlanan raporlar ve kapanislar:
- HISTORY.md -> Dongu 220 ve onceki donguler.
