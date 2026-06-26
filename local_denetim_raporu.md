# Local Denetim Raporu

> Son dongu: `2026-06-27 02:28`
> Kapanan maddeler local_denetim_tamamlananlar.md dosyasina tasinir.

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK | 3 | Mantik hatasi, izin ve gesture sorunu |
| ORTA | 6 | State tutarsizligi, validasyon, UX karisikligi |
| DUSUK | 14 | Erisilebilirlik, kod duzeni |
| INFO | 1 | Uygulanmamis veya gelecege donuk not |
| TOPLAM | 24 | |

---

## 1. LauncherActivity + Ana Ekran / Folder / All-Apps Denetimi

### [ORTA] [A6] AllAppsDrawer drag gesture scroll ile cakisiyor
**Dosya:** `AllAppsDrawer.kt` (satir 641-649)
**Sorun:** `detectVerticalDragGestures` kapanis icin kullaniliyor ama liste ustteyken drag consume ediliyor, scroll engelleniyor.
**Etki:** Kullanici listenin ustunde ikenicerigi asagi kaydiramaz, drawer'i kapanis icin swipleyemez.
**Oneri:** Scroll state kontrolu ekle: liste ustteyse ve yukari drag ise consume et, yoksa scroll'a izin ver.

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

### [DUSUK] [A14] SwipeHint icon contentDescription null
**Dosya:** `HomeScreenComponents.kt` (satir 521-526)
**Sorun:** `contentDescription = null` — klavye oku icon'u erisilebilir degil.
**Etki:** TalkBack kullanicilar animated arrow'in ne oldugunu anlamaz.
**Oneri:** `contentDescription = "Yukari kaydirma ipucu"` ekle.

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

### [YUKSEK] [P2] UsageStats izni icin onborda yonlendirme yok
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/utils/UsageStatsHelper.kt` (satir 24-29)
**Sorun:** `PACKAGE_USAGE_STATS` izni `ACTION_USAGE_ACCESS_SETTINGS` ile verilmeli; onborda bu adim veya yonlendirme yok.
**Etki:** Kullanici onborda ilerlerken UsageStats iznini vermez; oneri sistemi calismaz ama kullaniciya bilgi verilmez.
**Oneri:** Onboarding'a UsageStats adimi ekle ve `UsageStatsHelper.openPermissionSettings()` ile yonlendir.

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

---

*Denetim tarihi: 2026-06-27 | Denetim tipi: Yüksek öncelikli launcher, izin akisi ve kategori CRUD alanlari*
