# MD Denetim Raporu — 2026-06-19

> Otomatik 6 saatlik MD denetim rutini. Telegram engellendiği için GitHub'a yazıldı.
> **ONAY GEREKİYOR** — Bu dosya dışında hiçbir değişiklik yapılmadı.

---

⚠️ **8 sorun tespit edildi**

---

## 1. ROADMAP.md — Paket sayısı stale (2 yer)

| Yer | Mevcut | Doğru |
|-----|--------|-------|
| §Tamamlananlar/Akıllı Kategorizasyon | "3116+ benzersiz paket" | **3717** |
| §Akıllı Kategorizasyon Aşama 3 | "3116 paketlik exactMatchMap" | **3717** |

Doğrulama: `python scripts/check_duplicates.py AppClassifier.kt` → 3717 benzersiz, 0 duplicate ✅

**Öneri:** Her iki satırdaki "3116" → "3717" ile güncelle.

---

## 2. LEARNINGS.md — Hata Kataloğu E13 duplicate

Satır 145-146'da E13 ("VerifyError / DVM register limit") tam aynı satır iki kez yazılmış.

**Öneri:** İkinci E13 satırını sil.

---

## 3. LEARNINGS.md — Footer encoding hatası

Son satırda:
```
...CLAUDE.md'den taşınan döngü logları HISTORprojY.md'de.
```
`HISTORprojY.md` → doğrusu: `HISTORY.md`

**Öneri:** `python scripts/fix_encoding.py LEARNINGS.md` veya manuel düzelt.

---

## 4. ROADMAP.md — Sprint Metrikleri son satır kesilmiş

Son tablo satırı yarım kalmış:
```
| 2026-06-16 | MD Denetim + ROADMAP/LEARNINGS/CLAUDE düzeltme (K1/K
```
Cümle tamamlanmamış.

**Öneri:** Satırı tamamla veya sil.

---

## 5. CLAUDE.md — Versiyon uyuşmazlığı

- CLAUDE.md dipnotu: `CLAUDE.md v4`
- HISTORY.md Döngü 69: `CLAUDE.md v5 - rollback, paralel agent...`

**Öneri:** CLAUDE.md dipnotunu `v5` olarak güncelle.

---

## 6. harcananvakit.md — Döngü 88-91 zaman logları eksik

Son kayıt: Döngü 87 (2026-06-16 10:35-10:55). Döngü 88-91 logları yok:
- D88: AllApps arama kritik bug fix (remember derivedStateOf sorunu)
- D89: LauncherViewModelTest 4 yeni test
- D90: BUILD #17 — 24.79MB
- D91: Dark mode hardcode renk düzeltmesi (2026-06-18)

**Öneri:** 4 eksik döngüyü retrospektif olarak ekle.

---

## 7. HISTORY.md — Encoding bozukluğu (Döngü 82+)

Döngü 82 ve sonraki başlıklarda Türkçe karakter bozukluğu:
- `Doengue` → `Döngü`
- `Yapilanlar` → `Yapılanlar`
- `temizligi` → `temizliği`
- `Ayarlar talep formu` → `Ayarlar Talep Formu`

**Öneri:** `PYTHONIOENCODING=utf-8 python scripts/fix_encoding.py HISTORY.md`

---

## 8. harcananvakit.md — "Tekrar Eden Sorunlar" tablosu güncel değil

Tablo hâlâ "Gradle build dir kilitlenme | Sıklık: Sık" diyor. Oysa:
- Döngü 72: Defender exclusion sonrası 3m42s → **3s** (74x hızlanma) ✅
- Döngü 78-79: `merged_res` kilidi hâlâ çıkıyor (farklı bir sorun)

**Öneri:** Tabloyu güncelle — genel kilit sorunu "Çözüldü (Defender excl.)", merged_res kilidi ayrı satır olarak ekle.

---

## Özet

| # | Dosya | Sorun | Öncelik |
|---|-------|-------|---------|
| 1 | ROADMAP.md | Paket sayısı "3116" stale | Orta |
| 2 | LEARNINGS.md | E13 duplicate satır | Düşük |
| 3 | LEARNINGS.md | Footer "HISTORprojY.md" encoding | Düşük |
| 4 | ROADMAP.md | Sprint Metrikleri satır kesilmiş | Düşük |
| 5 | CLAUDE.md | Versiyon v4↔v5 uyuşmazlık | Düşük |
| 6 | harcananvakit.md | D88-91 logları eksik | Orta |
| 7 | HISTORY.md | D82+ Türkçe karakter bozuk | Orta |
| 8 | harcananvakit.md | Kilit tablosu güncel değil | Düşük |

*Denetim tarihi: 2026-06-19 | Denetleyen: Claude otomatik rutin*
