# Local Denetim Raporu

> Son dongu: `2026-06-27 01:46`
> Kapanan maddeler local_denetim_tamamlananlar.md dosyasina tasinir.

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK | 12 | Mantik hatasi, erisilebilirlik, izin akisi sorunu |
| ORTA | 14 | State tutarsizligi, validasyon, UX karisikligi |
| DUSUK | 9 | Erisilebilirlik, kod duzeni |
| TOPLAM | 35 | |

---

## 1. LauncherActivity + Ana Ekran / Folder / All-Apps Denetimi

### [YUKSEK] [A1] Home press timing state configuration change'de kayboluyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/LauncherActivity.kt` (satir 31-32)
**Sorun:** `lastHomePressMs` ve `receiverRegistered` plain mutable degiskenler; saved state handle ile saklanmiyor.
**Etki:** Ekran yon degistirdikten sonra cift-tap home ile All Apps acma islemi basarisiz olur.
**Oneri:** State'i `SavedStateHandle` veya `rememberSaveable` ile yonet.

### [YUKSEK] [A2] SharedPreferences listener activity recreate'de kaldirilmiyor
**Dosya:** `LauncherActivity.kt` (satir 205-208)
**Sorun:** `receiverRegistered` flag var ama `OnSharedPreferenceChangeListener` kaydi/yazimi DisposableEffect ile compose lifecycle'a bagli; activity recreate durumunda yonetim zayif.
**Etki:** Bellek sizintisi veya cift callback kaydi olusabilir.
**Oneri:** DisposableEffect'in listener kaydetme/birakma dongusunu dogrula veya singleton observer kullan.

### [ORTA] [A3] swipeDelta configuration change'de sifirlaniyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreen.kt` (satir 212)
**Sorun:** `var swipeDelta = 0f` plain property; config change'de her recomposition'ta sifirlanir.
**Etki:** Swipe-to-open All Apps gesture ilerlemesi yon degistirmeden sonra kaybolur.
**Oneri:** `swipeDelta`'yi `rememberSaveable` ile sakla.

### [ORTA] [A4] AppContextMenu favorite state ViewModel ile senkronize degil
**Dosya:** `AppContextMenu.kt` (satir 74, 241-248)
**Sorun:** `var isFav` lokal state SharedPrefs'ten baslatilir; `onToggleFavorite` callback ViewModel'e gunceller ama UI state asenkron guncellenir.
**Etki:** Hizli toggle'lar veya process olumunden sonra favorite tik durumu yanlis gorunebilir.
**Oneri:** Lokal `isFav` kaldir; ViewModel `favoriteApps` flow'una gore derive et.

### [ORTA] [A5] FolderRenameDialog bos isimle kaydetmeye izin veriyor
**Dosya:** `FolderRenameDialog.kt` (satir 119)
**Sorun:** Confirm butonu `nameField.isNotBlank()` kontrolu yapar ama bos isim kaydedilirse eski kategori adi gorunur; kullanici yanlis anlar.
**Etki:** Kullanici bos isimle kaydeder ve klasor adi degismemis gibi gorunur.
**Oneri:** Butonu `enabled = nameField.isNotBlank()` yap veya hata mesaji goster.

### [ORTA] [A6] AllAppsDrawer drag gesture scroll ile cakisiyor
**Dosya:** `AllAppsDrawer.kt` (satir 641-649)
**Sorun:** `detectVerticalDragGestures` kapanis icin kullaniliyor ama liste ustteyken drag consume ediliyor, scroll engelleniyor.
**Etki:** Kullanici listenin ustunde ikenicerigi asagi kaydiramaz, drawer'i kapanis icin swipleyemez.
**Oneri:** Scroll state kontrolu ekle: liste ustteyse ve yukari drag ise consume et, yoksa scroll'a izin ver.

### [ORTA] [A7] WidgetArea drag reordering tahmini boyut hatasi
**Dosya:** `WidgetArea.kt` (satir 86-87)
**Sorun:** `120 * 3f` hardcoded estimated height; gercek widget boyutu degisir.
**Etki:** Drag sirasinda yanlis drop hedefi hesaplanir, widget yanlis siraya duser.
**Oneri:** Gercek olculeri kullan veya daha dogru bir measurement stratejisi belirle.

