# Local Denetim Raporu Tamamlananlar

## 2026-06-26 11:26

- `K1` AllApps sıralama tercihi `AppPrefs` üzerinden tek prefs kaynağına taşındı.
- `Y1` `fuzzySearch()` Türkçe locale ile normalize edilerek AppList ve drawer araması hizalandı.
- `Y2` klasör arama sayacı `snapshotFlow` + `collectLatest` ile eski sayaçları iptal edecek hale getirildi.
- `Y3` `FolderTile` içindeki `swipeDy` recomposition güvenli Compose state oldu.
- `Y4` launcher varsayılan durumu tekrar hesaplama yerine hatırlanan state ile yönetildi.
- `O1` kategori sekmeleri ViewModel tarafında önceden hesaplanan `visibleCategories` listesine taşındı.
- `O2` All Apps içindeki recent/favorite ikon cache anahtarlarına `lastUpdatedTime` eklendi.
- `O3` `AppClassifier` üzerindeki global mutable flag kaldırıldı; sınıflandırma tercihi çağrı bazlı parametre oldu.
- `O4` klasör arama temizleme akışı tek aktif sayaçla sınırlandı.
- `O5` `filteredApps` ve kategori istatistikleri her erişimde değil state üretiminde hesaplanır hale geldi.
- `D1` kullanılmayan `itemHeightDp` parametreleri temizlendi.
- `D2` ayarlar ekranındaki en dolu kategori hesabı önbelleklenmiş state üzerinden okunur hale geldi.
- `D3` tekrar doğrulandı; `isLoading` değişkeni loading fallback ekranında kullanıldığı için yanlış alarm olarak kapatıldı.
