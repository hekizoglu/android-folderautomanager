# 🔍 MD Denetim Raporu — 2026-06-21 (Otomatik)

> Tarih: 2026-06-21 | Kontrol eden: Claude (zamanlanmış rutin)
> Okunan dosyalar: CLAUDE.md, LEARNINGS.md, ROADMAP.md, HISTORY.md, harcananvakit.md

---

## ⚠️ 6 Sorun Tespit Edildi

---

### 🟡 SORUN 1 — Paket Sayısı Tutarsızlığı [ORTA]

| Dosya | Değer |
|-------|-------|
| CLAUDE.md §7 | **3717** benzersiz paket |
| LEARNINGS.md AppClassifier Mimarisi | **3717** paket |
| HISTORY.md Döngü 115 | `exactMatchMap (**3702** entry)` JSON asset'e taşındı |

D115'te AppClassifier.kt exactMatchMap → `assets/app_categories.json` dönüşümünde **3702 entry** kaydedilmiş.
Belgelerin tümü 3717 söylüyor, 15 paket farkı var.

**Öneri:** D115 build çıktısını veya JSON dosyasını doğrula → CLAUDE.md + LEARNINGS.md'deki sayıyı güncelle.

---

### 🔴 SORUN 2 — ROADMAP.md Tamamen Stale [YÜKSEK]

| Stale Bilgi | Gerçek Durum |
|-------------|-------------|
| "Şu An Ne Yapılıyor (D108)" | Mevcut döngü D117+ |
| Multi-language "🔄 D108 devam ediyor" | D108-D113 + D116'da **tamamlandı** |
| "Hilt DI kurulumu" bekliyor | D117'de **tamamlandı** (utils/CategoryLLMFallback.kt silindi) |
| "AppClassifier → JSON asset" Tartışma ⚠️ | D115'te **tamamlandı** (AppClassifierAssets.kt + assets/app_categories.json) |

**Öneri:** 3 tamamlanan görevi ROADMAP'tan sil → HISTORY.md Tamamlananlar Arşivi'ne taşı.
"Şu An Ne Yapılıyor" bölümünü D117 sonrası durumla güncelle.

---

### 🟡 SORUN 3 — LEARNINGS.md AppClassifier Mimarisi Güncel Değil [ORTA]

LEARNINGS.md şu an şöyle yazıyor:
```
exactMatchMap: 3717 benzersiz paket
Bilinmeyen → CAT_OTHER → CategoryLLMFallback.kt (DeepSeek batch 15)
```

Güncel durum (D115 + D117 sonrası):
- `exactMatchMap` → `assets/app_categories.json` (3702 entry, 122 KB, `AppClassifierAssets.kt` ile lazy parse)
- `AppClassifier.kt` 4369 satır → 99 satır
- `utils/CategoryLLMFallback.kt` (14 kategori) **silindi** — `domain/usecase/classify/CategoryLLMFallback` (32 kategori, Hilt inject) aktif

**Öneri:** LEARNINGS.md §AppClassifier Mimarisi bölümünü JSON asset geçişi ve yeni CategoryLLMFallback yoluyla güncelle.

---

### 🟢 SORUN 4 — harcananvakit.md D96-D117 Logları Eksik [DÜŞÜK]

Son kayıtlı giriş: **Döngü 92 (2026-06-18 23:28)**

2026-06-21 tarihinde yapılan döngüler (D96-D117 = ~20+ döngü) hiçbiri kayıtlı değil.
Bu, 20+ döngülük iş süresinin tamamen kör nokta olduğu anlamına gelir.

**Öneri:** D96-D117 için tahmini süreler eklensin (kaynak: HISTORY.md döngü notları).

---

### 🟢 SORUN 5 — HISTORY.md Döngü Kronoloji Karışık [DÜŞÜK]

D112-D115 blokları ters sırayla yazılmış:
```
Mevcut sıra: D115 → D114 → D113 → D112 → D116 → D117
Doğru sıra:  D112 → D113 → D114 → D115 → D116 → D117
```

Aynı şekilde D92 "Retroaktif Belgeleme" D95'in **sonrasına** eklenmiş ama kronolojik olarak D91'in ardından gelmelidir.

Salt okunabilirlik sorunu, işlevsel etkisi yok.
**Öneri:** Düşük öncelik — fırsat bulununca yeniden sırala.

---

### 🟡 SORUN 6 — CLAUDE.md §7 AppClassifier Açıklaması Eski [ORTA]

CLAUDE.md §7 Önemli Mimari Notlar şöyle yazıyor:
```
AppClassifier: 3717 benzersiz paket, exactMatchMap + KeywordDatabase (32 kategori)
```

D115 sonrası gerçek mimari:
```
AppClassifier: ~3702 paket (assets/app_categories.json — AppClassifierAssets.kt ile lazy parse)
+ KeywordDatabase (32 kategori)
```

**Öneri:** §7 Önemli Mimari Notlar bölümünde `exactMatchMap` → JSON asset geçişini yansıtacak şekilde güncelle.

---

## 📋 Eylem Planı (Onay Sonrası)

| Öncelik | Sorun | Dosya | Eylem |
|---------|-------|-------|-------|
| 1 | SORUN 2 | ROADMAP.md | 3 tamamlanan görevi sil + Şu An güncelle |
| 2 | SORUN 3 | LEARNINGS.md | AppClassifier Mimarisi bölümünü yeniden yaz |
| 3 | SORUN 1 + 6 | CLAUDE.md + LEARNINGS.md | Paket sayısını doğrula ve güncelle (3717 → 3702?) |
| 4 | SORUN 4 | harcananvakit.md | D96-D117 zaman logları ekle |
| 5 | SORUN 5 | HISTORY.md | Döngü sırası düzelt (düşük öncelik) |

---

> ONAY GEREKİYOR — bu rapor bilgi amaçlıdır, değişiklik yapılmamıştır.
> Rapor oluşturma: 2026-06-21 otomatik MD denetim rutini