### [DUSUK] [A8] NiagaraAppRow semantics notification count eksik
**Dosya:** `NiagaraComponents.kt` (satir 85-87)
**Sorun:** contentDescription sadece app name ve category iceriyor; notification count yok.
**Etki:** TalkBack kullanicilar badge sayisini duymaz.
**Oneri:** contentDescription'a notification count ekle.

### [DUSUK] [A9] DockIcon clickable Column semantics eksik
**Dosya:** `HomeScreenComponents.kt` (satir 261-266)
**Sorun:** Column uzerinde clickable var ama semantics modifier eksik; TalkBack clear app name vermeyebilir.
**Etki:** Erisilebilirlik denetimi kismi karsilanmaz.
**Oneri:** Column'a `Modifier.semantics { contentDescription = appName }` ekle.

### [DUSUK] [A10] SuggestionAppItem fallback Box semantics eksik
**Dosya:** `HomeScreenComponents.kt` (satir 370-375)
**Sorun:** Icon yoksa fallback Box'un semantics'i bos.
**Etki:** TalkBack kullanicilar icon olmayan uygulamalar icin "Box" olarak okur.
**Oneri:** Fallback Box'a `Modifier.semantics { contentDescription = appName }` ekle.

### [DUSUK] [A11] FavoritesRow ve RecentAppsRow items semantics eksik
**Dosya:** `HomeScreenComponents.kt` (satir 411-445, 463-494)
**Sorun:** `combinedClickable` kullaniliyor ama explicit semantics yok; TalkBack okuma sirasi belirsiz.
**Etki:** Kullanici hangi uygulamaya basacagini anlamakta zorlanabilir.
**Oneri:** Her item Column'una `Modifier.semantics { contentDescription = appName; role = Role.Button }` ekle.

### [DUSUK] [A12] FolderTile swipe hint "^" sembolui erisilebilir degil
**Dosya:** `FolderTile.kt` (satir 224-227)
**Sorun:** `"^ ${topApp.appName}"` metni caret sembolunu kullaniyor; erisilebilirlik acisindan anlamli degil.
**Etki:** TalkBack kullanicilar gesture ipucunu anlamaz.
**Oneri:** Daha anlasilir icon veya semantics aciklamasi ekle.

### [DUSUK] [A13] Search history chips accessibility eksik
**Dosya:** `AllAppsDrawer.kt` (satir 161-167)
**Sorun:** History chips clickable ama semantics eksik.
**Etki:** TalkBack kullanicilar chip'in tiklanabilir oldugunu anlamaz.
**Oneri:** `Modifier.semantics { onClick(label = "Ara") }` ekle.

### [DUSUK] [A14] SwipeHint icon contentDescription null
**Dosya:** `HomeScreenComponents.kt` (satir 521-526)
**Sorun:** `contentDescription = null` — klavye oku icon'u erisilebilir degil.
**Etki:** TalkBack kullanicilar animated arrow'in ne oldugunu anlamaz.
**Oneri:** `contentDescription = "Yukari kaydirma ipucu"` ekle.

### [DUSUK] [A15] DrawerAppList header heading semantics eksik
**Dosya:** `AllAppsDrawer.kt` (satir 262)
**Sorun:** `NiagaraLetterHeader`'da `Modifier.semantics { heading() }` yok.
**Etki:** TalkBack kullanicilar alfabetik bolumler arasinda hizli gezinemez.
**Oneri:** Header'a `Modifier.semantics { heading() }` ekle.

### [DUSUK] [A16] HomeScreenPageIndicator focus order eksik
**Dosya:** `HomeScreenPageIndicator.kt` (satir 33)
**Sorun:** Page indicator contentDescription var ama proper focus/order semantics yok.
**Etki:** TalkBack focus sayfa gostericinde tahmin edilemez sekilde atlayabilir.
**Oneri:** `Modifier.semantics { role = Role.Tab }` ekle.

### [DUSUK] [A17] MiniAppIcon fallback contentDescription eksik
**Dosja:** `FolderTile.kt` (satir 347-362)
**Sorun:** Fallback Box (ilk harf) icin contentDescription yok.
**Etki:** TalkBack "Box" olarak okur, uygulama adini soylemez.
**Oneri:** `Modifier.semantics { contentDescription = appName }` ekle.

