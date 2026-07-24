# AppOrganizer Statik Kod Denetim Bulguları

**Tarih:** 2026-07-24  
**Denetim Türü:** Bağımsız statik kod denetimi  
**Incelenen commit:** 1edc55a651f5663ae37fe72966e64ee2bb216b92  
**Toplam Bulgular:** 32 (1 kritik, 16 yüksek, 15 orta)

---

## Özet

| Önem | Sayı | Durum | Öncelik |
|------|------|-------|---------|
| **Kritik** | 1 | ⏳ Çözülmedi | P0 |
| **Yüksek** | 16 | ⏳ Çözülmedi | P0-P1 |
| **Orta** | 15 | ⏳ Çözülmedi | P1-P2 |

---

## P0 — Veri Kaybı & Temel İşlev (Kritik + 5 Yüksek)

### ✅ 01 — Uygulama güncellemesi gerçek kaldırma gibi işleniyor [KRITIK]

**Dosyalar:**
- `PackageChangeReceiver.kt:32-43,100-115`
- `LauncherActivity.kt:277-285`
- `AndroidManifest.xml:128-140`

**Sorun:**
- `EXTRA_REPLACING` yalnız `PACKAGE_ADDED` yolunda kontrol ediliyor
- `PACKAGE_REMOVED` sırasında güncellemeler gerçek kaldırma olarak işleniyor
- İkili receiver (Manifest + Activity) aynı olay için iki kez çalışıyor

**Etki:** Kategori, sayaçlar, notlar, bildirimler, favoriler kaybolabilir

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 02 — Paket metadata güncellemesi `IGNORE` nedeniyle uygulanmıyor [YÜKSEK]

**Dosyalar:**
- `PackageChangeReceiver.kt:118-138`
- `AppDao.kt:21-24`

**Sorun:**
- `repo.insertApps()` → DAO `OnConflictStrategy.IGNORE` → veren paket varsa güncelleme atlanıyor

**Etki:** Uygulama adı, sürüm, ikon, SDK güncellemeleri uygulanmıyor

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 03 — Paket güncelleme merge'i kullanıcı verilerini tamamlamıyor [YÜKSEK]

**Dosyalar:**
- `PackageChangeReceiver.kt:126-138`
- `AppInfo.kt:43-93`

**Sorun:**
- `customNotes`, `notificationImportance`, `notificationText`, `appSizeBytes`, kurulum zamanı, sınıflandırma metadata'sı kopyalanmıyor

**Etki:** Kullanıcı notları ve sınıflandırma bilgileri güncelleme sonrası kaybolabilir

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 04 — Repository istisnaları yuttuğu için ViewModel sahte başarı üretiyor [YÜKSEK]

**Dosyalar:**
- `AppRepository.kt:194-214`
- `AppListViewModel.kt:297-317`

**Sorun:**
- `updateAppCategory` hatayı yakalayıp rethrow etmiyor
- ViewModel Room başarısızlığını fark etmeden SharedPreferences + arama indeksi yazıyor
- Veri tutarsızlığı oluşabilir

**Etki:** Kategoriler arasında tutarsızlık, farklı ekranlarda farklı kategori gösterimi

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 05 — Kategori silme atomik değil, uygulamalar silinmiş kategoriye bağlı kalabilir [YÜKSEK]

**Dosyalar:**
- `AppListViewModel.kt:400-419`
- `AppRepository.kt:313-330`

**Sorun:**
- Taşıma başarısızsa (ve exception yutuluşsa) kategori yine silinir
- Uygulamalar silinmiş kategori ID'sine referans tutabilir

**Etki:** Veritabanı referans bütünlüğü ihlali

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 06 — İstatistik sıfırlama servisi başarısız işlemi başarılı bildiriyor [YÜKSEK]

**Dosyalar:**
- `StatsResetService.kt:45-69,80-105`
- `AppRepository.kt:546-569`

