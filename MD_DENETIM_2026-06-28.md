# MD Denetim Raporu — 2026-06-28

> 4. gecis — 2026-06-28. .env bu ortamda yok, Telegram engelli -> GitHub commit ile iletildi.
> **ONAY GEREKTIREN MADDELER** — degisiklik yapilmadi, sadece rapor.

---

## Paket Sayisi Tutarliligi

| Dosya | Deger | Durum |
|-------|-------|-------|
| CLAUDE.md paragraf 5 AppClassifier | 3702 | OK |
| CLAUDE.md paragraf 7 Ozellik Ozeti | 3702 | OK |
| LEARNINGS.md | 3702 | OK |
| LEARNINGS.md [L1] | 3702 | OK |

**Sonuc:** Paket sayisi tutarli (3702). Sorun yok.

---

## Onceki Denetim (MD_DENETIM_2026-06-23.md) Durumu

| # | Sorun | Durum |
|---|-------|-------|
| S1 | HISTORY.md D140-D143 eksikti | COZULDU — D140/D141/D143 eklenmis |
| S6 | Sprint Ozeti tablosu eksik | STALE — format degisti, uygulanamaz |
| S7 | Widget hizli menu stale | COZULDU — D141'de fixlendi, FiKiRLER [TAMAMLANDI] |
| S8 | merged_res + KAPT acik | HALA ACIK — N3 olarak devam ediyor |

MD_DENETIM_2026-06-23.md silinebilir: S1+S7 cozuldu, S6 stale, S8 buraya tasinmis. (Bkz N8)

---

## Acik Sorunlar

---

### N1 — KRITIK: HISTORY.md D151 dongu numarasi cift

**Dosya:** HISTORY.md satir 144 ve 150
**Sorun:** "Dongu D151" iki kez farkli icerikle yazilmis:
- 1. D151 (satir 144): 5-skill kurulum, cron, audit.ps1 CE kurallari
- 2. D151 (satir 150): audit.ps1 KiloCode encoding temizligi, FiKiRLER guncelleme

**Oneri:** 2. satirin basligini D151b veya sonraki D152 yap. D153+ zaten dogru devam ediyor.

---

### N2 — ORTA: ROADMAP.md stale (D123'ten beri guncellenmedi)

**Dosya:** ROADMAP.md (son guncelleme: 2026-06-22 / D123; su an: D177)
**Sorunlar:**

1. **Tamamlanan ama hala acik gorunen gorevler:**
   - "Klasor tasma (overflow) sorunu" — D174'te tablet adaptive layout ile cozuldu
   - Android 15/16 Edge-to-Edge — [TAMAMLANDI D177] FiKiRLER'de, ROADMAP'te hic yok

2. **15+ puan alip ROADMAP'e girmemis bekleyen gorevler (CLAUDE.md kurali ihlali):**

