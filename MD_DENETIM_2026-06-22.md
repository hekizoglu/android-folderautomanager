# 🔍 MD Denetim Raporu — 2026-06-22 (Otomatik, Günlük Rutin)

> Tarih: 2026-06-22 | Kontrol eden: Claude (zamanlanmış rutin)
> Okunan dosyalar: CLAUDE.md · LEARNINGS.md · ROADMAP.md · HISTORY.md · harcananvakit.md
> Son git commit: `24d3e6e` — MD denetim 2026-06-22 rutin 3

## 🔄 Rutin Çalışma Geçmişi

| Çalışma | Saat (UTC) | Durum | Yeni Bulgu |
|---------|-----------|-------|-----------|
| 1. rutin | 2026-06-22 00:11 | 12 sorun tespit edildi, commit ile iletildi | 1 yeni (N1 footer tarihi) |
| 2. rutin | 2026-06-22 ~06:00 | 12 sorun hâlâ açık — Hüseyin onayı bekleniyor | Yok |
| 3. rutin | 2026-06-22 ~12:00 | O1/O2/D5 gerçekten push edildi · K1/K2/O3/O4/N1 YANLIŞ “PUSHED” işaretlendi | ⚠️ YANLIŞ POZİTİF TESPİT EDİLDİ |
| **4. rutin** | **2026-06-22 ~18:00** | **Dosyalar tek tek okunarak doğrulandı — gerçek durum aşağıda** | **15 açık sorun (5 yanlış kapatılmış)** |

---

## 🔴 KRİTİK BULGU — 4. Rutin

**3. rutin yanlış pozitif verdi:** K1, K2, O3, O4, N1 “PUSHED” işaretlendi fakat dosyalar okunarak kontrol edildiğinde HİÇBİR DEĞİŞİKLİK UYGULANMAMIŞ. Gerçek git log’da bu fixlere karşılık gelen commit YOK (`24d3e6e`, `2f704e2`, `613ddc6`, `64c4ffb` — hiçbirinde CLAUDE.md veya LEARNINGS.md paket sayısı/onboarding sırası/JSON prosedürü değişmedi).

---

## ✅ Gerçekten Kapatılan Sorunlar

| # | Sorun | Dosya | Commit | Durum |
|---|-------|-------|--------|---------|
| O1 | ROADMAP.md stale içerik (3 tamamlanan görev silindi) | ROADMAP.md | `64c4ffb` | ✅ DOĞRULANDI |
| O2 | harcananvakit.md D119–D123 logları eksikti | harcananvakit.md | `613ddc6` | ✅ DOĞRULANDI |
| D5 | FİKİRLER.md Dark mode + Multi-language etiketleri | FİKİRLER.md | `2f704e2` | ✅ DOĞRULANDI |

---

## ❌ Yanlış Kapatılan + Hâlâ Açık Sorunlar

### CLAUDE.md — 6 sorun

| # | Sorun | Satır | Mevcut (Yanlış) | Olmalı |
|---|-------|-------|-----------------|--------|
| A1 | Proje yapısı paket sayısı stale | §7 satır ~320 | `AppClassifier (3717 paket)` | `AppClassifier (3702 entry, assets/app_categories.json)` |
| A2 | Mimari not paket sayısı + mimari | §7 satır ~329 | `3717 benzersiz paket, exactMatchMap` | `3702 entry, assets/app_categories.json` |
| A3 | Özellik kontrol listesi | §8 satır ~338 | `AppClassifier 3717 paket` | `AppClassifier 3702 paket (JSON)` |
| A4 | Onboarding sırası §7 | §7 satır ~331 | `SET_LAUNCHER → CLASSIFY_MODE → DONE` | `CLASSIFY_MODE → SET_LAUNCHER → DONE` |
| A5 | AppClassifier Duplicate prosedürü §5 | §5 satır ~208 | `check_duplicates.py AppClassifier.kt` | D115 sonrası AppClassifier.kt JSON’a taşındı — script artık işlevsiz |
| A6 | Footer tarihi | Satır 3 + 393 | `2026-06-20` | `2026-06-22` (D120 §3 kuralını değiştirdi) |

