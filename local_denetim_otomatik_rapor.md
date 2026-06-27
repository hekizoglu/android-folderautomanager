# Local Denetim Otomatik Rapor

> Döngü: `15 dakikalık 8+1 odak rotasyonu`
> Son denetim: 2026-06-28 02:44
> Ana tur odak: **Dock, widget, yedekleme akışları** (Dock_Widget_Backup)
> Ekstra denetim: **StateFlow kullanımı, hot-path, race condition** (ViewModel_StateFlow)
> Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasına taşınır.

---

## Denetim Özeti

| Öncelik | Sayı | Açıklama |
|---------|------|----------|
| KRİTİK  | 0 | Açık kritik bulgu yok |
| YÜKSEK  | 0 | Açık yüksek bulgu yok |
| ORTA    | 0 | Açık orta bulgu yok |
| DÜŞÜK   | 0 | Açık düşük bulgu yok |
| TOPLAM  | 0 | Tüm bulgular kapatıldı |

---

## Kapatılan Bulgular

### [KRİTİK] K9 — Runtime NoSuchMethodError riski
**Durum:** ÇÖZÜLDÜ (D144 / 2026-06-27)
**Çözüm:** `getAllCategoriesFlow()` tüm katmanlarda (CategoryDao, AppRepository, AppListViewModel) tanımlı. Clean build ile APK senkronize edildi.

### [YÜKSEK] Y6 — Permission rette fallback eksik
**Durum:** YANLIŞ ALARM (D144 / 2026-06-28)
**Açıklama:** `OnboardingScreen.kt:108` ve `294`'te `shouldShowRequestPermissionRationale` + `ACTION_APPLICATION_DETAILS_SETTINGS` zaten mevcut.

### [ORTA] O7 — removeFromDock Unit döndürüyor
**Durum:** ÇÖZÜLDÜ (D144 / 2026-06-27)
**Açıklama:** `DockPrefs.kt:43` Boolean döndürüyor, `LauncherViewModel.kt:350` wrapper'ı toast ile geri bildirim sağlıyor.

---

*Denetim tarihi: 2026-06-28 | Encoding düzeltildi D146 | Tüm bulgular kapatıldı.*
