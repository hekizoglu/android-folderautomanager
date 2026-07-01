# Local Denetim Raporu

> Dongu: tiered frequency (T1:her · T2:3dongu · T3:10dongu)
> Son denetim: 2026-07-01 07:59
> Dongu: **#189** | Tier: **2**
> Ana tur odak: **State yonetimi, SharedPrefs, kalicilik** (Data_State_Persistence)
> Ekstra denetim: **Activity/Fragment leak, Flow collect, DisposableEffect** (Memory_Lifecycle)

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK |  | Acik yuksek bulgu |
| ORTA |  | Acik orta bulgu |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM | 2 | |

---

## YUKSEK

- CE7 | `app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt:258` | AppPrefs remember{} keysiz okunuyor - Settings donus guncellenmez. DisposableEffect + listener kullan. (E6 tekrari)

## ORTA

- CS13 | `app\src\main\java\com\armutlu\apporganizer\data\local\AppDao.kt:69` | AppDao SELECT * ORDER BY LIMIT yok - 500+ uygulama icin yavas. Pagination veya specific column sec.

---

*Denetim tarihi: 2026-07-01 07:59 | Dongu: #189 | Tier: 2 | Ana: State yonetimi, SharedPrefs, kalicilik | Ekstra: Activity/Fragment leak, Flow collect, DisposableEffect*