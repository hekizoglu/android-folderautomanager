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
| 2026-07-08 | 15p | **Akıllı Bildirim Analiz Sistemi** — `AppNotificationListenerService` + `notification_events` + `NotificationAnalyzer` + `SmartInsightWorker` hattı mevcut ama privacy-first rapor/öneri/günlük akıllı bildirim sistemine tamamlanmamış (worker duplicate-schedule riski, saat değişince yeniden planlama, POST_NOTIFICATIONS yoksa sessiz devam, Privacy Policy/Data Safety metniyle birebir uyum). Tam kabul kriterleri ve modül listesi → ROADMAP.md "🧠 Akıllı Bildirim Analiz Sistemi — Detay". (KV:4 · U:3 · BR:4 · EA:4 = **15p**) | Bekliyor — ROADMAP R7 ile eşleşiyor |
| 2026-07-09 | 16p | **Akıllı Kategorileme K1 — `ApplicationInfo.category` yerel sinyal katmanı + kalıcı LLM cache** (Fable danışmanlık) — İki tespit: (a) Android 8+'ın ücretsiz/offline Play Store kategorisi (`packageManager.getApplicationInfo(pkg).category`: GAME/AUDIO/VIDEO/IMAGE/SOCIAL/NEWS/MAPS/PRODUCTIVITY) sınıflandırma zincirinde HİÇ kullanılmıyor — exactMap sonrası, keyword öncesi katman olarak eklenmeli; (b) `CategoryLLMFallback` cache'i sadece in-memory `ConcurrentHashMap` — her uygulama açılışında aynı bilinmeyen paketler DeepSeek'e yeniden gidiyor (maliyet + gereksiz paket adı çıkışı + gecikme), sonuçlar AppPrefs/Room'a kalıcılaştırılmalı. İkisi birlikte DeepSeek bağımlılığını ciddi azaltır, tamamen cihaz içi. Dosyalar: `AppClassifier.kt`, `CategoryLLMFallback.kt`, `AppPrefs.kt` (veya Room `apps` tablosuna kolon). Zorluk: 3/10. (KV:4 · U:5 · BR:4 · EA:3 = **16p**) | [TAMAMLANDI] Döngü 228 |

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
| 2026-07-07 | ana-ekran-rekabet-revizyon-raporu.md | **Home UX karar listesi** — otomatik klasorleme, search bar, dock/favorites/recent/widget yuzeyleri icin kalacak-gidecek-yeniden gruplancak karar listesi cikarildi: `docs/internal/home_revizyon_karar_listesi.md`. (KV:3 · U:4 · BR:2 · EA:3 = **12p**) | Tamamlandi 2026-07-09 |
| 2026-07-07 | ana-ekran-rekabet-revizyon-raporu.md | **Minimal Search Mode / Focus Mode revizyonu** — klasor istemeyen kullanici icin search bar + favorites + recent odakli mod, mevcut Focus Mode uzerinden yeniden tasarlanmali. (KV:3 · U:4 · BR:2 · EA:3 = **12p**) | Bekliyor |
| 2026-07-09 | Fable danışmanlık | **Akıllı Kategorileme K2 — Override'lardan öğrenen öneri katmanı** — Manuel override'lar (`AppPrefs.getManualCategoryOverrides`) şu an tek paket bazlı, genelleme yok. Kullanıcı bir uygulamayı elle başka klasöre taşıyınca aynı üretici prefix'i/keyword kümesi/LLM kategorisindeki benzer uygulamalar için "Bunları da taşıyalım mı?" öneri chip'i göster; kabul edilen paternler yerel olarak öğrenilir (cihaz dışına veri çıkmaz). Statik taksonominin kullanıcı tercihine uyarlanmasının en ucuz yolu. Dosyalar: `AppClassifier.kt`, `LauncherViewModel.kt`, `FolderScreen.kt`, `AppPrefs.kt`. Zorluk: 6/10. (KV:4 · U:3 · BR:4 · EA:3 = **14p**) | Bekliyor |
| 2026-07-09 | Fable danışmanlık | **Akıllı Kategorileme K3 — Confidence tabanlı doğrulama akışı** — `AppClassifier.getConfidence()` mevcut ama UX'e bağlı değil. Düşük güvenli atamalar (CAT_OTHER=30, paket-keyword=70) Dashboard/Settings'te "Bu klasörleme doğru mu?" tek kartlık onay akışında gösterilsin; onay/düzeltme manuel override map'ine yazılır — hem doğruluk artar hem K2'ye eğitim verisi üretir. Çoklu-kategori sorunu (örn. YouTube hem Video hem Sosyal) burada "ikincil kategori önerisi" olarak ele alınabilir. Dosyalar: `AppOrganizerDashboardScreen.kt` veya `SettingsScreen.kt`, `AppPrefs.kt`. Zorluk: 4/10. (KV:3 · U:4 · BR:4 · EA:3 = **14p**) | [TAMAMLANDI] Döngü 228 — sadeleştirilmiş kapsamla: yeni Dashboard kartı yerine mevcut Home ticker'a "N uygulamanın kategorisi belirsiz" uyarısı + `Routes.APP_LIST`'e yönlendirme eklendi (manuel override akışı zaten oradan yapılabiliyordu). Çoklu-kategori/ikincil öneri kapsam dışı bırakıldı. |
| 2026-07-09 | Fable danışmanlık | **Akıllı Kategorileme K4 — Kullanım paternine göre bağlamsal akıllı klasör** — Room `usageCount`/`lastUsedTimestamp` + `UsageStatsHelper` saat-dilimi histogramıyla "Sabah Rutini / İş Saatleri / Akşam" tarzı sanal (dinamik) klasör veya klasör içi otomatik yeniden sıralama; tamamen cihaz içi, hiçbir veri dışarı çıkmaz. Mevcut 13p "kullanım sıklığına göre dock sıralama" fikriyle sinerjik — birlikte tek epik olarak ele alınabilir. Dosyalar: `LauncherViewModel.kt`, `HomeScreen.kt`, `UsageStatsHelper.kt`, `InsightEngine.kt`. Zorluk: 7/10 — CLAUDE.md kuralı gereği uygulamadan önce 2+ kaynak araştırma + Hüseyin onayı gerekir. (KV:4 · U:3 · BR:3 · EA:3 = **13p**) | Bekliyor |

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
| 2026-07-09 | Fable danışmanlık | **`AppClassifier.classifyByKeywords` Locale("tr") eksik** — `AppClassifier.kt:107-108`'de `lowercase()` locale'siz çağrılıyor; CLAUDE.md §5 Türkçe Locale kuralına aykırı (İ/ı riski). Keyword'ler ASCII olduğu için etki düşük ama bir sonraki AppClassifier dokunuşunda düzeltilmeli. (KV:2 · U:5 · BR:2 · EA:1) | 10p |
| 2026-07-10 | Universal Search analizi (Fable) | **Web fallback araması** — sıfır sonuçta "Google'da ara: {q}" satırı; ACTION_WEB_SEARCH intent, sunucu yok. (KV:4 · U:5 · BR:1 · EA:3) | 13p |
| 2026-07-10 | Universal Search analizi (Fable) | **Play Store fallback** — uygulama bulunamayınca "Play Store'da ara" satırı; market:// intent. (KV:3 · U:5 · BR:1 · EA:2) | 11p |
| 2026-07-10 | Universal Search analizi (Fable) | **Ayar araması (SETTING source)** — Wi-Fi/Bluetooth/Bildirim gibi ~20 statik sistem ayarı SearchDocument olarak indekslenir, Settings.ACTION_* intent ile açılır. (KV:4 · U:4 · BR:2 · EA:3) | 13p |
| 2026-07-10 | Universal Search analizi (Fable) | **Arama kalitesi öğrenmesi** — başarısız arama sonrası elle açılan uygulamayı tespit edip sıralamada boost et (query abandon sinyali). (KV:4 · U:2 · BR:3 · EA:3) | 12p |
| 2026-07-10 | Wrapped Phase 2 analizi (Fable) | **Gizlilik analizi ekranı** — mikrofon/konum/kamera izni kullanan uygulamaların listesi (PackageManager, tamamen lokal); Wrapped'a gizlilik kartı. (KV:5 · U:4 · BR:1 · EA:4) | 14p |
| 2026-07-10 | Wrapped Phase 2 analizi (Fable) | **UsageEvents oturum altyapısı** — günlük agregat oturum verisi (açılış sayısı, oturum süresi, saat dağılımı); ısı haritası, zaman tüneli, dikkat dağıtıcılar ve bağımlılık endeksinin ortak temeli. (KV:4 · U:3 · BR:3 · EA:5) | 15p |
| 2026-07-10 | Wrapped Phase 2 analizi (Fable) | **Kilit açma sayacı** — UsageEvents KEYGUARD_HIDDEN ile günlük telefon açma sayısı + haftalık trend; Wrapped kartı. (KV:4 · U:3 · BR:2 · EA:3) | 12p |
| 2026-07-10 | Wrapped Phase 2 analizi (Fable) | **AI koçu haftalık yorumu** — WrappedReport verisini lokal AI/DeepSeek'e özetletip 2 cümlelik kişisel yorum (opt-in, sadece agregat veri gider). (KV:4 · U:3 · BR:3 · EA:3) | 13p |
| 2026-07-10 | Wrapped Phase 2 analizi (Fable) | **Hedef sistemi** — haftalık kategori kullanım hedefi + rozet ödülü; oturum altyapısına bağımlı. (KV:4 · U:2 · BR:3 · EA:4) | 13p |
| 2026-07-10 | Wrapped Phase 2 analizi (Fable) | **ÇIKARILDI (kayıt):** tasarruf hesabı (uydurma metrik), RAM/pil sağlık puanı (API kısıtlı), pil/veri/fiyat istatistikleri (erişim yok), gelecek tahmini (spekülatif), kohort karşılaştırması (sunucu verisi yok) — güven zedeleyen/sahte veri gösterilmez. | - |

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

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda | Son güncelleme: 2026-07-09 (Fable akıllı kategorileme danışmanlığı K1-K4)*
*Not: 2026-06-29 rekabet analizi [TAMAMLANDI] maddeleri HISTORY.md Tamamlananlar Arşivi'ne taşındı (v1.2.0 döngüsü). S1/S2/K1 (Döngü 207-208) tamamlandı, HISTORY.md'ye taşındı. Ayarlar audit'inden 10 madde Döngü 213'te, Play Store privacy uyumu (Accessibility Service beyanı dahil) Döngü 214-215'te tamamlandı, HISTORY.md'ye taşındı. R1-R7 (DOCS_SCORE_HIGH) artık `scripts/score_docs_backlog.ps1` içinde gerçek kaynaklarla (FİKİRLER.md/HISTORY.md/ROADMAP.md) tanımlı — önceki phantom rapor dosyası referansları (Döngü 215'te) düzeltildi.*