**Sorun:**
- Notification temizleme repository metotları exception'ları kendi içinde yutyor
- Servis `success=true` döndürüyor fakat veriler kalmış olabilir

**Etki:** Kullanıcı veri silindiğini sanıyor ama veritabanında kalıyor

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

## P1 — Ana Ekran Işlev (7 Yüksek)

### ✅ 07 — Klasör sürükleme ana ekranda hiçbir zaman etkinleşmiyor [YÜKSEK]

**Dosyalar:**
- `HomeScreenFolderPager.kt:196-209`
- `HomeScreen.kt:1240-1329`

**Sorun:**
- Drag handler yalnız `editMode=true` iken ekleniyor
- HomeScreen `editMode=false` gönderiliyor

**Etki:** "Klasör sırasını değiştir" özelliği tamamen kapalı

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 08 — Tablet sürükleme hesabı yanlış sütun sayısını kullanıyor [YÜKSEK]

**Dosyalar:**
- `HomeScreen.kt:165-170,1287-1309`

**Sorun:**
- Grid telefonda 4, tablette 5-6 sütun çiziliyor
- Drop hesabı sabit `colCount = 4` kullanıyor

**Etki:** Tablette yanlış klasör taşıma

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 09 — "İlk klasör sayfası" ayarı Dashboard'u açıyor [YÜKSEK]

**Dosyalar:**
- `HomeScreen.kt:492-500`
- `HomePagePlanner.kt:22-30`

**Sorun:**
- `FIRST_FOLDER_PAGE -> 0` tanımlı
- Index 0 koşulsuz Dashboard

**Etki:** Ayar işe yaramıyor

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 10 — Akıllı Dashboard aç/kapat ayarı etkisiz [YÜKSEK]

**Dosyalar:**
- `HomePagePrefs.kt:55-61`
- `HomePagePlanner.kt:22-30`
- `HomeScreen.kt:1028-1038`

**Sorun:**
- Toggle okunup yazılıyor fakat planning'de `dashboardEnabled=true` zorunlu gönderiliyor

**Etki:** Kullanıcı Dashboard'u kapatsa bile görünüyor

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 11 — Klasör yukarı kaydırma aynı uygulamayı birden fazla açabilir [YÜKSEK]

**Dosyalar:**
- `FolderTile.kt:129-144`

**Sorun:**
- Parmak hareketi devam ederse eşik yeniden aşılabilir
- `startActivity` birden fazla çalışabilir

**Etki:** Aynı uygulama birden fazla örneğe açılabilir

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 12 — `notificationImportance` hiçbir zaman yazılmıyor [YÜKSEK]

**Dosyalar:**
- `AppInfo.kt:57-63`
- `AppNotificationListenerService.kt:30-49`
- `AppDao.kt:334-358`

**Sorun:**
- Alan var, FolderTile kullanıyor, listener yazımı yok
- DAO'da update metodu yok

**Etki:** Bildirim önem sıralaması hep `0`

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 13 — "En güncel bildirim" bildirim zamanıyla seçilmiyor [YÜKSEK]

**Dosyalar:**
- `FolderTile.kt:315-326`

**Sorun:**
- Comparator `lastUsedTimestamp` (uygulamanın son kullanım zamanı) kullanıyor
- Bildirim zamanı değil

**Etki:** Eski bildirim gönderen fakat yakın zamanda açılmış uygulama ön planda görünebilir

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 14 — Bildirim observer'ları eski state'i yeni üzerine yazabilir [YÜKSEK]

**Dosyalar:**
- `LauncherViewModel.kt:341-380`

**Sorun:**
- Her emisyon bağımsız `launch(IO)` başlatıyor
- Eski coroutine yeni sonucun üzerine yazabilir

**Etki:** Bildirim sayısı ve metni tutarsız, "üst üste kalan" raporlar

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 15 — Ana ekran düzen tercihleri reaktif değil [YÜKSEK]

