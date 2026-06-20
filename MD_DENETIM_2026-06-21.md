# MD Denetim Raporu — 2026-06-21

> Otomatik denetim: CLAUDE.md · LEARNINGS.md · ROADMAP.md · HISTORY.md · FİKİRLER.md · harcananvakit.md
> Son kod commit: `714e731` (D104 sonrası)
> **Önceki raporlar temizlendi** — bu ilk temiz başlangıç raporu.

---

## ✅ Önceki Raporlarda Çözülenler (D93–D104)

| # | Sorun | Çözüm | Döngü |
|---|-------|-------|-------|
| K1 | Room DB "v7" stale → v8 | LEARNINGS.md + CLAUDE.md güncellendi | D-öncesi |
| K2 | P11/P12 CLAUDE.md §5'te yoktu | LEARNINGS.md'den promote edildi | D-öncesi |
| K3 | DVM register limit E13 eksik | LEARNINGS.md E13 eklendi | D-öncesi |
| O1 | ROADMAP Telegram token "Bekliyor" stale | ROADMAP yeniden yazıldı | D103 |
| O2 | Paket sayısı "3116" stale | ROADMAP + LEARNINGS güncellendi | D-öncesi |
| O3 | Sprint Metrikleri eksik satırlar | HISTORY arşivine taşındı | D103 |
| O4/O5 | CLAUDE.md/ROADMAP footer tarih stale | Güncellendi | D-öncesi |
| D3 | Firebase Metrik Hedefleri yanlış dosyada | ROADMAP'ta belgelendi | D92 |
| #13 | FCM push belgelenmemişti | HISTORY + LEARNINGS + CLAUDE.md güncellendi | D92-D95 |
| #14 | Merge conflict AppClassifier CLAUDE.md §5'e taşınmadı | CLAUDE.md §5'e eklendi | D95 |
| E13 dup | LEARNINGS.md E13 duplicate satır | Temizlendi | D-öncesi |
| E14 | derivedStateOf + String reaktif değil | LEARNINGS.md E14 eklendi | D88 |
| Room v8 | LEARNINGS'te v8 kaydı yoktu | `- v8: boş migration (2026-06-16)` eklendi | D-öncesi |
| Footer enc | `HISTORprojY.md` encoding | Düzeltildi | D-öncesi |
| CLAUDE v4/v5 | Dipnot uyuşmazlığı | Güncellendi | D-öncesi |

---

## 🔴 Kritik (Hemen)

_Şu an kritik açık sorun yok._

---

## 🟡 Orta Öncelik

### O1 — harcananvakit.md: D93–D104 log satırları eksik
- **Sorun:** Son kayıt D92. D93-D104 arası 12 döngü loglanmamış.
- **Öneri:** Retrospektif olarak ekle (tarih: 2026-06-20 / 2026-06-21).

### O2 — Onboarding 14 vs 14+2 tutarsızlığı henüz kod seviyesinde doğrulanmadı
- **Sorun:** CLAUDE.md "14+2 adım (CLASSIFY_MODE → DEFAULT_LAUNCHER → DONE)" diyor. Kod doğrulaması yapılmadı.
- **FİKİRLER.md:** 17 puan — ROADMAP ⭐ listesinde, D105'e alındı.

### O3 — COZULEMEYEN_SORUNLAR.md içeriği kontrol edilmedi
- **Sorun:** Dosya var ama içeriği bu denetimde incelenmedi.
- **Öneri:** Sonraki döngüde içeriği gözden geçir, çözülenleri işaretle.

---

## 🟢 Düşük Öncelik

### D1 — harcananvakit.md: D86 satırı başlangıç/bitiş saati yok
- **Sorun:** Satır 74'te `—` yerine gerçek saat olmalı (artık önemli değil, retrospektif zaman bilinmiyor).
- **Öneri:** Olduğu gibi bırak, retrospektif saat doldurulamaz.

### D2 — HISTORY.md "Mimari Notlar" bölümü
- **Sorun:** Tema Sistemi, HorizontalPager gibi kalıcı mimari kararlar HISTORY.md'de duruyor; LEARNINGS.md'ye ait.
- **Öneri:** İleri dönemde taşı — acil değil.

### D3 — AGENTS.md içeriği kontrol edilmedi
- **Sorun:** `AGENTS.md` dosyası var, aktif agent listesiyle senkron mu bilinmiyor.
- **Öneri:** Bir sonraki döngüde hızlıca kontrol et.

---

## 📊 Özet

| Öncelik | Adet | Durum |
|---------|------|-------|
| 🔴 Kritik | 0 | — |
| 🟡 Orta | 3 | İzleniyor |
| 🟢 Düşük | 3 | Zaman bulununca |
| **✅ Önceki raporlardan çözülen** | **15** | Kapatıldı |

**Genel durum:** Proje sağlıklı. Kod tarafında D105+ ⭐ yüksek puanlı görevler aktif — Onboarding fix, Multi-language, Klasör sıra değiştirme, Akıllı Öneriler.

---

*Denetim: 2026-06-21 | Önceki 3 rapor silindi, bu dosya temiz başlangıç.*
