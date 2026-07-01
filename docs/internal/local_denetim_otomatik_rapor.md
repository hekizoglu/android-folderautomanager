# Local Denetim Raporu

> Dongu: tiered frequency (T1:her · T2:3dongu · T3:10dongu)
> Son denetim: 2026-07-01 19:01
> Dongu: **#25** | Tier: **1**
> Ana tur odak: **TalkBack, contentDescription, semantics** (Accessibility_A11y)
> Ekstra denetim: **Test edilmeyis senaryolari, dead code, unused imports** (Test_Coverage_Gap)

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK | 0 | Acik yuksek bulgu |
| ORTA |  | Acik orta bulgu |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM |  | |

---

## ORTA

- CS13 | `app\src\main\java\com\armutlu\apporganizer\data\local\AppDao.kt:69` | AppDao SELECT * ORDER BY LIMIT yok - 500+ uygulama icin yavas. Pagination veya specific column sec.

---

*Denetim tarihi: 2026-07-01 19:01 | Dongu: #25 | Tier: 1 | Ana: TalkBack, contentDescription, semantics | Ekstra: Test edilmeyis senaryolari, dead code, unused imports*