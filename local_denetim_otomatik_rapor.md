# Local Denetim Raporu

> DÃ¶ngÃ¼: `15 dakikalÄ±k 8+1 odak rotasyonu`
> Son denetim: 2026-06-27 17:44
> Ana tur odak: **Gesture, swipe, drawer akislari** (Gesture_Swipe_Drawer)
> Ekstra denetim: **Timber log quality, user-facing error messages, fallback** (Error_Handling_Logging)
> Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasina tasinir.

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK |  | Acik kritik bulgu |
| YUKSEK |  | Acik yuksek bulgu |
| ORTA |  | Acik orta bulgu |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM | 3 | |

---

## KRITIK

- K9 | "app\src\main\java\com\armutlu\apporganizer\presentation\viewmodel\AppListViewModel.kt:120" | Runtime NoSuchMethodError riski - getMethod/Flow API senkronu ihlali.

## YUKSEK

- Y6 | "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\OnboardingScreen.kt:108" | Permission rette fallback ve ayar yonlendirme eksik.

## ORTA

- O7 | "app\src\main\java\com\armutlu\apporganizer\utils\DockPrefs.kt:43" | removeFromDock Unit donduruyor, geri bildirim yok.

---

Not: Bu script agirlikli olarak statik ve otomatik taranabilen kurallari kontrol eder.
Buton adi ile yaptigi isin tutarliligi gibi anlamsal UI denetimleri manuel veya yari otomatik kod okumasi gerektirir.
Manuel semantik tur icin local_denetim_manuel_checklist.md kullan.

---

*Denetim tarihi: 2026-06-27 17:44 | Ana: Gesture, swipe, drawer akislari | Ekstra: Timber log quality, user-facing error messages, fallback*

---

KiloCode | Profesyonel local denetim asistani - Android uygulama kalitesi ve guvenilirligi.
Kod hatasiz, kullanici dostu, anlasilir ve suratli olmaya devam ediyor.