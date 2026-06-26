# Local Denetim Raporu

> Otomatik denetim — son kullanıcıya görünen katman odaklı.
> **ONAY GEREKİYOR** — değişiklik yapılmadı, sadece rapor.
> Tespit edilen hatalar maddeler halinde listelenmiştir. Bir sonraki döngüde düzeltilecektir.

---

## 📊 Denetim Özeti

| Öncelik | Sayı | Açıklama |
|---------|------|----------|
| 🔴 KRİTİK | 1 | Kullanıcı verisi kaybı / yanlış işlem riski |
| 🟠 YÜKSEK | 4 | UI çalışmaz, toggle etkisiz, arama tutarsız |
| 🟡 ORTA | 5 | Geçici state tutarsızlığı, cache eksikliği, performans |
| 🟢 DÜŞÜK | 3 | Kod kalitesi, minor UX, log eksikliği |
| **TOPLAM** | **13** | |

---

## 🔴 KRİTİK

### K1 — AllAppsDrawer sort mode ikinci SharedPreferences dosyasına yazılıyor

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/AllAppsDrawer.kt` (satır 213-214)
**Sorun:** `DrawerSearchBar` içinde sıralama modu `"app_organizer_prefs"` adında hardcoded bir SharedPreferences dosyasına yazılıyor. `AppPrefs.PREFS_NAME` kullanılmıyor. Bu durum:
- Ayarlar ekranından gelen `all_apps_sort_mode` değişiklikleri görünmüyor
- Kullanıcı sıralama tercihi kayboluyor
- İki farklı pref dosyası arasında tutarsızlık

**Etki:** Kullanıcı AllApps'te sıralama modunu seçtikten sonra ayarlara geri döndüğünde tercih kaybolur.
**Öneri:** `context.getSharedPreferences(com.armutlu.apporganizer.utils.AppPrefs.PREFS_NAME, ...)` kullan ve `AppPrefs` içine `getAllAppsSortMode`/`setAllAppsSortMode` metotları ekle.

---

## 🟠 YÜKSEK

### Y1 — AppListScreen araması Türkçe locale duyarsız

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/AppListScreenState.kt` (satır 140-186)
**Sorun:** `fuzzySearch()` fonksiyonu `query.lowercase()` ve `appName.lowercase()` olarak `Locale.getDefault()` kullanıyor. AllAppsDrawer ise `Locale("tr")` ile arama yapıyor. Türkçe İ/ı/ğ/ü/ö/ç karakterleri için farklı sonuçlar üretir.

**Etki:** Kullanıcı "ı" veya "ğ" içeren bir uygulama adını aratırken AppListScreen'de bulamaz, AllApps'te bulur.
**Öneri:** `fuzzySearch()` içindeki tüm `lowercase()` çağrılarını `lowercase(Locale("tr"))` ile değiştir.