### [DUSUK] [A18] FolderSheet notification row tappable semantics eksik
**Dosya:** `FolderSheet.kt` (satir 198-201)
**Sorun:** Semantics sadece app name ve notification count iceriyor; tiklanabilir oldugu belirtilmiyor.
**Etki:** TalkBack kullanicilar notification satirina tiklayarak uygulama acilacagini anlamaz.
**Oneri:** `Modifier.semantics { onClick(label = "Uygulamayi ac") }` ekle.

### [DUSUK] [A19] WidgetCard drag handle erisilebilir aciklama eksik
**Dosya:** `WidgetArea.kt` (satir 175)
**Sorun:** Drag handle icon contentDescription "Tasi" ama reordering baslattigi anlasilmiyor.
**Etki:** TalkBack kullanicilar drag handle'in amacini anlamaz.
**Oneri:** Daha aciklayici contentDescription ekle.

---

## 2. FolderTile + Swipe / Drawer Denetimi

### [YUKSEK] [S1] FolderTile ve HomeScreen'de cakisan swipe gesture handling
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/FolderTile.kt` (satir 106-122)
**Sorun:** `detectVerticalDragGestures` FolderTile'da `swipeDy` kullaniyor; HomeScreen'de de benzer pointerInput handler var. Gesture cakismasi riski.
**Etki:** Kullanici klasor uzerinde yukari kaydirirken inconsistent davranis veya cift tetikleme gorur.
**Oneri:** Swipe detection'i tek bir yerde topla veya `pointerInput` key'lerini ayarla.

### [YUKSEK] [S2] swipeDy drag baslangicinda sifirlanmiyor
**Dosya:** `FolderTile.kt` (satir 82, 106-122)
**Sorun:** `swipeDy` sadece `onDragEnd/onDragCancel`'de sifirlaniyor; `onDragStart`'te sifirlanmiyor.
**Etki:** Kesilen bir swipe sonrasinda sonraki swipe beklenenden erken veya gec tetiklenir.
**Oneri:** `onDragStart`'da `swipeDy = 0f` yap.

### [ORTA] [S3] Birden fazla vertical drag detector pointer consumption yok
**Dosya:** `HomeScreen.kt` (satir 351-388)
**Sorun:** Kok Box'da hem `detectTapGestures` hem `detectVerticalDragGestures` + `nestedScrollConnection` var; pointer consumption koordinasyonu yok.
**Etki:** Drawer acma/kapama davranisi titrek veya ongorulebilir olmayan threshold'lara sahip olabilir.
**Oneri:** Tek bir gesture detector kullan veya nested scroll ile pointer input arasinda proper consumption propagation yap.

### [ORTA] [S4] FolderTile icinde semantics eksik — TalkBack erisilebilir degil
**Dosya:** `FolderTile.kt` (satir 134-275)
**Sorun:** Folder Column'unda semantics modifier yok; clickable ve longClick var ama screen reader icin aciklama yok.
**Etki:** Gorme engelli kullanicilar klasorun amacini veya icindeki uygulama sayisini anlayamaz.
**Oneri:** `Modifier.semantics { contentDescription = "$folderName, $count uygulama, yukari kaydirarak en cok kullanilani ac" }` ekle.

### [ORTA] [S5] SwipeHint animasyonu erisilebilir duyuru yok
**Dosya:** `HomeScreenComponents.kt` (satir 497-534)
**Sorun:** SwipeHint composable'inda accessibility services icin semantics yok; sadece gorsel feedback var.
**Etki:** Screen reader kullanicilar yukari kaydirma gesture'inden habersiz olur.
**Oneri:** `Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "Yukari kaydirarak tum uygulamalari ac" }` ekle.

### [DUSUK] [S6] Farkli swipe threshold'lari birbirinden farkli birimlerde
**Dosya:** `FolderTile.kt` (satir 83, 112) ve `HomeScreen.kt`
**Sorun:** FolderTile 40dp, HomeScreen 80dp, pointerInput 60px — birimler ve degerler tutarsiz.
**Etki:** Kullanici klasor ve ana ekranda farkli hassasiyetle karsilasir.
**Oneri:** Tum swipe threshold'larini ayni birimde (dp) ve ayni degerde standartlastir.

### [DUSUK] [S7] AllAppsDrawer SWIPE_DOWN_THRESHOLDLocalDensity eksik
**Dfsya:** `AllAppsDrawer.kt` (satir 67)
**Sorun:** `SWIPE_DOWN_THRESHOLD = 90f` raw pixel; screen density'e gore degismeli.
**Etki:** Farkli ekran yogunluklarinda kapanis swipe mesaresi farkli hissettirilir.
**Oneri:** `with(LocalDensity.current) { 90.dp.toPx() }` kullan.

### [INFO] [S8] Kilitli/kilitsiz kategori davranisi mevcut degil
**Dosya:** `Category.kt` (satir 11-50)
**Sorun:** `isSystemCategory` alani var ama `locked` alani yok; FolderTile/FolderSheet'te restrict erisim yok.
**Etki:** Denetim kapsaminda bu ozellik henuz implemente edilmemis.
**Oneri:** Gerekiyorsa `Category` modeline `locked: Boolean` ekle ve FolderTile click korumasini uygula.

---

## 3. PermissionHelper + Izin Akisi Denetimi

### [YUKSEK] [P1] QUERY_ALL_PACKAGES runtime olarak istenmiyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/utils/PermissionHelper.kt` (satir 16-35)
**Sorun:** `QUERY_ALL_PACKAGES` `REQUIRED_PERMISSIONS`'te tanimli ama kodun hicbir yerinden `requestPermissions()` cagrilmiyor.
**Etki:** Kullanici izin isteme dialog'u gormez; manifest'te izin olsa bile runtime verilmezse app cokmaz ama eksik paket gorunur.
**Oneri:** Onboarding veya ilk acilista `PermissionHelper.requestPermissions()` cagrisini ekle.

