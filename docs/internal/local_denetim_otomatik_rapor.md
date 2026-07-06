# Local Denetim Raporu

> Dongu: tiered frequency (T1:her · T2:3dongu · T3:10dongu)
> Son denetim: 2026-07-06 13:33
> Dongu: **#189** | Tier: **2**
> Ana tur odak: **Settings etiket-davranis tutarliligi** (UI_Settings_Labels)
> Ekstra denetim: **Ekran gecisleri, route, intent, back press** (Navigation_Routing)

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

- CE7 | `app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt:258` | AppPrefs remember{} keysiz okunuyor - Settings donus guncellenmez. DisposableEffect + listener kullan. (E6 tekrari)

---

*Denetim tarihi: 2026-07-06 13:33 | Dongu: #189 | Tier: 2 | Ana: Settings etiket-davranis tutarliligi | Ekstra: Ekran gecisleri, route, intent, back press*