| Madde | Puan | FiKiRLER Durumu | ROADMAP'te? |
|-------|------|-----------------|-------------|
| Tablet Destegi | 16p | Bekliyor (ama D174'te yapildi — bkz N6) | Sadece "Uzun Vade" |
| Gesture/Multitasking Uyumlulugu | 16p | Bekliyor | EKSIK |
| Google Drive Cross-Device Sync | 17p | Bekliyor | EKSIK |
| Safe Mode/Crash Recovery | 15p | [TAMAMLANDI D176] | EKSIK |

**Oneri:** ROADMAP.md'ye "Yuksek Puanli Gorevler (15+)" bolumu ekle; Gesture ve Google Drive
buraya girmeli. Tamamlananlar HISTORY.md'ye tasinmali (Safe Mode, Edge-to-Edge).

---

### N3 — ORTA: harcananvakit.md — D144-D177 log boslugu (34 dongu)

**Dosya:** harcananvakit.md
**Sorun:** Son log D143 (2026-06-23 11:15). D144-D177 arasi 34 dongu (yaklasik 5 gun) log yok.
Tekrar Eden Sorunlar tablosunda `merged_res kilidi` ve `KAPT incremental cache` hala "Acik" —
D72'den beri cozum durumu degismemis, plan yok.

**Oneri:**
1. D144-D177 icin toplu ozet log girisi ekle (kategori bazli toplam yeterli)
2. merged_res ve KAPT icin cozum plani: FiKiRLER.md'ye madde ekle ya da "Kabul Edildi" olarak kapat

---

### N4 — DUSUK: LEARNINGS.md Promote Bekleyenler — zaten promote edilmis madde duruyor

**Dosya:** LEARNINGS.md satir 160-164
**Sorun:** "[2026-06-13] Merge conflict AppClassifier — PROMOTE EDILDI" satiri bolumde duruyor.
CLAUDE.md paragraf 5'e zaten eklenmis (satir 162: "CLAUDE.md paragraf 5'e eklendi (2026-06-20)").

**Oneri:** Bu 3 satiri Promote Bekleyenler bolumunden sil.

---

### N5 — DUSUK: LEARNINGS.md KiloCode tuzagi CLAUDE.md paragraf 5'e eklenmemis

**Dosya:** LEARNINGS.md satir 175-181
**Sorun:** "[2026-06-28] KiloCode audit.ps1 Encoding Tuzagi — Oncelik: YUKSEK" var.
CLAUDE.md kurali: "ONCELIK:YUKSEK -> CLAUDE.md paragraf 5'e tasinir." Mevcut Encoding bolumu bunu kapsamiyor.

**Oneri:** LEARNINGS.md'deki KiloCode tuzagini CLAUDE.md paragraf 5 Encoding bolumune alt madde olarak ekle:
"KiloCode audit.ps1'e otomatik eklenen kurallar curly quote / em dash icerebilir —
PS5.1 syntax hatasi. Description alanlarinda daima ASCII-safe string kullan."

---

### N6 — ORTA: FiKiRLER.md Tablet Destegi "Bekliyor" ama HISTORY.md D174'te tamamlandi

**Dosya:** FiKiRLER.md satir 37, HISTORY.md D174
**Sorun:** FiKiRLER.md Tablet Destegi — "Bekliyor" yazıyor.
HISTORY.md D174: "FolderPager adaptive columns: 600dp+=5 sutun, 840dp+=6 sutun; maxFolderSizeDp tablet'e gore hesaplandi."
Bu tamamlandi — FiKiRLER.md guncellenmemis.

**Oneri:** FiKiRLER.md Tablet Destegi satirini "[TAMAMLANDI D174]" olarak guncelle.
ROADMAP.md Uzun Vade listesinden "Tablet layout" satirini da guncelle.

---

### N7 — ORTA: LEARNINGS.md + CLAUDE.md paragraf 7 Onboarding adim sayisi stale

**Dosya:** LEARNINGS.md satir 109-112, CLAUDE.md paragraf 7
**Sorun:**
- LEARNINGS.md: "D120 guncel — 16 adim: ... THEME_SELECT -> CLASSIFY_MODE -> SET_LAUNCHER -> DONE"
- CLAUDE.md paragraf 7: "Onboarding: 16 adim (WELCOME -> ... -> THEME_SELECT -> CLASSIFY_MODE -> SET_LAUNCHER -> DONE)"
- HISTORY.md D173: "QUICK_SETTINGS adimi aktif edildi; adim sirasi: THEME_SELECT -> QUICK_SETTINGS -> CLASSIFY_MODE -> SET_LAUNCHER -> DONE"

D173'te QUICK_SETTINGS eklendi — adim sayisi 17 oldu. Iki dosya da 16 diyor.

**Oneri:**
- LEARNINGS.md: "D173 guncel — 17 adim" olarak duzeltle, adim sirasina QUICK_SETTINGS ekle.
- CLAUDE.md paragraf 7: "16 adim" -> "17 adim"; adim listesine QUICK_SETTINGS ekle.

---

### N8 — DUSUK: MD_DENETIM_2026-06-23.md hala acik — silinebilir

**Dosya:** MD_DENETIM_2026-06-23.md
**Sorun:** Eski rapor tum maddeleri ya cozuldu (S1, S7), ya stale (S6), ya da buraya tasindi (S8->N3).
CLAUDE.md kurali: "Rapor tamamen bosalinca -> dosyayi sil, HISTORY.md'ye 'MD Denetim KAPANDI' notu ekle."

**Oneri:** MD_DENETIM_2026-06-23.md'yi sil. HISTORY.md'ye "MD Denetim 2026-06-23 KAPANDI" notu ekle.

---

## Ozet

| Oncelik | Sayi | Sorunlar |
|---------|------|----------|
| Paket sayisi | 0 sorun | 3702 tutarli, tum dosyalar uyumlu |
| Kritik | 1 | N1 — HISTORY.md D151 cift dongu numarasi |
| Orta | 4 | N2 ROADMAP stale (D123'ten 40+ dongu); N3 harcananvakit 34 dongu boslugu; N6 Tablet tamamlandi ama Bekliyor; N7 Onboarding 16→17 adim stale |
| Dusuk | 3 | N4 LEARNINGS temizlik; N5 CLAUDE.md promote eksik; N8 eski denetim silinmeli |

**Toplam acik: 8 sorun — onay bekleniyor.**

---

*4. gecis: 2026-06-28 | Onceki gecis: 2026-06-28 (3. gecis) | Telegram engelli -> GitHub commit ile iletildi*