### [YUKSEK] [P2] UsageStats izni icin onborda yonlendirme yok
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/utils/UsageStatsHelper.kt` (satir 24-29)
**Sorun:** `PACKAGE_USAGE_STATS` izni `ACTION_USAGE_ACCESS_SETTINGS` ile verilmeli; onborda bu adim veya yonlendirme yok.
**Etki:** Kullanici onborda ilerlerken UsageStats iznini vermez; oneri sistemi calismaz ama kullaniciya bilgi verilmez.
**Oneri:** Onboarding'a UsageStats adimi ekle ve `UsageStatsHelper.openPermissionSettings()` ile yonlendir.

### [YUKSEK] [P3] PermissionHelper class hic cagirilmiyor — dead code
**Dosya:** `PermissionHelper.kt` (satir 14)
**Sorun:** `PermissionHelper` sınıfı tanimlanmis ama hicbiryerde `new PermissionHelper()` veya `@Inject` ile kullanilmiyor.
**Etki:** Izın kontrol mekanizmasi denetim dısinda; manuel tekrar tekrar kontrol ediliyor.
**Oneri:** PermissionHelper'i entegre et veya gereksizse kaldir.

### [YUKSEK] [P4] POST_NOTIFICATIONS kalici rette fallback yok
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/OnboardingScreen.kt` (satir 98-99)
**Sorun:** `shouldShowRequestPermissionRationale` kontrolu eksik; "Asla sorma" secince onboarding sonsuz donguye girer.
**Etki:** Kullanici izni reddederse onboarding tamamlanamaz, uygulama kullanilamaz hale gelir.
**Oneri:** `shouldShowRequestPermissionRationale` kontrolu ekle ve `ACTION_APPLICATION_DETAILS_SETTINGS` ile ayarlara yonlendir.

### [YUKSEK] [P5] PermissionsBanner QUERY_ALL_PACKAGES durumunu gostermiyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/PermissionsBanner.kt` (satir 82-125)
**Sorun:** Banner sadece notification, launcher ve notification listener kontrol eder; QUERY_ALL_PACKAGES yok.
**Etki:** Kullanici QUERY_ALL_PACKAGES iznini reddederse banner'da gorunmez, sorun tespit edilemez.
**Oneri:** Banner'a `ContextCompat.checkSelfPermission(QUERY_ALL_PACKAGES)` kontrolu ekle.

### [ORTA] [P6] syncUsageStats izin yokken sessizce donuyor
**Dosya:** `LauncherViewModel.kt` (satir 500-512)
**Sorun:** `syncUsageStats()` icinde `UsageStatsHelper.hasPermission()` false ise sessizce return; kullaniciya bilgi yok.
**Etki:** Oneri sistemi calismaz ama kullanici bunun sebebini gormez.
**Oneri:** ViewModel'den event firlat veya banner ile uyar.

