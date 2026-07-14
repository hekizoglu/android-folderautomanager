# FİKİRLER.md — AppOrganizer Fikir & Görev Havuzu

> Yeni özellik fikirleri, döngüden gelen görevler, backlog adayları buraya eklenir.
> 15+ puan alanlar ROADMAP.md'ye de taşınır. Tamamlananlar → HISTORY.md arşiv.
> Telegram onayı alındıktan sonra fikir hayata geçirilir.

---

## Nasıl Kullanılır

- Claude her döngü sonunda yeni fikri/görevi buraya ekler
- Her madde: tarih + kaynak + öncelik + kısa açıklama
- Onay gelince: `[ONAYLANDI 2026-xx-xx]` etiketi + uygulama başlar
- Tamamlanınca: `[TAMAMLANDI]` etiketi → HISTORY.md'ye taşınır

**Öncelik:** 🔴 Kritik · ⭐ Yüksek Puanlı (15+) · 🟡 Orta (12-14) · 🟢 Düşük · ⚪ Fikir

---

## 🔴 Kritik

| Tarih | Puan | Kaynak | Madde | Durum |
|-------|------|--------|-------|-------|
| 2026-06-16 | 19p | ROADMAP | **QUERY_ALL_PACKAGES Play Store beyan formu** — göndermeden önce zorunlu, aksi halde APK reddedilir. Play Console'da temel işlev gerekçesi, kullanıcı faydası ve daha dar görünürlük alternatiflerinin neden yetersiz olduğu kanıtlanmalı. (KV:5 · U:5 · BR:5 · EA:4 = **19p**) | Bekliyor ⚠️ — dış aksiyon (Play Console) gerekli |

---

## ⭐ Yüksek Puanlı (≥15p)

