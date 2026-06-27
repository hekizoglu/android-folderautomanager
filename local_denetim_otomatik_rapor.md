# Local Denetim Raporu

> DÃ¶ngÃ¼: `15 dakikalÄ±k 8+1 odak rotasyonu`
> Son denetim: 2026-06-27 09:11
> Ana tur odak: **Gesture, swipe, drawer akislari** (Gesture_Swipe_Drawer)
> Ekstra denetim: **Timber log quality, user-facing error messages, fallback** (Error_Handling_Logging)
> Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasina tasinir.

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK | 2 | Acik yuksek bulgu |
| ORTA | 2 | Acik orta bulgu |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM | 4 | |

---

## YUKSEK

- Y5 | "app\src\main\java\com\armutlu\apporganizer\presentation\ui\theme\Theme.kt:88" | darkTheme parametresi devre disi birakilmis.
- Y6 | "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\OnboardingScreen.kt:108" | Permission rette fallback ve ayar yonlendirme eksik.

## ORTA

- O7 | "app\src\main\java\com\armutlu\apporganizer\utils\DockPrefs.kt:43" | removeFromDock Unit donduruyor, geri bildirim yok.
- O8 | "app\src\main\java\com\armutlu\apporganizer\utils\PackageManagerHelper.kt:38" | shouldHide endsWith ile yanlis eslesme riski.

---

Not: Bu script agirlikli olarak statik ve otomatik taranabilen kurallari kontrol eder.
Buton adi ile yaptigi isin tutarliligi gibi anlamsal UI denetimleri manuel veya yari otomatik kod okumasi gerektirir.
Manuel semantik tur icin local_denetim_manuel_checklist.md kullan.

---

*Denetim tarihi: 2026-06-27 09:11 | Ana: Gesture, swipe, drawer akislari | Ekstra: Timber log quality, user-facing error messages, fallback*