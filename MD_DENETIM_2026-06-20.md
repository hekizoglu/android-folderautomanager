# MD Denetim Raporu — 2026-06-20

> Otomatik 6 saatlik MD denetim rutini. Telegram bu ortamda engellendiği için GitHub commit olarak raporlandı.
> **ONAY GEREKİYOR** — Bu dosya dışında hiçbir değişiklik yapılmadı.

---

⚠️ **12 sorun tespit edildi** (10 önceki hâlâ açık + 2 yeni)

---

## Özet Tablo

| # | Dosya | Sorun | Öncelik | Durum |
|---|-------|-------|---------|-------|
| 1 | ROADMAP.md | Paket sayısı "3116" stale (2 yer) | Orta | Açık (4. denetimden beri) |
| 2 | LEARNINGS.md | E13 duplicate satır (satır 145-146) | Düşük | Açık |
| 3 | LEARNINGS.md | Footer "HISTORprojY.md" encoding hatası | Düşük | Açık |
| 4 | ROADMAP.md | Sprint Metrikleri son satır kesilmiş ("K1/K") | Düşük | Açık |
| 5 | CLAUDE.md | Dipnot "v4" ↔ HISTORY.md D69 "v5" uyuşmazlık | Düşük | Açık |
| 6 | harcananvakit.md | D88-91 zaman logları eksik | Orta | Açık |
| 7 | harcananvakit.md | D86 satırı "Dongue 86" / "uyari" encoding bozuk | Orta | Açık |
| 8 | harcananvakit.md | "Tekrar Eden Sorunlar" tablosu güncel değil | Düşük | Açık |
| 9 | LEARNINGS.md | E14 eksik (derivedStateOf + String parametresi reaktif olmaz) | Orta | Açık |
| 10 | LEARNINGS.md | Room DB versiyon geçmişi v7'de bitiyor, v8 kaydı yok | Düşük | Açık |
| 11 | LEARNINGS.md | Onboarding adım listesi güncel değil (CLASSIFY_MODE eksik, 14 adım vs CLAUDE.md "14+2") | **Orta** | **YENİ** |
| 12 | ROADMAP.md | Sprint Metrikleri D88-D91 döngü satırları eksik | Düşük | **YENİ** |

---

## Detaylı Bulgular

### #1 — ROADMAP.md: Paket sayısı stale

| Yer | Mevcut | Doğru |
|-----|--------|-------|
| §Tamamlananlar / Akıllı Kategorizasyon | "3116+ benzersiz paket" | **3717** |
| §Backlog / Akıllı Kategorizasyon Aşama 3 | "3116 paketlik exactMatchMap" | **3717** |

Doğrulama: HISTORY.md Döngü 67 → "3717 benzersiz" kaydı var; CLAUDE.md §7 "3717 benzersiz paket" yazıyor.

**Fix:** Her iki "3116" → "3717".

---

### #2 — LEARNINGS.md: E13 duplicate

Satır 145-146 bire bir aynı:
```
| E13 | VerifyError / DVM register limit | Büyük @Composable (300+ satır) → register limiti aşılıyor | Fonksiyonlara böl, composable'ı küçült |
| E13 | VerifyError / DVM register limit | Büyük @Composable (300+ satır) → register limiti aşılıyor | Fonksiyonlara böl, composable'ı küçült |
```

**Fix:** İkinci E13 satırını sil.

---

### #3 — LEARNINGS.md: Footer encoding hatası

Son satır:
```
...CLAUDE.md'den taşınan döngü logları HISTORprojY.md'de.
```

**Fix:** `HISTORprojY.md` → `HISTORY.md`

---

### #4 — ROADMAP.md: Sprint Metrikleri son satır kesilmiş

Son tablo satırı yarım:
```
| 2026-06-16 | MD Denetim + ROADMAP/LEARNINGS/CLAUDE düzeltme (K1/K
```

**Fix:** Satırı tamamla veya sil.

---

### #5 — CLAUDE.md: Versiyon dipnotu uyuşmazlığı

- CLAUDE.md son satır: `CLAUDE.md v4`
- HISTORY.md Döngü 69: `CLAUDE.md v5 - rollback, paralel agent...`

**Fix:** CLAUDE.md son satırını `v5` olarak güncelle.

---

### #6 — harcananvakit.md: D88-91 logları eksik

Son kayıt: Döngü 87 (2026-06-16 10:35-10:55). Eksikler:
- D88 (2026-06-16): AllApps arama kritik bug fix (~20dk, KOD+BUILD)
- D89 (2026-06-16): LauncherViewModelTest 4 yeni test (~15dk, TEST)
- D90 (2026-06-16): BUILD #17 — 24.79MB, 1s cache (~3dk, BUILD)
- D91 (2026-06-18): Dark mode hardcode renk düzeltmesi (~30dk, KOD)

**Fix:** 4 satırı retrospektif ekle.

---

### #7 — harcananvakit.md: D86 satırı encoding bozuk

