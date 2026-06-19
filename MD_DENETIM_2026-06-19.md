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

## 7. harcananvakit.md — Encoding bozukluğu (Döngü 86 satırı)

> **4. Kontrolde düzeltme:** Önceki raporlarda bu bozukluk HISTORY.md'ye atfedilmişti. Dosya yeniden okundu — HISTORY.md'de encoding sorunu yok. Gerçek bozukluk `harcananvakit.md` satır 73'te:

```
| Dongue 86 — AutoMirrored+Divider 55->18 uyari |
```
Doğrusu: `Döngü 86 — AutoMirrored+Divider 55->18 uyarı`

**Öneri:** `python scripts/fix_encoding.py harcananvakit.md` veya satırı manuel düzelt.

---

## 8. harcananvakit.md — "Tekrar Eden Sorunlar" tablosu güncel değil

Tablo hâlâ "Gradle build dir kilitlenme | Sıklık: Sık" diyor. Oysa:
- Döngü 72: Defender exclusion sonrası 3m42s → **3s** (74x hızlanma) ✅
- Döngü 78-79: `merged_res` kilidi hâlâ çıkıyor (farklı bir sorun)

**Öneri:** Tabloyu güncelle — genel kilit sorunu "Çözüldü (Defender excl.)", merged_res kilidi ayrı satır olarak ekle.

---

## 9. LEARNINGS.md — Hata Kataloğu'nda E14 eksik

HISTORY.md Döngü 88'de kritik bir bug belgelendi ama LEARNINGS.md Hata Kataloğu'na eklenmedi:

> `searchQuery` String parametresi Compose State olmadığı için `derivedStateOf` izleyemiyordu — kullanıcı yazınca liste güncellenmiyordu.  
> **Fix:** `remember(searchQuery)` ile key-based invalidation.

Bu pattern (`derivedStateOf` + plain String parametresi reaktif olmaz) LEARNINGS.md E14 olarak eklenmelidir.

**Öneri:** LEARNINGS.md Hata Kataloğu'na E14 satırı ekle.

---

## 10. LEARNINGS.md — Room DB Versiyon Geçmişi v8 eksik (YENİ)

`LEARNINGS.md` Room DB Versiyon Geçmişi bölümü v7'de bitiyor:
```
- v7: 18 yeni kategori — şema değişimi yok, MIGRATION_6_7 boş migration ile eklendi
```
Oysa `CLAUDE.md §7`'de şu bilgi var: *"Room DB: v8 (v7→v8 boş migration, 2026-06-16)"*

Migration şablonu LEARNINGS.md'de örnek olarak gösterilmiş ancak sürüm geçmişine v8 kaydı eklenmemiş.

**Öneri:** LEARNINGS.md → Room DB Versiyon Geçmişi'ne ekle: `- v8: boş migration (2026-06-16, şema değişimi yok)`

---

## Denetim Özeti (4. Kontrol — Güncel)

| # | Dosya | Sorun | Öncelik | Durum |
|---|-------|-------|---------|-------|
| 1 | ROADMAP.md | Paket sayısı "3116" stale (2 yer) | Orta | Açık |
| 2 | LEARNINGS.md | E13 duplicate satır | Düşük | Açık |
| 3 | LEARNINGS.md | Footer "HISTORprojY.md" encoding | Düşük | Açık |
| 4 | ROADMAP.md | Sprint Metrikleri satır kesilmiş | Düşük | Açık |
| 5 | CLAUDE.md | Versiyon v4↔v5 uyuşmazlık | Düşük | Açık |
| 6 | harcananvakit.md | D88-91 logları eksik | Orta | Açık |
| 7 | harcananvakit.md | D86 satırı encoding bozuk ("Dongue 86", "uyari") | Orta | Açık — önceki raporda HISTORY.md olarak yanlış atfedilmişti |
| 8 | harcananvakit.md | Kilit tablosu güncel değil | Düşük | Açık |
| 9 | LEARNINGS.md | E14 eksik (derivedStateOf+String) | Orta | Açık |
| 10 | LEARNINGS.md | Room DB versiyon geçmişi v7'de bitiyor, v8 yok | Düşük | **YENİ** |

**4. denetimde 10 sorun tespit edildi. 9 sorun hâlâ açık + 1 yeni. Düzeltme onayı bekleniyor.**

---

*Denetim tarihi: 2026-06-19 (4. çalıştırma) | Denetleyen: Claude otomatik rutin*