**Dosyalar:**
- `HomeScreen.kt:196-197`
- `HomeLayoutPrefs.kt:64-92`

**Sorun:**
- `HomeLayoutPrefs.read(context)` yalnız `remember` içinde bir kez çalışıyor
- Ayarlar ekranında değişiklik yapılsa bile launcher'a dönüşte uygulanmıyor

**Etki:** Düzen değişiklikleri görmek için Activity restart gerekli

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 16 — UsageStats senkronizasyonu açılışta iki kez başlayabiliyor [YÜKSEK]

**Dosyalar:**
- `LauncherActivity.kt:205-211,323-338`
- `LauncherViewModel.kt:1165-1177`

**Sorun:**
- `onCreate` async sync başlatıyor, hemen `onResume` ikinci sync başlatabiliyor
- Sonuç beklenmeden zaman damgası yazılıyor

**Etki:** Aynı sorgu iki kez, eğer ikincisi başarısız olsa bile başarılı kaydediliyor

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

## P2 — Performans & Bildirim (5 Yüksek)

### ✅ 17 — All Apps fuzzy arama UI thread'inde çalışıyor [YÜKSEK]

**Dosyalar:**
- `AllAppsDrawerUtils.kt:230-279`
- `AllAppsDrawer.kt:1257-1261`

**Sorun:**
- Filtre, kategori eşleştirme, edit-distance fuzzy, sıralama `remember` bloğunda senkron
- 680 uygulama üzerinde her tuş basıştada

**Etki:** Arama sırasında UI freeze

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 18 — Tablette aynı anda iki arama yüzeyi görünüyor [YÜKSEK → ORTA downgrade]

**Dosyalar:**
- `HomeScreen.kt:701-737,862-899`
- `AllAppsDrawer.kt:1334-1350`

**Sorun:**
- Drawer sağ panel olarak açılırken global arama arkada kalıyor
- Scrim dokunmayı engelliyor ama görseli kapatmıyor

**Etki:** Görsel ve kavramsal çoğalmış hissiyat

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 19 — Widget öğesi sorgusunun binder çağrısı Main thread'de [YÜKSEK → ORTA]

**Dosyalar:**
- `LauncherViewModel.kt:1215-1221`
- `WidgetSuggestionEngine.kt:18-30`

**Sorun:**
- `AppWidgetManager.installedProviders` Flow map içinde Main thread'de çalışıyor

**Etki:** Binder çağrısı ana thread'i bloklayabilir

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 20 — FolderTile ikonları PackageManager sorgusu composition'da [YÜKSEK → ORTA]

**Dosyalar:**
- `FolderTile.kt:457-478`

**Sorun:**
- `getPackageInfo(...).lastUpdateTime` `remember` bloğunda
- Main thread binder çağrısı

**Etki:** Ikon yükleme sırasında UI gecikme

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 21 — SharedPreferences listener temizlenmiyor [YÜKSEK → ORTA]

**Dosyalar:**
- `LauncherViewModel.kt:978-986`

**Sorun:**
- `smartTickerPrefsListener` Application'a kaydediliyor
- ViewModel destroy edildiğinde unregister yok

**Etki:** Eski listener'lar kalabilir, çift işlem

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

## P3 — Görsel & UX & Kod Sağlığı (8 Orta)

### ✅ 22 — Dashboard, grid ve dock farklı eksenler kullanıyor [ORTA]

**Dosyalar:**
- `HeroDashboardPage.kt:50-60`
- `FolderPager.kt:143-152`
- `HomeScreen.kt:812-830`

**Sorun:**
- Dashboard: `HomeHeroLayoutPolicy` padding
- Grid: `16.dp`
- Dock: `10.dp + fillMaxWidth`

**Etki:** Görsel hizalama tutarsız

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 23 — Indicator dokunma hedefi `48dp` altında [ORTA]

**Dosyalar:**
- `HomeScreenPageIndicator.kt:90-104`