| Tarih | Puan | Madde | Durum |
|-------|------|-------|-------|
| 2026-07-07 | 18p | **Play Store Privacy/Data Safety uyum paketi** — `play-store-hazirlik-risk-raporu.md` + `izin-veri-haritasi.md`: privacy policy, Data Safety, Firebase/Crashlytics/Analytics/FCM, DeepSeek, Drive/SAF ve package inventory beyanlari ayni hikayeye cekilmeli. (KV:5 · U:5 · BR:5 · EA:3 = **18p**) | Kısmen tamamlandı (Döngü 214+215) — privacy_policy.html Firebase/Contacts/Bildirim/Accessibility Service celiskileri duzeltildi, Firebase Analytics'ten package_name kaldirildi; Play Console Data Safety formu doldurma dış aksiyon olarak kalıyor |
| 2026-07-07 | 16p | **Play Store release imza ve submission kapisi** — release keystore, content rating, QUERY_ALL_PACKAGES declaration ve final AAB temiz committen build edilmeden yayin tamamlanamaz. (KV:5 · U:4 · BR:4 · EA:3 = **16p**) | Bekliyor — dış aksiyon (keystore/Play Console) gerekli |
| 2026-07-07 | 15p | **Gercek cihaz Play-oncesi QA paketi** — Android 14 NotificationListener, Play screenshot smoke, backup/restore, worker schedule ve blur/API26 testleri tek kanitli test paketinde kosulmali. (KV:4 · U:4 · BR:4 · EA:3 = **15p**) | Bekliyor |
| 2026-07-14 | 17p | **Arama çubuğu konumu: alta taşıma + Ayarlar toggle** — Tek elle kullanım için ana ekran arama çubuğu en alta (dock bölgesine) alınabilmeli; Ayarlar > Ana Ekran'da "Üstte/Altta" tercihi (yeni vizyon: kendi kimliğimiz, karar ölçütü kullanıcı ergonomisi). Dikkat: dock ile yerleşim, AllAppsDrawer yukarı-swipe jest çakışması, sistem gesture bar inset'i, klavye açılınca sonuç listesi konumu. (KV:5 · U:4 · BR:4 · EA:4 = **17p**) [ONAYLANDI 2026-07-14] [TAMAMLANDI D257] | ✅ v1.3.15 — varsayılan Altta, Ayarlar > Ana Ekran'dan değiştirilebilir |
| 2026-07-14 | 16p | **🐛 İzin ver butonu izin sonrası takılı kalıyor** — callback state güncellenmiyor. Detay → ROADMAP.md "Hüseyin Geri Bildirim Listesi" [1]. (KV:5·U:4·BR:4·EA:3) | ✅ Tamamlandı (Döngü 268) — `ContextualPermissionDialog.kt` checkSelfPermission çapraz doğrulama + ON_RESUME re-check |
| 2026-07-14 | 15p | **🐛 Silip tekrar kurunca onboarding başlamıyor** — Auto Backup şüphesi (`AppPrefs` restore). Detay → ROADMAP.md [2]. (KV:5·U:3·BR:4·EA:3) | ✅ Tamamlandı (Döngü 268) — cihaza-özel `install_marker` dosyası backup/device-transfer dışına alındı |
| 2026-07-14 | 15p | **🐛 Ticker açık item'a tekrar tıklayınca donuyor** — 700ms debounce ile düzeltildi. Detay → HISTORY.md Döngü 265. | ✅ Tamamlandı (D265) |
| 2026-07-14 | 15p | **🐛 "Sınıflandırılmamış: N" satırı tıklanınca açılmıyor** — navigation eksik. Detay → ROADMAP.md [11]. (KV:4·U:5·BR:4·EA:2) | Bekliyor 🐛 |
| 2026-07-14 | 15p | **🐛 "Tüm istatistikleri sıfırla" 2 kez onay istemeli** — İstatistik sıfırlama sihirbazı fikriyle (13p, aşağıda) birleştirilmeli. Detay → ROADMAP.md [12]. (KV:4·U:4·BR:4·EA:3) | Bekliyor 🐛 |
| 2026-07-08 | 15p | **Akıllı Bildirim Analiz Sistemi** — `AppNotificationListenerService` + `notification_events` + `NotificationAnalyzer` + `SmartInsightWorker` hattı mevcut ama privacy-first rapor/öneri/günlük akıllı bildirim sistemine tamamlanmamış (worker duplicate-schedule riski, saat değişince yeniden planlama, POST_NOTIFICATIONS yoksa sessiz devam, Privacy Policy/Data Safety metniyle birebir uyum). Tam kabul kriterleri ve modül listesi → ROADMAP.md "🧠 Akıllı Bildirim Analiz Sistemi — Detay". (KV:4 · U:3 · BR:4 · EA:4 = **15p**) | Bekliyor — ROADMAP R7 ile eşleşiyor |
---