### LEARNINGS.md — 4 sorun

| # | Sorun | Satır | Mevcut (Yanlış) | Olmalı |
|---|-------|-------|-----------------|--------|
| B1 | L1 gövde paket sayısı | ~54 | `exactMatchMap (3717 paket, 2026-06-16 itibarıyla)` | `(3702 entry, assets/app_categories.json; D115 itibarıyla)` |
| B2 | AppClassifier Mimarisi paket sayısı | ~93 | `exactMatchMap: **3717** benzersiz paket` | `3702 entry — app_categories.json (AppClassifierAssets.kt)` |
| B3 | AppClassifier güncelleme prosedürü | ~98-101 | `exactMatchMap’te doğru konuma ekle` | JSON dosyasını düzenle: `assets/app_categories.json` |
| B4 | Onboarding adım listesi sırası (YANLIŞ) | ~105 + ~107 | `SET_LAUNCHER → CLASSIFY_MODE → DONE` | `CLASSIFY_MODE → SET_LAUNCHER → DONE` (D120 değişikliği) |

### HISTORY.md — git push 403 nedeniyle açık (5 sorun)

| # | Sorun | Satır | Durum |
|---|-------|-------|-------|
| D3 | “Onboarding Adım Listesi (14 adım)” — 16 adım yazmalı | ~147 | ⚠️ Düzeltme gerekiyor |
| D6 | Sprint Özeti tablosu D112-D123 satırı eksik | Sprint Özeti tablosu | ⚠️ Düzeltme gerekiyor |
| D1 | D110/D111 ters kronoloji | — | Düşük öncelik |
| D2 | İki ayrı “Tamamlananlar Arşivi” bölümü merge | — | Düşük öncelik |
| D4 | AGENTS.md içeriği hiç kontrol edilmedi | — | Düşük öncelik |

---

## 📊 Gerçek Durum Özeti

| Öncelik | Adet | Durum |
|---------|------|-------|
| 🔴 CLAUDE.md fixleri | 6 | A1–A6 — hepsi açık |
| 🟡 LEARNINGS.md fixleri | 4 | B1–B4 — hepsi açık |
| 🟢 HISTORY.md (push engeli) | 5 | D1–D4 + D6 — açık |
| ✅ Gerçekten kapatılan | 3 | O1 · O2 · D5 |
| **Toplam açık** | **15** | 10 CLAUDE.md+LEARNINGS (fix edilebilir) + 5 HISTORY.md (push engeli) |

---

## 📋 Önerilen Fix Sırası (Hüseyin onayı sonrası)

```
1. CLAUDE.md: A1-A3 paket sayısı (3717→3702, exactMatchMap→JSON) — 3 yer
2. CLAUDE.md: A4 onboarding sırası §7 (SET_LAUNCHER↔CLASSIFY_MODE)
3. CLAUDE.md: A5 §5 AppClassifier Duplicate prosedürü → JSON tabanlı güncelle
4. CLAUDE.md: A6 footer tarihi 2026-06-20 → 2026-06-22
5. LEARNINGS.md: B1-B2 paket sayısı + mimari güncelleme
6. LEARNINGS.md: B3 güncelleme prosedürü → JSON tabanlı
7. LEARNINGS.md: B4 onboarding sırası (satır 105 + 107)
8. HISTORY.md: D3 + D6 — interaktif oturumda (push engeli yok)
```

---

## 📝 Dikkat Notu

Zamanlanmış rutin olarak interaktif oturuma kıyasla kısıtlı çalışıyorum:
- git push: `git-receive-pack` 403 → HISTORY.md (71KB+) push edilemiyor
- CLAUDE.md + LEARNINGS.md için MCP `push_files` çalıştı: O1/O2/D5 commitlendi
- K1/K2/O3/O4/N1 için “önceki oturum” commit referansı YANLIŞ — herhangi bir önceki oturumda bu fixler uygulanmadı

> **Telegram engelli (bu ortamda)** — rapor commit mesajına yazıldı.
> 1. rutin: 00:11 UTC · 2. rutin: ~06:00 UTC · 3. rutin: ~12:00 UTC · **4. rutin: ~18:00 UTC**
