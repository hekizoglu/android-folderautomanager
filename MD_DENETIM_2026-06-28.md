# MD Denetim Raporu — 2026-06-28

> Otomatik denetim rutini (3. gecis). .env bu ortamda yok, Telegram engelli -> GitHub commit ile iletildi.
> **ONAY GEREKTIREN MADDELER** — degisiklik yapilmadi, sadece rapor.

---

## Paket Sayisi Tutarliligi

| Dosya | Deger | Durum |
|-------|-------|-------|
| CLAUDE.md §5 AppClassifier | 3702 | OK |
| CLAUDE.md §7 Ozellik Ozeti | 3702 | OK |
| LEARNINGS.md §97 | 3702 | OK |
| LEARNINGS.md [L1] | 3702 | OK |

**Sonuc:** Paket sayisi tutarli (3702). Sorun yok.

---

## Onceki Denetim (MD_DENETIM_2026-06-23.md) Durumu

| # | Sorun | Durum |
|---|-------|-------|
| S1 | HISTORY.md D140-D143 eksikti | COZULDU — D140/D141/D143 eklenmis |
| S6 | Sprint Ozeti tablosu eksik | STALE — format degisti, uygulanamaz |
| S7 | Widget hizli menu stale | COZULDU — D141'de fixlendi, FiKiRLER [TAMAMLANDI] |
| S8 | merged_res + KAPT acik | HALA ACIK — asagida N3 olarak devam ediyor |

---

## Yeni Bulgular

---

### N1 — KRITIK: HISTORY.md D151 dongu numarasi cift

**Dosya:** HISTORY.md satir 144 ve 150  
**Sorun:** "Dongu D151" iki kez farkli icerikle yazilmis.
- 1. D151 (satir 144): 5-skill kurulum, cron, audit.ps1 CE kurallari
- 2. D151 (satir 150): audit.ps1 KiloCode encoding temizligi, FiKiRLER guncelleme

**Oneri:** 2. satir D152 olarak duzeltilmeli. Sonraki dongu numaralari (D153+) zaten dogru devam ediyor — sadece bu satirin basligi D152 yapilmali.

---

### N2 — ORTA: ROADMAP.md stale — 15+ puanli FiKiRLER maddeleri eksik

**Dosya:** ROADMAP.md (son guncelleme D123, 2026-06-22 — su an D163)  
**Sorun:** CLAUDE.md kurali: "15+ puan alanlari ROADMAP yildiz bolumune de yaz."  
FiKiRLER.md'de yildiz bolumsuz olmayan 2 madde var:

| Madde | Puan | FiKiRLER Durumu | ROADMAP'te? |
|-------|------|-----------------|-------------|
| Onboarding Ayar Sihirbazi | 15 | Bekliyor | EKSIK |
| Tablet Destegi | 16 | Bekliyor | Sadece "Uzun Vade" listesinde, yildiz etiketiyle degil |

**Oneri:** ROADMAP.md'ye `### Yuksek Puanli Gorevler (15+)` bolumu ekle, bu 2 maddeyi buraya tasi.

---

### N3 — ORTA: harcananvakit.md — D144-D163 log boslugu

**Dosya:** harcananvakit.md  
**Sorun:** Son log girisi D143 (2026-06-23). D144'ten D163'e kadar 20 dongu (yaklasik 5 gun) hic log eklenmemis. Tekrar Eden Sorunlar tablosunda `merged_res kilidi` ve `KAPT incremental cache` hala "Acik" durumda.  
**Oneri:**
1. D144-D163 ozet girisi ekle (toplam zaman/kategori olarak bile olsa)
2. `merged_res kilidi` ve `KAPT` sorunlarinin cozum durumu guncellenmeli — cozulmediyse FiKiRLER.md'ye madde ekle

---

### N4 — DUSUK: LEARNINGS.md Promote Bekleyenler temizlenmemis

**Dosya:** LEARNINGS.md satir 160-162  
**Sorun:** "[2026-06-13] Merge conflict AppClassifier — PROMOTE EDILDI" satiri Promote Bekleyenler bolumunde duruyor.  
Bu madde CLAUDE.md §5'e zaten eklenmis (satir 162: "CLAUDE.md §5'e eklendi (2026-06-20)").  
**Oneri:** Bu 3 satiri Promote Bekleyenler bolumunden sil.

---

### N5 — DUSUK: LEARNINGS.md KiloCode tuzagi CLAUDE.md §5'e eklenmemis

**Dosya:** LEARNINGS.md satir 175-179  
**Sorun:** "[2026-06-28] KiloCode audit.ps1 Encoding Tuzagi — Tekrar: 1 | Oncelik: YUKSEK" satiri var.  
CLAUDE.md kurali: "3+ tekrar veya ONCELIK:YUKSEK -> CLAUDE.md §5'e tasinir."  
Bu tuzak henuz CLAUDE.md §5'te yok (mevcut Encoding bolumu bunu kapsamiyor).  
**Oneri:** LEARNINGS'ten bu bilgiyi CLAUDE.md §5 Encoding bolumune ek madde olarak tasi.

---

## Ozet

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| Onceki denetimden cozulen | 3 | S1, S7, (S6 stale) |
| Hala acik (onceki) | 1 | S8 -> N3 olarak devam |
| Kritik (yeni) | 1 | N1 — D151 cift dongu numarasi |
| Orta (yeni) | 2 | N2 — ROADMAP stale; N3 — harcananvakit log boslugu |
| Dusuk (yeni) | 2 | N4 — LEARNINGS temizlik; N5 — CLAUDE.md promote eksik |

**Toplam acik: 5 sorun — onay bekleniyor.**

---

*Denetim: 2026-06-28 | Onceki: 2026-06-23 | Telegram engelli -> commit ile iletildi*