**Sorun:**
- Item `28.dp × 48.dp`
- Yatay erişilebilirlik minimumu `48.dp` değil

**Etki:** Yanlış sayfa dokunma riski

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 24 — Reduce motion açıkken sayfa animasyonu devam ediyor [ORTA]

**Dosyalar:**
- `HomeScreenPageIndicator.kt:58-62,93-100`

**Sorun:**
- Reduce motion açıkken `animateScrollToPage` hâlâ çalışıyor

**Etki:** Sistem erişilebilirlik ayarı yok sayılıyor

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 25 — Rozet renk motorunun gri yolu ulaşılamaz [ORTA]

**Dosyalar:**
- `BadgeColorEngine.kt:8-22,78-90`

**Sorun:**
- Dokümantasyon: "Diğer = gri"
- Gerçek `else`: kırmızı

**Etki:** Tanınmayan bildirimler acil uyarı gibi kırmızı görünüyor

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 26 — "Son bildirimler" zaman penceresi ViewModel ömrü boyunca sabit [ORTA]

**Dosyalar:**
- `LauncherViewModel.kt:278-281`

**Sorun:**
- Cutoff yalnız ViewModel oluşturulurken hesaplanıyor
- Launcher günlerce açık kalırsa pencere ilerlemez

**Etki:** Eski notification'lar "yakın zamanlı" listede kalabilir

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 27 — Kategori ekranı hatayı göstermeden temizliyor [ORTA]

**Dosyalar:**
- `CategoryEditorScreen.kt:105-109`

**Sorun:**
- Hata `LaunchedEffect` ile doğrudan temizleniyor
- Snackbar veya dialog yok

**Etki:** Hatalar sessizce kaybolur

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 28 — Kategori silme onaysız ve geri alınamaz [ORTA]

**Dosyalar:**
- `CategoryEditorScreen.kt:80-88,166-174`

**Sorun:**
- Çöp ikonuna bas → doğrudan sil
- N uygulama kapsamı gösterilmiyor

**Etki:** Kazayla silme riski

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 29 — Bozuk kategori rengi ekranı çökertebilir [ORTA]

**Dosyalar:**
- `CategoryEditorScreen.kt:159-165`

**Sorun:**
- `Color.parseColor(category.colorHex)` try/catch yok
- `IllegalArgumentException` composition'ı çökertebilir

**Etki:** Restore veya el değişikliğinde crash

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

### ✅ 30 — README güncel kodla uyuşmuyor [ORTA]

**Dosyalar:**
- `README.md:25-33,81-87`

**Sorun:**
- Dock 4 slot olarak tanımlı (5 gerçek)
- Sürükleme çalışıyor diyor (çalışmıyor)
- Eski dosya yapısı gösterilmiş

**Etki:** Belgelendirme hatalı

**Çözüm Gereken:** YES  
**Durum:** ⏳

---

## Hata Olmayan Alanlar (Doğrulanmadı)

### ✅ Dock'ta dört hardcoded slot [KANIT YOK]
Mevcut kod beş slot destekliyor.

### ✅ All Apps ikon yükleme ana thread'i blokluyor [KISMEN DOĞRU]
İkon yükleme IO thread'de. Asıl problem fuzzy filtre UI thread'de.

### ✅ Planner boş sayfa üretiyor [KANIT YOK]
Klasörler `chunked()` ile bölünüyor, boş chunk yok.

---

## Çözüm Sırası

**Paralel Agentlar:**
1. Agent A: Kritik + P0.1-P0.6 (Veri kaybı)
2. Agent B: P0.7-P0.16 (Ana ekran)
3. Agent C: P1.17-P1.21 (Performans)
4. Agent D: P2.22-P2.30 (Görsel/UX)

**Tüm agent'lar eş zamanlı çalışacak.**

---

**Güncelleme:** Tamamlanan sorun sayısı güncellenecek

