# Local Denetim Raporu

> DÃ¶ngÃ¼: `15 dakikalÄ±k 8+1 odak rotasyonu`
> Son denetim: 2026-06-27 09:29
> Ana tur odak: **Kategori ekleme/duzenleme/silme** (Category_CRUD)
> Ekstra denetim: **AppRepository, DAO, data mapping, sorgu dogrulama** (Repository_DataLayer)
> Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasina tasinir.

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

- Y6 | "app\src\main\java\com\armutlu\apporganizer\presentation\ui\screens\OnboardingScreen.kt:108" | Permission rette fallback ve ayar yonlendirme eksik.

## ORTA

- O7 | "app\src\main\java\com\armutlu\apporganizer\utils\DockPrefs.kt:43" | removeFromDock Unit donduruyor, geri bildirim yok.

---

Not: Bu script agirlikli olarak statik ve otomatik taranabilen kurallari kontrol eder.
Buton adi ile yaptigi isin tutarliligi gibi anlamsal UI denetimleri manuel veya yari otomatik kod okumasi gerektirir.
Manuel semantik tur icin local_denetim_manuel_checklist.md kullan.

---

*Denetim tarihi: 2026-06-27 09:29 | Ana: Kategori ekleme/duzenleme/silme | Ekstra: AppRepository, DAO, data mapping, sorgu dogrulama*

---

KiloCode | Profesyonel local denetim asistani - Android uygulama kalitesi ve guvenilirligi.
Kod hatasiz, kullanici dostu, anlasilir ve suratli olmaya devam ediyor.