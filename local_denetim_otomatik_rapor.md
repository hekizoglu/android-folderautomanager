# Local Denetim Raporu

> Dongu: tiered frequency (T1:her Â· T2:3dongu Â· T3:10dongu)
> Son denetim: 2026-06-29 08:33
> Dongu: **#150** | Tier: **3**
> Ana tur odak: **Gesture, swipe, drawer akislari** (Gesture_Swipe_Drawer)
> Ekstra denetim: **Timber log quality, user-facing error messages, fallback** (Error_Handling_Logging)

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK |  | Acik yuksek bulgu |
| ORTA | 0 | Acik orta bulgu |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM |  | |

### Tier 3 Notlari
- Compose: metrics dosyasi yok (build sonrasi olusur)
- Compose BOM: 2024.09.03
- Compose Compiler: 1.5.15
- compileSdk: 35
- targetSdk: 35
- APK: 24.57 MB
- Skill: 2 kontrol edildi
- TODO/FIXME: temiz

---

## YUKSEK

- CE7 | `app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt:255` | AppPrefs remember{} keysiz okunuyor - Settings donus guncellenmez. DisposableEffect + listener kullan. (E6 tekrari)

---

*Denetim tarihi: 2026-06-29 08:33 | Dongu: #150 | Tier: 3 | Ana: Gesture, swipe, drawer akislari | Ekstra: Timber log quality, user-facing error messages, fallback*