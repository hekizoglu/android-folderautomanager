# Local Denetim Raporu

> Dongu: tiered frequency (T1:her Â· T2:3dongu Â· T3:10dongu)
> Son denetim: 2026-06-30 00:14
> Dongu: **#170** | Tier: **3**
> Ana tur odak: **Recomposition, cache, IO, performans** (Performance_Memory)
> Ekstra denetim: **Hassas veri, log, izin, data export/import guvenligi** (Privacy_Security)

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
- Compose: metrics dosyasi yok (build sonrasi olusur)
- Compose BOM: 2024.09.03
- Compose Compiler: 1.5.15
- compileSdk: 35
- targetSdk: 35
- APK: 25.03 MB
- Skill: 2 kontrol edildi
- TODO/FIXME: temiz

---

## YUKSEK

- CE7 | `app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\SettingsScreen.kt:258` | AppPrefs remember{} keysiz okunuyor - Settings donus guncellenmez. DisposableEffect + listener kullan. (E6 tekrari)

## ORTA

- CS13 | `app\src\main\java\com\armutlu\apporganizer\data\local\AppDao.kt:69` | AppDao SELECT * ORDER BY LIMIT yok - 500+ uygulama icin yavas. Pagination veya specific column sec.

---

*Denetim tarihi: 2026-06-30 00:14 | Dongu: #170 | Tier: 3 | Ana: Recomposition, cache, IO, performans | Ekstra: Hassas veri, log, izin, data export/import guvenligi*