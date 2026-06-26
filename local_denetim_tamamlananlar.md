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
