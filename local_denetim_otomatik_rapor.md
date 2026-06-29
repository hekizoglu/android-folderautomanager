# Local Denetim Raporu

> Dongu: tiered frequency (T1:her Â· T2:3dongu Â· T3:10dongu)
> Son denetim: 2026-06-29 17:16
> Dongu: **#160** | Tier: **3**
> Ana tur odak: **Izin akislari, onboarding, fallback** (Permission_Izin)
> Ekstra denetim: **Samsung/Xiaomi/Huawei varyasyonlari, edge cases** (OEM_Compatibility)

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK |  | Acik yuksek bulgu |
| ORTA |  | Acik orta bulgu |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM | 2 | |

### Tier 3 Notlari
- Compose: app\build\compose_compiler bulunamadi
- Compose BOM: 2024.09.03
- Compose Compiler: 1.5.15
- compileSdk: 35
- targetSdk: 35
- APK: build edilmemis, boyut kontrol edilemedi
- Skill: 2 kontrol edildi
- TODO/FIXME: temiz

---

## YUKSEK

- CE7 | `app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt:255` | AppPrefs remember{} keysiz okunuyor - Settings donus guncellenmez. DisposableEffect + listener kullan. (E6 tekrari)

## ORTA

- CS13 | `app\src\main\java\com\armutlu\apporganizer\data\local\AppDao.kt:69` | AppDao SELECT * ORDER BY LIMIT yok - 500+ uygulama icin yavas. Pagination veya specific column sec.

---

*Denetim tarihi: 2026-06-29 17:16 | Dongu: #160 | Tier: 3 | Ana: Izin akislari, onboarding, fallback | Ekstra: Samsung/Xiaomi/Huawei varyasyonlari, edge cases*