## 🟡 Orta Öncelik (12-14p)

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-07-09 | ThemePreferences.kt + HomeScreen.kt + OnboardingScreen.kt | **HomeScreen genelinde hardcoded Türkçe metinler EN cihazda kırık UI'a yol açıyor (Döngü 224'te emülatör smoke testinde doğrulandı)** — Emülatör sistem dili `en-US` iken klasör adları ("Sosyal Medya", "Üretkenlik" vb.), arama kutusu, filtre chip'leri gibi HomeScreen metinlerinin büyük çoğunluğu Kotlin literal Türkçe kalıyor (stringResource kullanmıyor); buna karşın gerçekten `stringResource()` kullanan birkaç yer (long-press context menü "Open Folder/Move Position/Go to All Apps", ticker "Midday Picks", `HomeScreen.kt` isLoading fallback ekranı "Launcher Settings/App Settings") doğru şekilde İngilizce render ediyor — sonuç karma dilli, tutarsız bir UI. `ThemePreferences.kt`'deki tema/font enum `label`'ları ve Onboarding Quick Settings toggle metinleri de aynı kökten (Döngü 218'de kısmen belgelendi). Kapsam muhtemelen Settings ekranlarına da yayılıyor, tam envanter çıkarılmadı. (KV:4 · U:3 · BR:3 · EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | Rekabet (Smart Launcher/Niagara) | **Home ekranı/dock'ta kullanım sıklığına göre otomatik sıralama** — Niagara pattern. `lastUsedTimestamp` alanı zaten Room'da mevcut (MIGRATION_3_4), sadece dock/klasör içi sıralama mantığı eksik. (KV:4 · U:3 · BR:3 · EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-etki-matrisi.md` çıkar** — tüm `AppPrefs` key'leri + ayar ViewModel state'leri için UI konumu / yazıldığı yer / okunduğu yer / gerçek davranış / sorun / aksiyon tablosu. "Ayar UI'da var" ile "ayar gerçekte çalışıyor" ayrımını netleştirir, diğer 5 inceleme bunun üzerine kurulu. (KV:4·U:4·BR:2·EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | play-store-hazirlik-risk-raporu.md | **Play Store screenshot paketi** — Pixel 6 light/dark seti, privacy hassas ekranlarda kisisel veri olmadan alinmali; store listing ve QA pack ayni gorsel hikayeyi anlatmali. (KV:4 · U:4 · BR:2 · EA:4 = **14p**) | Bekliyor — dış aksiyon (emülatör screenshot çekimi) |
| 2026-07-07 | yedekleme-geri-yukleme-guvenilirlik-raporu.md | **Backup kapsam matrisi ve eksik ayarlar** — yedek "tum ayarlar" iddiasi tasiyor ama Smart/Search/Widget/Theme gibi ayarlar eksik olabilir; her AppPrefs key'i backup'a giriyor mu cikarilmali. (KV:4 · U:4 · BR:3 · EA:3 = **14p**) | Bekliyor |
| 2026-07-07 | yedekleme-geri-yukleme-guvenilirlik-raporu.md | **Restore sonrasi global UI refresh stratejisi** — import AppPrefs/DockPrefs/Room guncelliyor; listener olmayan ekranlar eski kalabilir, restart veya global invalidation karari gerekli. (KV:3 · U:4 · BR:3 · EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | performans-bellek-build-raporu.md | **Performans benchmark sonuclari dosyasi** — build suresi, cold start, Home idle bellek, AllApps search jank ve APK boyutu olculup `performans-benchmark-sonuclari.md` icine islenmeli. (KV:4 · U:4 · BR:2 · EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | performans-bellek-build-raporu.md | **App startup init listesini sadelestir** — `AppOrganizerApp.onCreate` Firebase, worker schedule, FCM token, channel ve analytics baslatiyor; zorunlu/ertelenebilir isler ayrilmali. (KV:4 · U:3 · BR:3 · EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | ana-ekran-rekabet-revizyon-raporu.md | **Minimal Search Mode / Focus Mode revizyonu** — klasor istemeyen kullanici icin search bar + favorites + recent odakli mod, mevcut Focus Mode uzerinden yeniden tasarlanmali. (KV:3 · U:4 · BR:2 · EA:3 = **12p**) | Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 3) | **Güven skoruna göre otomatik kategorize** — düşük confidence'lı sınıflandırmalarda kullanıcıya sor. Detay → ROADMAP.md "Hüseyin Geri Bildirim Listesi" [3]. (KV:4·U:4·BR:3·EA:3) | 14p — Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 4) | **Arama barı klavye ile hafif çakışıyor** — `WindowInsets.ime` offset artırılmalı. Detay → ROADMAP.md [4]. (KV:3·U:4·BR:4·EA:2) | 13p — Bekliyor 🐛 |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 6) | **Ticker swipe çalışmıyor** — tap+swipe tek gesture döngüsünde birleştirilip nested-scroll çakışması giderildi. Detay → HISTORY.md Döngü 265. | ✅ Tamamlandı (D265) |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 7) | **Pulse Clock altı insight metni kaldır, saat küçült** — Detay → ROADMAP.md [7]. (KV:3·U:4·BR:2·EA:3) | 12p — Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 8) | **Onboarding'de güçlü özellikler (3702 paket, gizlilik) öne çıkarılsın** — Detay → ROADMAP.md [8]. (KV:3·U:4·BR:2·EA:2) | 11p — Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 9) | **"En Çok Kullandıklarım" küçültülüp yanına teknik bilgi eklensin** — Detay → ROADMAP.md [9]. (KV:3·U:3·BR:2·EA:2) | 10p — Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 10) | **"Dijital Yaşam Skoru" renk kodlu rozet** — ticker'a skor rozeti. Detay → ROADMAP.md [10]. (KV:4·U:3·BR:3·EA:3) | 13p — Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 13) | **Ana ekranda "Görevler" giriş noktası** — gamification sistemi. Detay → ROADMAP.md [13]. (KV:4·U:2·BR:2·EA:3) | 11p — Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 14) | **"Direkt Onayla" butonuna açıklama eklensin** — Detay → ROADMAP.md [14]. (KV:3·U:5·BR:1·EA:2) | 11p — Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 15) | **Görev puanlama motoru (gamification puan sistemi)** — Detay → ROADMAP.md [15]. (KV:3·U:2·BR:2·EA:3) | 10p — Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 16) | **🐛 "Sınıflandırılmamışları sınıflandır" Türkçe karakter hatası (encoding)** — Detay → ROADMAP.md [16]. (KV:3·U:5·BR:4·EA:2) | 14p — Bekliyor 🐛 |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 17) | **🐛 Birleşik arama kapsamı eksik (kategori/klasör/dosya)** — D265'te doğrulandı: klasör adı (özel ad dahil) zaten aranıyor, kategori adı = klasör adı. Detay → ROADMAP.md [17]. | ✅ Tamamlandı (D265) |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 18) | **AllAppsDrawer'da uygulama altına bildirim özeti** — Detay → ROADMAP.md [18]. (KV:3·U:3·BR:2·EA:3) | 11p — Bekliyor |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 19) | **Arama sonuçlarına tür etiketi (uygulama/kişi/dosya/klasör)** — D265'te doğrulandı: `HomeAppSearchBar` sonuçları ikonlu grup başlıklarıyla (Uygulamalar/Klasörler/Ayarlar/Kişiler/Dosyalar) zaten ayrıştırıyor. Detay → ROADMAP.md [19]. | ✅ Tamamlandı (D265) |
| 2026-07-14 | Hüseyin geri bildirim listesi (madde 20) | **Klasörler arası geçiş animasyonu iyileştirilsin (iPhone tarzı)** — Detay → ROADMAP.md [20]. (KV:3·U:3·BR:3·EA:3) | 12p — Bekliyor |
| 2026-07-13 | Hüseyin (kullanıcı) | **İstatistik sıfırlama sihirbazı** — Kullanım/bildirim/Wrapped istatistiklerini tek dokunuşla sıfırlamak yerine kısa bir soru akışı (örn. "Hangi verileri sıfırlamak istiyorsun: kullanım süresi / bildirim geçmişi / haftalık skor karşılaştırması / hepsi?") üzerinden sıfırlansın — yanlışlıkla tüm geçmişi silme riskini azaltır, hangi verinin ne işe yaradığını da öğretir. Muhtemel dosyalar: `UsageStatsHelper.kt`/Room `apps` tablosu (usageCount/lastUsedTimestamp), `notification_events` tablosu, `WrappedSnapshotPrefs.kt` (haftalık skor geçmişi), yeni bir onay diyalog ekranı (`SettingsAppsSection.kt` veya `SettingsBackupAboutSection.kt` altına). (KV:3 · U:4 · BR:3 · EA:3 = **13p**) | Bekliyor |

---

## ⏸ Beklet (≤11p)

| Tarih | Kaynak | Madde | Puan |
|-------|--------|-------|------|
| 2026-06-29 | Rekabet | **Online App Category DB** — Opsiyonel, gizlilik riski yüksek. Lokal → keyword → kullanıcı override → en son online. | 10p |
| 2026-06-29 | Hüseyin | **claude-code-templates mobile-design skill** — Tablet/foldable desteği planlandığında ekle. | 9p |
| 2026-07-07 | Rekabet (Smart Launcher/Niagara) | **Ayarlar'da grid yoğunluğu (satır/sütun) manuel slider'ı** — Smart Launcher'ın Fluid Grid pattern'i; mevcut grid sabit mi kontrol edilmeli, sabitse ayarlanabilir hale getirilsin. (KV:3 · U:3 · BR:2 · EA:3) | 11p |
| 2026-07-07 | ayarşar-raporlar.md | **Haftalık Uygulama Raporu ile Akıllı Bildirimler/Kullanılmayan Uygulamalar kavramsal çakışıyor** — ikisi de "uzun süredir açılmayan uygulama" bildirimi; Haftalık Uygulama Raporu Bildirimler ekranına taşınmalı. (KV:2·U:3·BR:3·EA:2) | 10p |
| 2026-07-07 | ayarşar-raporlar.md | **"Hakkında & Yedekleme" ekranı çok fazla iş yapıyor** — hakkında/gizlilik/yedek/Drive/restore/tanıtım/dashboard/debug aynı alt ekranda; ayrı alt sayfalara bölünmeli. (KV:3·U:3·BR:3·EA:2) | 11p |
| 2026-07-07 | ayarşar-raporlar.md | **DeepSeek API key düz metin SharedPreferences'ta saklanıyor** — `SettingsAppsSection.kt:164`; EncryptedSharedPreferences'a taşınmalı veya en azından risk uyarısı eklenmeli. Güvenlik ilgili, öncelik yükseltilebilir. (KV:4·U:2·BR:2·EA:3) | 11p |
| 2026-07-07 | ayarşar-raporlar.md | **Ayarlar hub'ına "ayar ara" ekle** — bu kadar çok ayar varken arama kolaylaştırır. (KV:3·U:3·BR:2·EA:3) | 11p |
| 2026-07-07 | ayarşar-raporlar.md | **Kart radius 16dp → 8-12dp'ye indirilebilir** — repo UI disiplinine göre ayarlar ekranı daha dense/operasyonel olmalı (`SettingsComponents.kt:35`). (KV:1·U:2·BR:1·EA:5) | 9p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-gizlilik-izin-uyum-raporu.md` çıkar** — NotificationListener/UsageStats/Contacts/File indexing/QUERY_ALL_PACKAGES/Crashlytics/FCM/DeepSeek/Drive verilerinin ne sakladığını Play Data Safety beyanıyla karşılaştır. Play Store gönderimi öncesi risk taşır. (KV:4·U:3·BR:2·EA:2) | 11p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-worker-zamanlama-raporu.md` çıkar** — BackupWorker/SmartInsightWorker/WeeklyDigestWorker'ın toggle ile schedule/cancel bağını, seçilen gün/saat/dakikanın hesaba katılıp katılmadığını ve duplicate notification riskini doğrula. (KV:3·U:3·BR:3·EA:2) | 11p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-state-reaktivite-raporu.md` çıkar** — hangi ayarların SharedPreferences listener ile reaktif, hangilerinin sadece ilk açılışta `remember` ile okunduğunu tara; restore/import/onboarding sonrası UI'ın eski kalıp kalmadığını doğrula. (KV:3·U:3·BR:2·EA:3) | 11p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-test-senaryolari.md` çıkar** — bildirim erişimi reaktifliği, çift tık arama/gesture çakışması, arama geçmişi limiti, dosya arama varsayılanı, yedekleme/SmartInsight worker schedule, ikon pack canlı güncelleme gibi 9 senaryoyu emülatörde uçtan uca doğrula. (KV:3·U:3·BR:2·EA:3) | 11p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-bilgi-mimarisi-raporu.md` çıkar** — hub rozetleri, Hakkında/Yedekleme bölünmesi, Launcher>Ana ekran alt gruplama, tekrar eden ayarların hangi ekranda kalacağı ve tehlikeli aksiyon onay standardı kullanıcı akışı olarak incelensin. (KV:2·U:3·BR:1·EA:3) | 9p |
| 2026-07-07 | yedekleme-geri-yukleme-guvenilirlik-raporu.md | **Backup JSON'a eski appName ekle** — restore eksik paket dialogunda sadece packageName yerine eski uygulama adi + packageName gosterilsin. (KV:2 · U:3 · BR:1 · EA:4) | 10p |
| 2026-07-07 | yedekleme-geri-yukleme-guvenilirlik-raporu.md | **Yedekte usage verisini dahil etme secenegi** — backup JSON usageCount/lastUsedTimestamp/hidden state tasiyor; kullaniciya hassas veri dahil etme secenegi verilebilir. (KV:3 · U:2 · BR:3 · EA:3) | 11p |
| 2026-07-07 | ana-ekran-rekabet-revizyon-raporu.md | **Permission Center kartini sade anlatimla yenile** — Contacts/files/bildirim/usage izinleri icin "kapaliysa ne kaybedersin, aciksa ne kazanirsin" dilini tek kartta toparla. (KV:3 · U:3 · BR:2 · EA:3) | 11p |
| 2026-07-07 | performans-bellek-build-raporu.md | **Worker schedule listesini app acilisinda logla** — Backup/SmartInsight/WeeklyDigest/FilesIndex worker durumlari debug logda gorulsun, duplicate schedule takibi kolaylassin. (KV:2 · U:3 · BR:2 · EA:4) | 11p |
| 2026-07-07 | ana-ekran-rekabet-revizyon-raporu.md | **Home ilk ekran deger onerisi testi** — ilk ekranda "otomatik klasorlendi + arama hazir + dock akilli" mesaji 3 saniyede anlasiliyor mu screenshot/QA ile kontrol edilsin. (KV:3 · U:3 · BR:1 · EA:3) | 10p |
| 2026-07-13 | Hüseyin (kullanıcı) + Fable fizibilite analizi | **Çoklu Cihaz Eşleştirme ve Senkronizasyon (AppOrganizer Sync Cloud)** — Firebase Auth + Firestore + Cloud Functions + E2EE + QR eşleştirmeli 9 fazlı yıldız topolojisi sync sistemi. ÖZEL NOT: Mimari tasarım sağlam ve varsayımların çoğu kod tabanında doğrulandı, ama tek geliştirici için 3-6 ay efor + Blaze (ücretli) plan zorunluluğu + minSdk 26'da Keystore ECDH imkânsızlığı (API 31+) + "veri cihazda kalır" Play konumlandırmasının kökten değişmesi nedeniyle **v1.0 Play Store yayını SONRASINA ertelendi**. Ara MVP: BackupManager v4 JSON'unu Drive/SAF klasörüne otomatik yükle + ikinci cihazda yedekten kur akışı (~1-2 döngü, sıfır sunucu). Faz-bazlı puan tablosu ve uyarlanmış dosya haritası → ROADMAP.md "Analiz — Çoklu Cihaz Senkronizasyonu (Dongu 247)". (KV:4 · U:2 · BR:1 · EA:4) | 11p |
| 2026-07-09 | Fable danışmanlık | **`AppClassifier.classifyByKeywords` Locale("tr") eksik** — `AppClassifier.kt:107-108`'de `lowercase()` locale'siz çağrılıyor; CLAUDE.md §5 Türkçe Locale kuralına aykırı (İ/ı riski). Keyword'ler ASCII olduğu için etki düşük ama bir sonraki AppClassifier dokunuşunda düzeltilmeli. (KV:2 · U:5 · BR:2 · EA:1) | 10p |

### Çıkarılan Fikirler — Puanlama Dışı Kayıt

- **Wrapped Phase 2:** Tasarruf hesabı (uydurma metrik), RAM/pil sağlık puanı (API kısıtlı), pil/veri/fiyat istatistikleri (erişim yok), gelecek tahmini (spekülatif) ve kohort karşılaştırması (sunucu verisi yok) çıkarıldı. Güven zedeleyen veya sahte veri gösterilmez.

---

## 📊 Rekabet Pozisyonlama Özeti

| Rakip | Bizim aldığımız | Kalan fark |
|-------|----------------|------------|
| KISS | UsageScore + Smart Search fikri | KISS'in arama hızı (native C) |
| Lawnchair | Pixel hissi korunuyor | Icon pack, global search |
| mLauncher | Gesture fikri | Biometric lock |
| Neo | Backup/kategori fikri | Icon shape, drawer özelleştirme |
| Fossify | Privacy Center fikri | Tam offline mimari |
| Kvaesitso | Search provider mimarisi | Plugin sistemi |
| Pie Launcher | Quick Wheel fikri | Tam radyal nav |
| Smart Launcher | Otomatik kategori atama vaadi (en yakın rakip) | Fluid Grid yoğunluk ayarı, gesture bar |
| Niagara Launcher | Arama-öncelikli minimal ana ekran fikri | Kullanım sıklığına göre otomatik dock sıralama |

**Hedef cümle:** "AppOrganizer, uygulamalarını otomatik klasörleyen; kullanım alışkanlığına göre dock, arama ve önerileri akıllandıran privacy-first Android launcher'dır."

---

## K2 — Override'lardan öğrenen öneri katmanı (Döngü 248 güncellemesi)

**KISMEN TAMAMLANDI (Döngü 248):** "Tek tek seçilebilir öneri" alt kısmı yapıldı — `AppClassifier.findSimilarUnclassifiedApps()` üretici prefix/keyword sinyaliyle adayları bulur, `SimilarAppsSuggestionDialog.kt` her satırı bağımsız checkbox ile gösterir (toplu kabul/red yok, sadece isteğe bağlı "Hepsini Seç/Hiçbirini Seçme" kısayolu var). `AppListViewModel.acceptSimilarCategorySuggestions(selectedPackageNames)` sadece seçilenleri taşır.
**Bekliyor:** Kabul edilen/reddedilen önerilerin paternlerinin yerel olarak "öğrenilmesi" (ağırlıklandırma, tekrar aynı öneri türü çıkarsa güven skoru artırma) — tam K2 speki bu değil, sonraki bir döngüye bırakıldı.

---

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda | Son güncelleme: 2026-07-13 (Döngü 248 — K2 kısmi: tek tek seçilebilir öneri)*
*Not: 2026-06-29 rekabet analizi [TAMAMLANDI] maddeleri HISTORY.md Tamamlananlar Arşivi'ne taşındı (v1.2.0 döngüsü). S1/S2/K1 (Döngü 207-208) tamamlandı, HISTORY.md'ye taşındı. Ayarlar audit'inden 10 madde Döngü 213'te, Play Store privacy uyumu (Accessibility Service beyanı dahil) Döngü 214-215'te tamamlandı, HISTORY.md'ye taşındı. R1-R7 (DOCS_SCORE_HIGH) artık `scripts/score_docs_backlog.ps1` içinde gerçek kaynaklarla (FİKİRLER.md/HISTORY.md/ROADMAP.md) tanımlı — önceki phantom rapor dosyası referansları (Döngü 215'te) düzeltildi.*
