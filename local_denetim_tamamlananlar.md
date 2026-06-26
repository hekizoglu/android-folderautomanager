# Local Denetim Raporu Tamamlananlar

## 2026-06-26 11:26

- `K1` AllApps siralama tercihi `AppPrefs` uzerinden tek prefs kaynagina tasindi.
- `Y1` `fuzzySearch()` Turkce locale ile normalize edilerek AppList ve drawer aramasi hizalandi.
- `Y2` klasor arama sayaci `snapshotFlow` ve `collectLatest` ile eski sayaclari iptal edecek hale getirildi.
- `Y3` `FolderTile` icindeki `swipeDy` recomposition guvenli Compose state oldu.
- `Y4` launcher varsayilan durumu tekrar hesaplama yerine hatirlanan state ile yonetildi.
- `O1` kategori sekmeleri ViewModel tarafinda onceden hesaplanan `visibleCategories` listesine tasindi.
- `O2` All Apps icindeki recent ve favorite ikon cache anahtarlarina `lastUpdatedTime` eklendi.
- `O3` `AppClassifier` uzerindeki global mutable flag kaldirildi; siniflandirma tercihi cagri bazli parametre oldu.
- `O4` klasor arama temizleme akisi tek aktif sayacla sinirlandi.
- `O5` `filteredApps` ve kategori istatistikleri her erisimde degil state uretiminde hesaplanir hale geldi.
- `D1` kullanilmayan `itemHeightDp` parametreleri temizlendi.
- `D2` ayarlar ekranindaki en dolu kategori hesabi onbelleklenmis state uzerinden okunur hale geldi.
- `D3` tekrar dogrulandi; `isLoading` degiskeni loading fallback ekraninda kullanildigi icin yanlis alarm olarak kapatildi.

## 2026-06-27 01:46

- Manuel semantik denetimdeki `Tum Kategorileri Sifirla` satiri onay dialogu ile korundu ve navigasyon yerine aksiyon gibi sunuldu.
- Dock `Varsayilanlara Sifirla` satiri chevron olmadan ve onay dialogu ile calisacak sekilde duzeltildi.
- `Izin Ver` etiketi `Bildirim Erisimini Ac` olarak guncellenip gercek davranisla hizalandi.
- `Otomatik Yedekleme` aciklamasi haftalik periyodik worker davranisini dogru anlatacak sekilde duzeltildi.
- `Geri Yukle` akisina ice aktarma oncesi onay dialogu eklendi.
- `Klasor Onizleme` ayari `Yukari Kaydirma Ipucu` olarak yeniden adlandirildi ve gercek davranisla hizalandi.
- App listesi menusundeki `Yeniden Siniflandir` aksiyonu `Kategorileri Sifirla ve Yeniden Siniflandir` olarak netlestirildi ve onay dialogu ile korundu.

## 2026-06-27 02:28

- `A1` ve `A2` `LauncherActivity` icinde home-press zamani `savedInstanceState` ile korunup receiver kaydi `onStart/onStop` yasam dongusune tasindi.
- `A3` `HomeScreen` swipe state'i `rememberSaveable` ile config-change guvenli hale getirildi.
- `A4` `AppContextMenu` favori durumu lokal prefs okumasindan cikarilip ViewModel state'iyle hizalandi.
- `A5` `FolderRenameDialog` bos isimde kaydi engelleyen hata ve disabled confirm davranisi kazandi.
- `A7` `WidgetArea` drag siralama hesabi sabit yukseklik yerine gercek olculen kart yuksekligine baglandi.
- `A13` arama gecmisi chip'lerine tiklanabilirlik semantics'i eklendi.
- `A15` alfabetik drawer basliklari `heading()` semantics'i ile erisilebilir hale getirildi.
- `P3` kullanilmayan `PermissionHelper` tamamen kaldirildi; olu kod kapatildi.
- `P1` ve `P5` `QUERY_ALL_PACKAGES` icin runtime/banner beklentisinin yanlis alarm oldugu dogrulandi; bu izin manifest kapsaminda degerlendirildigi icin ilgili bulgular kapatildi.
- `P4` bildirim izni kalici reddedildiginde onboarding artik uygulama ayarlarina yonlendiren fallback akisi kullaniyor.
- `P6` izin eksigi yalnizca sessiz donus olmaktan cikarildi; `PermissionsBanner` artik Usage Access eksigini de gosteriyor.
- `P9` kullanilmayan `GET_INSTALLED_PACKAGES` manifest izni kaldirildi.
- `P7` `QUERY_PACKAGES` onboarding adimi skippable hale getirildi.
- `P8` `NOTIF_ACCESS` adimi mevcut ortak skip davranisi ile yeniden dogrulandi; bulgu yanlis alarm olarak kapatildi.
- `C1`, `C2`, `C3` kategori CRUD akisi repository + ViewModel + ekran tarafinda gercek Room verisine baglandi.
- `C4` bos kategori adi artik UI seviyesinde engelleniyor.
- `C5` duplicate kategori adi kontrolu eklendi.
- `C6` sistem kategorilerinin silinmesini DAO seviyesinde koruyan kosul eklendi.
- `C7` launcher kategori secici sabit liste yerine veritabanindan gelen kategorileri kullanir hale getirildi.
- `C10` kategori editorunde duzenleme ve silme aksiyonlari gercek islev kazandi.

## 2026-06-27 03:20

- `P2` onboarding akisina `Usage Access` adimi eklenip `UsageStatsHelper.openPermissionSettings()` ile sistem ekranina yonlendirme baglandi.
- `P10` `PermissionsBanner` snooze suresi sabit yerine `BANNER_SNOOZE_DAYS` uzerinden okunur hale getirildi.
- `A8` `NiagaraAppRow` semantics metnine bildirim sayisi eklendi.
- `A9` `DockIcon` etkileşimli alani icin acik semantics tanimlandi.
- `A10` `SuggestionAppItem` fallback icon durumu TalkBack icin anlamli hale getirildi.
- `A11` `FavoritesRow` ve `RecentAppsRow` item'lari button rolu ve uygulama adi ile etiketlendi.
- `A12` klasor swipe ipucu caret sembolu yerine anlamli metne donusturuldu.
- `A14` `SwipeHint` icin icon aciklamasi ve polite live region semantics eklendi.
- `A16` `HomeScreenPageIndicator` tab rolu ile odak acisindan netlestirildi.
- `A17` `MiniAppIcon` fallback kutusu uygulama adini sesli aktarir hale geldi.
- `A18` `FolderSheet` bildirim satirina acik `onClick` etiketi eklendi.
- `A19` widget drag handle bulgusu yeniden dogrulandi; mevcut aciklama yeterli oldugu icin rapordan dusuruldu.
- `S2` `FolderTile` drag baslangicinda `swipeDy` sifirlanarak birikimli gesture hatasi kapatildi.
- `S4` `FolderTile` klasor kutusu erisilebilir semantics ve ozet aciklamasi kazandi.
- `S5` swipe ipucu screen reader dostu hale getirildi.
- `S7` `AllAppsDrawer` threshold'unun zaten `LocalDensity` ile dp->px donusturuldugu yeniden dogrulandi; bulgu kapatildi.
- `C8` iki kategori secicide secim sonrasi kapanis davranisi hizalandi.
- `C9` kategori secicilerinde secili ikon aciklamasi ve satir semantics'i eklendi.
- Denetim otomasyonu saatlik `Full` ve +15 dakika `Resolve` gorev akisi ile yeniden kurgulandi.
- Manuel checklist'e sistem ayari dili, durumun sesli okunmasi ve gesture cakismasi sorulari eklendi.
