# MD Denetim Raporu — 2026-06-21

> Otomatik denetim: CLAUDE.md · LEARNINGS.md · ROADMAP.md · HISTORY.md · FİKİRLER.md · harcananvakit.md
> Son kod commit inceleme kapsamı: D104 → D111
> **ONAY GEREKİYOR** — değişiklik yapılmadı, sadece rapor güncellendi.

---

## ✅ D104 Öncesi Çözülenler

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

## ✅ D105–D111 Döneminde Çözülenler

| # | Sorun | Çözüm | Döngü |
|---|-------|-------|-------|
| O2 (eski) | Onboarding 14 vs 14+2 tutarsızlığı | Kod incelemesiyle 16 adım doğrulandı, CLAUDE.md güncellendi | D105 |
| O3 (eski) | COZULEMEYEN_SORUNLAR.md incelenmemişti | D111'de temizlendi, çözülenler tablo haline getirildi | D111 |

---

## 🔴 Kritik (Hemen)

_Şu an kritik açık sorun yok._

---

## 🟡 Orta Öncelik

### O1 — ROADMAP.md: Multi-language support tamamlandı ama hâlâ açık görünüyor ⭐ YENİ
- **Sorun:** ROADMAP.md `Şu An Ne Yapılıyor (D108)` bölümü "SettingsScreen + SettingsAppearanceSection sırada" diyor. ROADMAP.md `⭐ Yüksek Puanlı` tablosunda `🔄 D108 devam ediyor` yazıyor.
- **Gerçek durum:** HISTORY.md Döngü 108-109 — SettingsScreen (11 key) + SettingsAppearanceSection (10 key) dahil toplam 34 string tamamlandı. Görev BITTI.
- **Eksik:** ROADMAP.md'den silinmedi. HISTORY.md Tamamlananlar Arşivi'ne eklenmedi.
- **Öneri:** ROADMAP.md `Şu An Ne Yapılıyor` bölümünü temizle. ⭐ tablosundan sil. HISTORY.md Tamamlananlar'a `| D108-109 | 17 | Multi-language support (TR/EN) | strings.xml 50+ key, 34 Türkçe literal |` ekle.

### O2 — harcananvakit.md: D93–D111 arası 19 döngü loglanmamış ⭐ GENİŞLEDİ
- **Sorun:** Son log satırı D92 (2026-06-18). D93-D111 arası ~19 döngü (2026-06-20/21) kaydedilmemiş. Önceki raporda D93-D104 olarak belirtilmişti, sorun devam ediyor ve kapsam genişledi.
- **Öneri:** Retrospektif olarak HISTORY.md döngü loglarından özet çıkarılarak eklenebilir.

---

## 🟢 Düşük Öncelik

### D1 — HISTORY.md: D110/D111 ters kronoloji ⭐ YENİ
- **Sorun:** HISTORY.md'de Döngü 111 (satır ~1213) Döngü 110'dan (satır ~1217) önce geliyor. D110 retroaktif eklendi.
- **Öneri:** Anlam kaybı yok, olduğu gibi bırakılabilir. Veya D110'u D111'den önce sıraya taşı.

### D2 — HISTORY.md: İki ayrı "Tamamlananlar Arşivi" bölümü ⭐ YENİ
- **Sorun:** HISTORY.md'de satır ~1196 ve ~1239'da aynı başlıkla iki ayrı Tamamlananlar Arşivi bölümü var. Küçük bölüm (D104/105/106/107 puan tablosu) büyük kapsamlı bölümle örtüşüyor.
- **Öneri:** Küçük bölümü büyük bölüme merge et veya başlığını değiştir.

### D3 — HISTORY.md: "Mimari Notlar" bölümünde "14 adım" stale
- **Sorun:** HISTORY.md satır 147'deki "Mimari Notlar" bölümü `Onboarding Adım Listesi (14 adım)` diyor; CLASSIFY_MODE yok. D105'te 16 adım doğrulandı, bu not güncellenmedi.
- **Bağlam:** Bu tarihi bir snapshot — döneminde doğruydu. Ancak LEARNINGS.md zaten 16 adımı doğru gösteriyor.
- **Öneri:** HISTORY.md satır 147'ye `(D105 itibarıyla 16 adım — bkz. LEARNINGS.md)` notu ekle.

### D4 — AGENTS.md içeriği kontrol edilmedi (devam)
- **Sorun:** `AGENTS.md` dosyası var, aktif agent listesiyle senkron mu bilinmiyor.
- **Öneri:** Sonraki döngüde kontrol et.

---

## 📊 Paket Sayısı Tutarlılık Kontrolü

| Dosya | Paket Sayısı | Durum |
|-------|-------------|-------|
| CLAUDE.md §7 | 3717 | ✅ |
| CLAUDE.md §8 | 3717 | ✅ |
| LEARNINGS.md L1 | 3717 | ✅ |
| LEARNINGS.md Mimari Kararlar | 3717 | ✅ |
| HISTORY.md D67 | 3717 | ✅ |
| HISTORY.md Tamamlananlar Arşivi | 3717 | ✅ |

**Sonuç: Paket sayısı tüm dosyalarda tutarlı — sorun yok.**

---

## 📊 Özet

| Öncelik | Adet | Durum |
|---------|------|-------|
| 🔴 Kritik | 0 | — |
| 🟡 Orta | 2 | Onay bekliyor |
| 🟢 Düşük | 4 | Zaman bulununca |
| **✅ Çözülen (toplam)** | **17** | Kapatıldı |

**Genel durum:** Proje sağlıklı. Kod tarafı D108-111 itibarıyla stabil. En acil aksiyon: ROADMAP.md multi-language kaydı (O1) — 2 satır değişiklik, onay gelince yapılabilir.

---

*Denetim güncellemesi: 2026-06-21 otomatik rutin | Kapsam: D105-D111 | Telegram engelli, GitHub commit ile iletildi.*
