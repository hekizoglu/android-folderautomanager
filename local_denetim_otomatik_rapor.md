# Local Denetim Raporu

> Dongu: tiered frequency (T1:her Â· T2:3dongu Â· T3:10dongu)
> Son denetim: 2026-06-29 10:34
> Dongu: **#153** | Tier: **2**
> Ana tur odak: **Dock, widget, yedekleme akislari** (Dock_Widget_Backup)
> Ekstra denetim: **StateFlow kullanimi, hot-path, race condition** (ViewModel_StateFlow)

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

- CE7 | `app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt:255` | AppPrefs remember{} keysiz okunuyor - Settings donus guncellenmez. DisposableEffect + listener kullan. (E6 tekrari)

---

*Denetim tarihi: 2026-06-29 10:34 | Dongu: #153 | Tier: 2 | Ana: Dock, widget, yedekleme akislari | Ekstra: StateFlow kullanimi, hot-path, race condition*