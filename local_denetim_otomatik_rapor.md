# Local Denetim Raporu

> Dongu: tiered frequency (T1:her Â· T2:3dongu Â· T3:10dongu)
> Son denetim: 2026-06-29 08:12
> Dongu: **#148** | Tier: **1**
> Ana tur odak: **State yonetimi, SharedPrefs, kalicilik** (Data_State_Persistence)
> Ekstra denetim: **Activity/Fragment leak, Flow collect, DisposableEffect** (Memory_Lifecycle)

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK |  | Acik yuksek bulgu |
| ORTA | 0 | Acik orta bulgu |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM |  | |

---

## YUKSEK

- CE9 | `app\src\main\java\com\armutlu\apporganizer\presentation\ui\launcher\HomeScreen.kt:89` | AppPrefs remember{} keysiz okunuyor - tum KEY_*'ler DisposableEffect listener'da olmali. Eksik: KEY_DOUBLE_TAP_SEARCH/KEY_ASSISTANT_CARDS gibi yeni eklenenler. (D191'de yakalandi)

---

*Denetim tarihi: 2026-06-29 08:12 | Dongu: #148 | Tier: 1 | Ana: State yonetimi, SharedPrefs, kalicilik | Ekstra: Activity/Fragment leak, Flow collect, DisposableEffect*