### Y2 — HomeScreen klasör arama sayacı race condition

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreen.kt` (satır 258-268)
**Sorun:** `LaunchedEffect(folderSearchQuery)` içinde `repeat(30)` ile 1 saniye bekleyen bir sayaç var. Kullanıcı hızlıca yazıyorsa her karakter için yeni bir `LaunchedEffect` başlar. Eski sayaçlar iptal edilmez (key değişmediği için), birden fazla sayaç aynı anda çalışır ve `folderSearchCountdown` üzerinde yarışır.

**Etki:** Kullanıcı arama yaparken sayaç rastgele sıfırlanabilir, hızlı yazarken arama aniden kaybolabilir.
**Öneri:** `LaunchedEffect(folderSearchQuery)` yerine `var job by remember { mutableStateOf<Job?>(null) }` ve `LaunchedEffect(Unit)` içinde `snapshotFlow { folderSearchQuery }` kullan, veya `debounce` ile arama tetikle.

### Y3 — FolderTile swipeDy non-state — recomposition'da sıfırlanır

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/FolderTile.kt` (satır 82, 109-121)
**Sorun:** `swipeDy` `mutableFloatStateOf` değil, normal `var`. Amaç recomposition tetklememek. Ancak Composable herhangi bir nedenle yeniden derlendiğinde (örn. parent'ın state değişimi) `swipeDy` 0f'a sıfırlanır. Bu durumda kullanıcı yavaşça sürüklerken aniden swipe iptal olur.

**Etki:** Kullanıcı klasörü yukarı sürüklemeye çalışırken UI yeniden çizilirse swipe gesture aniden kesilir, uygulama açılmaz.
**Öneri:** `swipeDy`'yi `mutableFloatStateOf(0f)` yap veya `pointerInput` callback'inde kümülatif delta'yı doğrudan kullan (state olmadan).

### Y4 — SettingsScreen `isDefaultLauncher()` her recomposition'da PackageManager sorgular

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsScreen.kt` (satır 66-70)
**Sorun:** `isDefaultLauncher()` fonksiyonu Composable gövdesinde direkt çağrılıyor. Her recomposition'da `PackageManager.resolveActivity()` çalışır. Bu pahalı bir sistem çağrısıdır.

**Etki:** Ayarlar ekranı her açılışta/kaydırma yapıldıkça PackageManager sorgusu tetiklenir. Görünürde sorun yok ama gereksiz sistem yükü.
**Öneri:** `remember { isDefaultLauncher() }` ile önbelleğe al veya `LaunchedEffect` ile periyodik kontrol et.

---

## 🟡 ORTA

### O1 — AppListScreen kategori listesi her recomposition'da yeniden hesaplanıyor

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/AppListScreen.kt` (satır 181)
**Sorun:** `items(screenState.categories.filter { ... }.sortedBy { ... })` her recomposition'da kategori listesi filtrelenip sıralanıyor. Kategoriler nadiren değişir.

**Etki:** Performans kaybı — özellikle çok kategori varsa her kaydırma/sayaç değişikliğinde sıralama tekrar çalışır.
**Öneri:** Filtreleme ve sıralama işlemini ViewModel'a taşı, sonucu `StateFlow` olarak expose et.

### O2 — DrawerRecentFavSection ikon cache anahtarı `lastUpdatedTime` içermiyor

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/AllAppsDrawer.kt` (satır 372)
**Sorun:** Favori uygulama ikon cache anahtarı `"${pkg}_48_$iconPackPkg"` formatında. HomeScreenFavorites.kt'deki anahtar `"${pkg}_48_${lastUpdatedTime}_$iconPackPkg"` formatında. Uygulama güncellendiğinde HomeScreen'de ikon yenilenir ama AllApps drawer'da eski ikon kalır.

**Etki:** Uygulama güncellenince AllApps drawer'daki favori ikonları eski kalır, aniden değişir veya bozulur.
**Öneri:** Cache anahtar formatını her yerde aynı yap — `lastUpdatedTime` ekle.

### O3 — AppListViewModel `classifier.manufacturerClassifyEnabled` global state mutasyonu

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/viewmodel/AppListViewModel.kt` (satır 340)
**Sorun:** `classifier` Hilt `@Singleton` ile inject ediliyor. `manufacturerClassifyEnabled = ...` direkt olarak singleton üzerinde değiştiriliyor. Bu tüm ViewModel instance'larını ve tüm ekranları etkiler.

**Etki:** Kullanıcı bir ekranda üretici sınıflandırmayı kapatırsa, tüm ekranlarda geçersiz olur. Geri döndüğünde beklenmedik davranış.
**Öneri:** `AppClassifier` içine `classifyApp()` çağrısı sırasında flag geçmek için parametre ekle, ya da flag'i `AppPrefs`'e taşı.

### O4 — HomeScreen `folderSearchCountdown` kullanıcı arama yaparken kaybolabilir

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreen.kt` (satır 258-268)
**Sorun:** Kullanıcı arama yaparken her karakter değişikliği `folderSearchCountdown`'u 30'a sıfırlar. Ancak `repeat(30)` içindeki `delay`'ler hala devam ediyor olabilir. Eski sayaçlar `folderSearchQuery = ""` yapabilir, kullanıcı hala yazıyor.

**Etki:** Kullanıcı yazarken arama aniden sıfırlanabilir, yazdığı metin kaybolur.
**Öneri:** Sayaç job'unu `remember` ile sakla, yeni arama için eski job'u `cancel()` et.

### O5 — AppListScreenState `filteredApps` getter'ı `apps` üzerinde in-place filtreleme yapmıyor

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/AppListScreenState.kt` (satır 39-68)
**Sorun:** `filteredApps` getter'ı her çağrıldığında `apps` listesini filtreler ve sıralar. `apps` değişmediği halde her `collectAsState` veya `get()` çağrısında tekrar çalışır.

**Etki:** Görünürde sorun yok ama gereksiz CPU kullanımı — özellikle büyük uygulama listesinde her recomposition'da 4-5 geçiş (filter+sort) çalışır.
**Öneri:** Filtreleme ve sıralama işlemini ViewModel'da yap, filtrelenmiş sonucu `StateFlow` olarak sun.

---

## 🟢 DÜŞÜK

### D1 — Kullanılmayan parametre uyarısı: `itemHeightDp`

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreenComponents.kt` (satır 281, 400, 454)
**Sorun:** `AppSuggestionsRow`, `FavoritesRow`, `RecentAppsRow` fonksiyonları `itemHeightDp` parametresi alıyor ama hiç kullanmıyor. Derleyici uyarısı veriyor.

**Etki:** Derleme uyarısı, kod kalitesi.
**Öneri:** Parametreyi kaldır veya gerçekten kullan (örn. `Modifier.height(itemHeightDp)`).

### D2 — SettingsScreen istatistik bölümü `getCategoryStats()` her recomposition'da map oluşturur

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsScreen.kt` (satır 296-298)
**Sorun:** `state.getCategoryStats().maxByOrNull { ... }` her recomposition'da yeni bir Map oluşturur ve sonra max arar. Çok küçük bir performans kaybı.

**Etki:** Çok küçük — sadece gereksiz nesne oluşturma.
**Öneri:** `topCategory`'yi `remember` ile önbelleğe al veya ViewModel'a taşı.

### D3 — `isLoading` değişkeni HomeScreen'de hiç kullanılmıyor

**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreen.kt` (satır 249)
**Sorun:** `val isLoading = folders.isEmpty() && allApps.isEmpty()` tanımlanıyor ama hiçbir yerde referans edilmiyor.

**Etki:** Görünürde sorun yok — ölü kod.
**Öneri:** Kaldır veya kullan (loading state göstergesi için).

---

## 📋 Maddeler Halinde Düzeltme Listesi (Sonraki Döngü İçin)

1. [ ] AllAppsDrawer sort mode için `AppPrefs` kullan (K1)
2. [ ] `fuzzySearch()` Türkçe locale duyarlı hale getir (Y1)
3. [ ] HomeScreen klasör arama sayacını race-condition'dan kurtar (Y2)
4. [ ] FolderTile `swipeDy` state yapısını düzelt (Y3)
5. [ ] SettingsScreen `isDefaultLauncher()` önbelleğe al (Y4)
6. [ ] AppListScreen kategori listesi hesaplamasını ViewModel'a taşı (O1)
7. [ ] AllApps drawer ikon cache anahtarına `lastUpdatedTime` ekle (O2)
8. [ ] `classifier.manufacturerClassifyEnabled` global mutasyonu AppPrefs'e taşı (O3)
9. [ ] HomeScreen arama sayacı job cancel mekanizması ekle (O4)
10. [ ] `filteredApps` hesaplamasını ViewModel'a taşı (O5)
11. [ ] Kullanılmayan `itemHeightDp` parametrelerini kaldır veya kullan (D1)
12. [ ] SettingsScreen `topCategory` hesaplamasını önbelleğe al (D2)
13. [ ] Kullanılmayan `isLoading` değişkenini kaldır (D3)

---

## 🔍 Denetim Yöntemi

- Manuel kod okuma: 12 kritik dosya (HomeScreen, AllAppsDrawer, FolderTile, SettingsScreen, AppListScreen, AppListViewModel, AppListScreenState, AppRepository, LauncherActivity, HomeScreenComponents, HomeScreenFavorites, AppNotificationListenerService)
- Her dosya için: mantık akışı, state yönetimi, locale tutarlılığı, cache anahtarları, null safety, race condition riski
- Derleme başarısız olan dosyalar öncelikli incelendi
- Önceki denetim raporları (`MD_DENETIM_2026-06-23.md`) referans alındı

---

*Denetim tarihi: 2026-06-26 | Denetçi: Otomatik sistem + Kilo | Sonraki denetim: Bir sonraki döngüde*