### [ORTA] [P7] Onboarding QUERY_PACKAGES adimi skippable degil ama gerekli degil
**Dosya:** `OnboardingModels.kt` (satir 62-68)
**Sorun:** `QUERY_PACKAGES` adimi `isSkippable=false` ama Android 11+ cihazlarda runtime istenemez, kullanici atlayamaz.
**Etki:** Kullanici gereksiz yere onborda takilabilir.
**Oneri:** Adimi skippable yap veya aciklayici metin ekle.

### [ORTA] [P8] NOTIF_ACCESS adimi icin skip secenegi eksik
**Dosya:** `OnboardingModels.kt` (satir 101-109)
**Sorun:** `NOTIF_ACCESS isSkippable=true` ama "Simdi Degil" secenegi SET_LAUNCHER icin gosteriliyor, NOTIF_ACCESS icin consistent degil.
**Etki:** Kullanici tekrar tekrar "Atla" butonunu aramak zorunda kalir.
**Oneri:** NOTIF_ACCESS adimi icin de consistent skip secenegi ekle.

### [ORTA] [P9] GET_INSTALLED_PACKAGES manifest'te ama kullanilmiyor
**Dosya:** `AndroidManifest.xml` (satir 9-10)
**Sorun:** `GET_INSTALLED_PACKAGES` izni tanimli ama kodun hicbir yerinde kullanilmiyor.
**Etki:** Gereksiz izin Play review surecini geciktirebilir, kullanici guveni azalir.
**Oneri:** Kullanilmiyorsa kaldir.

### [DUSUK] [P10] PermissionsBanner snooze sure hardcoded
**Dosya:** `PermissionsBanner.kt` (satir 76-79)
**Sorun:** Banner 7 gun snoozed kaliyor; magic number.
**Etki:** Kullanici banner'i kapattiktan sonra 7 gun icinde UsageStats izni vermezse uyari alir.
**Oneri:** Snooze suresini yapilandirilabilir yap.

### [DUSUK] [P11] OEM-specific package visibility restrictions yok
**Dosya:** `PackageManagerHelper.kt` (satir 56-95)
**Sorun:** OEM'ler (Samsung, Xiaomi) paketleri gizleyebilir; `<queries>` eklenmis ama runtime'da ekstra kontrol yok.
**Etki:** OEM cihazlarda bazin sistem uygulamalari gizli kalabilir.
**Oneri:** Runtime'da ekstra OEM kontrol mantigi ekle.

---

## 4. CategoryPickerDialog + Kategori CRUD Denetimi

### [YUKSEK] [C1] Kategori CRUD metotlari ViewModel/Repository'de eksik
**Dosya:** `AppListViewModel.kt`
**Sorun:** `addCategory`, `updateCategory`, `deleteCategory` metotlari yok; `CategoryDao` referansi alinmis ama kullanilmiyor.
**Etki:** Kullanici kategorileri ekleyemez, duzenleyemez, silemez; CategoryEditorScreen islevsiz kalir.
**Oneri:** Repository uzerinden categoryDao erisimi sagla ve ViewModel'de CRUD metotlari ekle.

### [YUKSEK] [C2] AddCategoryDialog'da kalici kayit yok
**Dosya:** `CategoryEditorScreen.kt` (satir 82-85)
**Sorun:** `onAdd` callback'i sadece Timber log yazip dialog kapatir; Room DB'ye kayit veya ViewModel state guncellemesi yok.
**Etki:** Kullanici "Ekle" butonuna bastiginda hicbir kayit olusmaz, kategori aniden kaybolur.
**Oneri:** `onAdd` icine `viewModel.addCategory(...)` cagrisi ekle.

### [YUKSEK] [C3] Kategoriler hardcoded getDefaultCategories() ile yukleniyor
**Dosya:** `AppListViewModel.kt` (satir 116)
**Sorun:** `initializeScreen()` icinde `Category.getDefaultCategories()` sabit listesi; `categoryDao.getAllCategoriesFlow()` kullanilmiyor.
**Etki:** Kullanici tarafindan eklenen ozel kategoriler hicbir ekranda gorunmez.
**Oneri:** Kategori listesini `categoryDao.getAllCategoriesFlow()` ile flow'dan yukle.

