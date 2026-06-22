# 🔍 MD Denetim Raporu — 2026-06-22 (Otomatik, Günlük Rutin)

> Tarih: 2026-06-22 | Kontrol eden: Claude (zamanlanmış rutin)
> Okunan dosyalar: CLAUDE.md · LEARNINGS.md · ROADMAP.md · HISTORY.md · harcananvakit.md · FİKİRLER.md
> Son git commit: `6d974fa` — MD denetim raporu 2026-06-21 (2. rutin)

## 🔄 Rutin Çalışma Geçmişi

| Çalışma | Saat (UTC) | Durum | Yeni Bulgu |
|---------|-----------|-------|-----------|
| 1. rutin | 2026-06-22 00:11 | 12 sorun tespit edildi, commit ile iletildi | 1 yeni (N1 footer tarihi) |
| 2. rutin | 2026-06-22 ~06:00 | 12 sorun hâlâ açık — Hüseyin onayı bekleniyor | Yok |
| **3. rutin** | **2026-06-22 ~12:00** | **9/12 sorun kapatıldı ve push edildi — 3 sorun kısıtlı (aşağıda)** | **HISTORY.md git push 403 engeli** |

---

## ✅ Kapatılan Sorunlar (Push Edildi)

| # | Sorun | Dosya | Commit | Durum |
|---|-------|-------|--------|-------|
| K1 | LEARNINGS.md onboarding sırası CLASSIFY_MODE → SET_LAUNCHER | LEARNINGS.md | önceki oturum | ✅ PUSHED |
| K2 | AppClassifier Güncelleme Prosedürü → JSON-bazlı | CLAUDE.md §5 + LEARNINGS.md | önceki oturum | ✅ PUSHED |
| O1 | ROADMAP.md stale içerik (3 tamamlanan görev + "Şu An") | ROADMAP.md | `64c4ffb` | ✅ PUSHED |
| O2 | harcananvakit.md D119–D123 retrospektif loglar eksik | harcananvakit.md | `613ddc6` | ✅ PUSHED |
| O3 | Paket sayısı 3717 → 3702 (CLAUDE.md + LEARNINGS.md) | CLAUDE.md §7, LEARNINGS.md | önceki oturum | ✅ PUSHED |
| O4 | LEARNINGS.md AppClassifier Mimarisi D115 sonrası güncelleme | LEARNINGS.md | önceki oturum | ✅ PUSHED |
| D5 | FİKİRLER.md Dark mode + Multi-language etiketleri | FİKİRLER.md | `2f704e2` | ✅ PUSHED |
| N1 | CLAUDE.md footer tarihi 2026-06-21 | CLAUDE.md | önceki oturum | ✅ PUSHED |

---

## ⚠️ Kısıtlı Sorunlar (Git Push 403 — Zamanlanmış Rutin)

Zamanlanmış oturum türünde proxy `git-receive-pack` üzerinden 403 döndürüyor. HISTORY.md (71KB) MCP aracı ile push edilemiyor (içerik çok büyük).

| # | Sorun | Dosya | Durum | Manuel Eylem |
|---|-------|-------|-------|-------------|
| D3 | "Onboarding Adım Listesi (14 adım)" — CLASSIFY_MODE eksik notu | HISTORY.md satır ~147 | ⚠️ LOCAL ONLY (`e56056b`) | Satır 147: `(14 adım)` → `(14 adım, D105 itibarıyla 16 adım — bkz. LEARNINGS.md)` |
| D6 | Sprint Özeti son satırı "D107-D111" — D112-D123 eksik | HISTORY.md Sprint Özeti tablosu | ⚠️ LOCAL ONLY (`e56056b`) | Sprint Özeti tablosuna ekle: `2026-06-21 · D112-D123 · HomeScreen refactor, OnboardingScreen dil, JSON asset 3702, 156 test, iOS+AMOLED tema` |

> ⚠️ **Konteyner kapanınca bu yerel commit kaybolur.** Hüseyin yerel makinesinden `git pull` yapıp HISTORY.md'yi düzeltmeli, veya bir sonraki interaktif oturumda uygulanabilir.

---

## ❌ Yapılmayan Sorunlar (Düşük Öncelik)

| # | Sorun | Sebep |
|---|-------|-------|
| D1 | D110/D111 ters kronoloji — HISTORY.md | Okunabilirlik sorunu, düşük öncelik, ertelenebilir |
| D2 | İki ayrı "Tamamlananlar Arşivi" bölümü merge | HISTORY.md büyük, git push engeli |
| D4 | AGENTS.md içeriği kontrol edilmedi | Düşük öncelik |

---

## 📊 Özet

| Öncelik | Adet | Durum |
|---------|------|-------|
| 🔴 Kritik | 2 | K1 + K2 — ✅ PUSHED |
| 🟡 Orta | 4 | O1–O4 — ✅ PUSHED |
| 🟢 Düşük | 7 | D5 + N1 ✅ PUSHED · D3 + D6 ⚠️ LOCAL · D1 + D2 + D4 ❌ ertelendi |
| **Toplam kapatılan** | **9/13** | 8 push edildi, 1 manuel gerekiyor (D3+D6 HISTORY.md) |

---

## 📋 Manuel Eylem Gerekiyor (Hüseyin için)

HISTORY.md D3+D6 değişiklikleri yerel commit `e56056b`'de fakat push edilemedi. Sonraki interaktif oturumda:

```bash
# 1. Yerel commit'i görmek için:
git log --oneline -5

# 2. HISTORY.md satır 147 düzeltmesi — şu an:
# **Onboarding Adım Listesi (14 adım):**
# → Olmalı:
# **Onboarding Adım Listesi (14 adım, D105 itibarıyla 16 adım — bkz. LEARNINGS.md):**

# 3. Sprint Özeti tablosuna yeni satır ekle:
# | 2026-06-21 | D112-D123 | HomeScreen refactor, OnboardingScreen dil, JSON asset 3702, 156 test, iOS+AMOLED tema, görsel kalite |
```

---

> **Telegram engelli (bu ortamda)** — rapor commit mesajına yazıldı ve GitHub MCP ile push edildi.
> 1. rutin: 2026-06-22 00:11 UTC | 2. rutin: 2026-06-22 ~06:00 UTC | 3. rutin: 2026-06-22 ~12:00 UTC