Mevcut (satır 73):
```
| Dongue 86 — AutoMirrored+Divider 55->18 uyari |
```
Doğrusu:
```
| Döngü 86 — AutoMirrored+Divider 55->18 uyarı |
```

**Fix:** `python scripts/fix_encoding.py harcananvakit.md` veya manuel düzelt.

---

### #8 — harcananvakit.md: "Tekrar Eden Sorunlar" tablosu güncel değil

Tablo hâlâ şunu diyor:
```
| Gradle build dir kilitlenme | Sıklık: Sık | 20-40 dk |
```

Oysa:
- Döngü 72 Defender exclusion → 3m42s → **3s** (74x hızlanma, artık sorun değil)
- Döngü 78-79: `merged_res` kilidi hâlâ çıkıyor (farklı sorun, ayrı satır gerekiyor)

**Fix:** "Gradle build dir" → "Çözüldü ✅ (Defender excl. Döngü 72)"; yeni satır: "merged_res kilit" eklenmeli.

---

### #9 — LEARNINGS.md: E14 eksik

HISTORY.md Döngü 88'de kritik bug belgelendi ama LEARNINGS.md Hata Kataloğu'na eklenmedi:

> `searchQuery` String parametresi Compose State olmadığı için `derivedStateOf` izleyemiyordu — kullanıcı yazınca liste güncellenmiyordu.  
> **Fix:** `remember(searchQuery)` key-based invalidation.

**Fix:** LEARNINGS.md Hata Kataloğu'na ekle:
```
| E14 | AllApps arama reaktif değil | `derivedStateOf` plain String parametresini izleyemez | `remember(searchQuery) { derivedStateOf { ... } }` — key-based invalidation |
```

---

### #10 — LEARNINGS.md: Room DB versiyon geçmişi v7'de bitiyor

LEARNINGS.md Room DB Versiyon Geçmişi:
```
- v7: 18 yeni kategori — MIGRATION_6_7 boş migration
```
CLAUDE.md §7: "Room DB: v8 (v7→v8 boş migration, 2026-06-16)"

**Fix:** Şu satırı ekle: `- v8: boş migration (2026-06-16, şema değişimi yok, MIGRATION_7_8 eklendi)`

---

### #11 — LEARNINGS.md: Onboarding adım listesi güncel değil 🆕

**Uyuşmazlık:**
- CLAUDE.md §7: `"14+2 adım (son: CLASSIFY_MODE → DEFAULT_LAUNCHER → DONE)"`
- LEARNINGS.md §Onboarding Adım Sırası: 14 adım listesi, **CLASSIFY_MODE adımı yok**, son adım "SET_LAUNCHER"
- HISTORY.md §Mimari Notlar: aynı 14 adım, CLASSIFY_MODE yok

**Kanıt:** HISTORY.md D37 → "Onboarding CLASSIFY_MODE adımı + temizlik" (D37'de eklendi ama LEARNINGS.md güncellenmedi)

Ayrıca SET_LAUNCHER (LEARNINGS) ↔ DEFAULT_LAUNCHER (CLAUDE.md) isim uyuşmazlığı var — muhtemelen aynı adım.

**Fix:** LEARNINGS.md Onboarding bölümünü güncelle:
```
WELCOME → RESTORE_BACKUP → QUERY_PACKAGES → NOTIFICATIONS → UNUSED_GREY → AUTO_BACKUP → NOTIF_TEXT → NOTIF_ACCESS → SWIPE_HINT → NEW_BADGE → FOLDER_COUNT → NAV_HIDE → THEME_SELECT → CLASSIFY_MODE → DEFAULT_LAUNCHER → DONE (14+2 adım)
Toggle chip adımları: AUTO_BACKUP, NOTIF_TEXT, SWIPE_HINT, NEW_BADGE, FOLDER_COUNT, NAV_HIDE, CLASSIFY_MODE
```

---

### #12 — ROADMAP.md: Sprint Metrikleri D88-D91 satırları eksik 🆕

Sprint Metrikleri tablosunun son satırı kesilmiş durumda (sorun #4) ve D88-D91 aktivitesi tabloya hiç eklenmemiş:

- D88: AllApps arama bug fix (derivedStateOf+String)
- D89: LauncherViewModelTest 19 test
- D90: BUILD #17 (24.79MB)
- D91: Dark mode hardcode renk fix (2026-06-18)

**Fix:** Sprint Metrikleri tablosuna son satırı tamamla ve D88-91 satırını ekle.

---

## Denetim Durumu

- **Önceki açık sorunlar:** 10 (MD_DENETIM_2026-06-19.md, 4. kontrol)
- **Bu denetimde yeni:** 2 (#11 onboarding tutarsızlığı, #12 sprint metrikleri)
- **Düzeltilen:** 0
- **Toplam açık:** 12
- **Son kod değişikliği:** Döngü 91 (2026-06-18)
- **Denetim tarihi:** 2026-06-20

**Telegram engellendiği için bu rapor GitHub commit olarak iletildi.**

---

*Denetim: Claude otomatik rutin | ONAY GEREKİYOR — değişiklik yapılmadı*
