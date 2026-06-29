# Local Denetim Raporu

> Dongu: tiered frequency (T1:her Â· T2:3dongu Â· T3:10dongu)
> Son denetim: 2026-06-29 08:04
> Dongu: **#147** | Tier: **2**
> Ana tur odak: **Izin akislari, onboarding, fallback** (Permission_Izin)
> Ekstra denetim: **Samsung/Xiaomi/Huawei varyasyonlari, edge cases** (OEM_Compatibility)

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK | 2 | Acik yuksek bulgu |
| ORTA | 0 | Acik orta bulgu |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM | 2 | |

---

## YUKSEK

- CE7 | `app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt:255` | AppPrefs remember{} keysiz okunuyor - Settings donus guncellenmez. DisposableEffect + listener kullan. (E6 tekrari)
- CE9 | `app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt:89` | AppPrefs remember{} keysiz okunuyor - tum KEY_*'ler DisposableEffect listener'da olmali. Eksik: KEY_DOUBLE_TAP_SEARCH/KEY_ASSISTANT_CARDS gibi yeni eklenenler. (D191'de yakalandi)

---

*Denetim tarihi: 2026-06-29 08:04 | Dongu: #147 | Tier: 2 | Ana: Izin akislari, onboarding, fallback | Ekstra: Samsung/Xiaomi/Huawei varyasyonlari, edge cases*