### [YUKSEK] [C4] Bos kategori adi validasyonu kullaniciya gosterulmuyor
**Dosya:** `CategoryEditorScreen.kt` (satir 188-197)
**Sorun:** OutlinedTextField bos birakildiginda confirm button islevsiz kaliyor, `isError=false`, hata mesaji yok, buton da disabled degil.
**Etki:** Kullanici "Ekle" butonuna tikladiginda hicbir tepki alamiyor.
**Oneri:** `categoryName.isBlank()` kontrolune gore Button'u `enabled=false` yap veya `isError=true` ile hata metni goster.

### [ORTA] [C5] Duplicate kategori olusturma engelleme yok
**Dosya:** `CategoryEditorScreen.kt` (satir 161-168)
**Sorun:** Ayni isimde yeni kategori eklenmesini onleyen kontrol yok.
**Etki:** Kullanici ayni isimde birden fazla kategori olusturabilir, veri tutarsizligi olusur.
**Oneri:** `CategoryDao.findByCategoryName()` sorgusu ekle ve duplicate kontrolu yap.

### [ORTA] [C6] Sistem kategorileri silinme korumasi eksik
**Dosya:** `CategoryDao.kt` (satir 41-42)
**Sorun:** `deleteCategoryById` ve `deleteCategory` metotlarinda `isSystemCategory` kontrolu yok.
**Etki:** Sistem kategorileri (sosyal medya, oyunlar vb.) silinebilir; uygulama kategorilere sahip tum uygulamalari kaybedebilir.
**Oneri:** DELETE sorgusuna `WHERE isSystemCategory = 0` kosulu ekle.

### [ORTA] [C7] CategoryPickerSheet hardcoded defaults kullaniyor
**Dosja:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/CategoryPickerSheet.kt` (satir 36-37)
**Sorun:** Composable icinde `Category.getDefaultCategories()` sabit cagrisi yapiyor; dışarıdan categories listesi almiyor.
**Etki:** Launcher'daki kategori secicide ozel kullanici kategorileri asla gorunmez.
**Oneri:** Composable'e `categories: List<Category>` parametresi ekle ve kullan.

### [ORTA] [C8] Iki kategori secici aracinin yapisi ve kapatma mekanizmasi tutarsiz
**Dosya:** `AppListComponents.kt` (satir 213-252) ve `CategoryPickerSheet.kt` (satir 40-95)
**Sorun:** Biri `AlertDialog`, digeri `ModalBottomSheet`; biri parent tarafindan kapatiliyor, digeri kendi icinde `onDismiss()` cagiriyor.
**Etki:** Bakim ve test zorlugu, davranis ongorebilirligi azalir.
**Oneri:** Her iki secicide de secim sonrasi state degisikligi ile Composition'dan cikma standartlastir.

### [ORTA] [C9] Dialog icon'larinda contentDescription eksik
**Dosya:** `AppListComponents.kt` (satir 241) ve `CategoryPickerSheet.kt` (satir 89)
**Sorun:** Secili kategoriyi belirten Check icon'un contentDescription'i null; tiklanabilir satirlarda semantics eksik.
**Etki:** TalkBack kullanicilar secili kategoriyi okuyamaz.
**Oneri:** Icon'a `contentDescription = "Secili"` ekle; satirlara `Modifier.semantics { role = Role.Button }` ekle.

### [ORTA] [C10] CategoryEditorScreen edit butonu islevsiz, delete butonu eksik
**Dosya:** `CategoryEditorScreen.kt` (satir 69-72, 136-140)
**Sorun:** Edit butonu sadece `Timber.d` log yaziyor; ozel kategoriler icin delete butonu yok.
**Etki:** Kullanici kategori duzenleyemez, olusturdugu kategorileri silemez; editor ekrani sadece listeleme amacli calisir.
**Oneri:** Edit butonu icin duzenleme dialogu ekle; custom kategoriler icin delete butonu ve `viewModel.deleteCategory()` cagrisi ekle.

---

*Denetim tarihi: 2026-06-27 | Denetim tipi: Yüksek öncelikli launcher, izin akisi ve kategori CRUD alanlari*
