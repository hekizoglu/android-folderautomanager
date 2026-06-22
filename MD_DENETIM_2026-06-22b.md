# MD Denetim Raporu — 2026-06-22 (Otomatik, 2. Rutin)

> Tarih: 2026-06-22 | Kontrol eden: Claude (zamanlanmış rutin)
> Okunan dosyalar: CLAUDE.md, LEARNINGS.md, ROADMAP.md, HISTORY.md, harcananvakit.md
> Kapsam: D112–D125 ve mevcut açık MD_DENETIM dosyaları

---

## ⚠️ 6 Sorun Tespit Edildi

---

### 🔴 SORUN 1 — MD_DENETIM_2026-06-21.md ve MD_DENETIM_2026-06-21b.md KAPANMADI [KRİTİK]

HISTORY.md son girdisi "MD Denetim 2026-06-22 — KAPANDI (commit f5e7412)" diyor.
Ancak dosya sisteminde 2 eski rapor dosyası hâlâ mevcut:

- `MD_DENETIM_2026-06-21.md` — 2 kritik + 4 orta + 6 düşük öncelik içeriyor
- `MD_DENETIM_2026-06-21b.md` — 6 sorun içeriyor

Bu dosyalardaki sorunların büyük kısmı (K1/K2/O1/O2/O3/D5/D6) commit'lerle çözüldü:
- `64c4ffb` — O1: ROADMAP stale temizlendi
- `613ddc6` — O2: harcananvakit.md D119-D123 eklendi
- `2f704e2` — D5: FİKİRLER.md tamamlandı etiketleri
- `f5e7412` — Genel düzeltmeler

**Sorun:** Raporlar silinmedi / HISTORY.md'ye "KAPANDI" notu yazılmadı.
**Öneri:** Her iki dosyayı sil. HISTORY.md'ye `## MD Denetim 2026-06-21 — KAPANDI` notu ekle.

---

### 🟡 SORUN 2 — harcananvakit.md: D93-D117 ve D124-D125 Logları Eksik [ORTA]

Mevcut durum: Son log D92 (2026-06-18 23:28), ardından D118-D123 (2026-06-21).

| Eksik Döngüler | Döngü Sayısı | Tarih |
|---------------|-------------|-------|
| D93–D117 | 25 döngü | 2026-06-20/21 |
| D124 | 1 döngü | 2026-06-22 |
| D125 | 1 döngü | 2026-06-22 |

Commit 613ddc6 D119-D123 retroaktif logları ekledi ama D93-D117 hâlâ eksik.
D124: H1 mail bug fix (~20 dk KOD+BUILD), D125: H3 klasör arama (~25 dk KOD+BUILD).

**Öneri:** D93-D117 için HISTORY.md'den tahmini süreler çıkarılarak retroaktif ekle. D124-D125 anında logla.

---

### 🟡 SORUN 3 — HISTORY.md Tamamlananlar Arşivi: "3717 benzersiz paket" Stale [ORTA]

HISTORY.md satır ~1262 (Tamamlananlar Arşivi, Akıllı Kategorizasyon bölümü):
```
- Aşama 1: Offline veritabanı — 3717 benzersiz paket
```

D115'te AppClassifier.kt exactMatchMap → assets/app_categories.json dönüşümü yapıldı.
Export edilen gerçek entry sayısı: **3702**.

CLAUDE.md §5, §7 ve LEARNINGS.md AppClassifier Mimarisi zaten "3702" diyor (doğru).
Sadece HISTORY.md Tamamlananlar Arşivi'nde "3717" kalmış.

**Öneri:** HISTORY.md satır ~1262: "3717" → "3702" güncelle.

---

### 🟡 SORUN 4 — HISTORY.md Sprint Özeti: D116-D123 Tarih Hatası [ORTA]

Sprint Özeti tablosunda:
```
| 2026-06-22 | D116-D123 | Hüseyin H1-H10 talepleri puanlama, ... D124 H1 bug fix |
```

Sorunlar:
1. D116-D123 HISTORY.md kayıtlarında "2026-06-21" tarihli — Sprint Özeti "2026-06-22" diyor, yanlış
2. D124 hem başlığa (D116-D123) hem de "D124 H1 bug fix" olarak dahil edilmiş — tutarsız

**Öneri:** Sprint Özeti son satırını düzelt:
```
| 2026-06-21 | D116-D123 | Hüseyin H1-H10 talepleri puanlama, görsel kalite, iOS+AMOLED tema |
| 2026-06-22 | D124-D125 | H1 mail bug fix, H3 klasör arama FolderSearchBar |
```

---

### 🟢 SORUN 5 — LEARNINGS.md: AppClassifierAssets.kt Sınıfı Belgelenmemiş [DÜŞÜK]

D115 büyük mimari değişiklik: AppClassifier.kt 4369→99 satır, yeni `AppClassifierAssets.kt` singleton oluşturuldu.
LEARNINGS.md AppClassifier Mimarisi bölümü JSON asset geçişini belirtiyor ama `AppClassifierAssets.kt`'nin kendisi hiç belgelenmemiş.

**Öneri:** LEARNINGS.md AppClassifier Mimarisi bölümüne 1 satır ekle:
```
- AppClassifierAssets.kt: singleton, thread-safe double-check lazy init, JSONObject ile 122 KB parse
```

---

### 🟢 SORUN 6 — HISTORY.md: İki Ayrı "Tamamlananlar Arşivi" Bölümü [DÜŞÜK]

HISTORY.md'de iki ayrı bölüm:
- Satır ~1196: `## ✅ Tamamlananlar Arşivi (ROADMAP ⭐ bölümünden taşındı)` — 4 görev
- Satır ~1239: `## ✅ Tamamlananlar Arşivi (ROADMAP'tan taşındı — 2026-06-21)` — kapsamlı liste

**Öneri:** Küçük bölümü (satır ~1196-1204) büyük bölüme merge et. Düşük öncelik — işlevsel etkisi yok.

---

## 📊 Özet

| Öncelik | Adet | Açıklama |
|---------|------|----------|
| 🔴 Kritik | 1 | MD_DENETIM 2026-06-21 dosyaları kapanmadı |
| 🟡 Orta | 3 | harcananvakit eksik loglar, 2 HISTORY tarih/paket hatası |
| 🟢 Düşük | 2 | LEARNINGS AppClassifierAssets + 2 arşiv bölümü |

**En acil:** SORUN 1 — 2 eski denetim dosyası repo'da açık kalıyor.

---

> ONAY GEREKİYOR — Bu rapor bilgi amaçlıdır. Değişiklik yapılmamıştır.
> Telegram engelli (remote ortam) → GitHub commit ile iletildi.
> Rapor: MD_DENETIM_2026-06-22b.md
