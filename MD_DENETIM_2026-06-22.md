# 🔍 MD Denetim Raporu — 2026-06-22 (Otomatik, Günlük Rutin)

> Tarih: 2026-06-22 | Kontrol eden: Claude (zamanlanmış rutin)
> Okunan dosyalar: CLAUDE.md · LEARNINGS.md · ROADMAP.md · HISTORY.md · harcananvakit.md · FİKİRLER.md
> Son git commit: `6d974fa` — MD denetim raporu 2026-06-21 (2. rutin)
> D123 sonrasında yeni kod commit'i yok.
> **ONAY GEREKİYOR** — değişiklik yapılmadı, sadece rapor oluşturuldu.

---

## ⚠️ 12 Sorun Açık (Önceki Rapordan Değişmedi — Hüseyin Onayı Bekleniyor)

---

## 🔴 Kritik (2 sorun)

### K1 — LEARNINGS.md: Onboarding adım sırası CLAUDE.md ile çelişiyor
**Kaynak:** MD_DENETIM_2026-06-21.md (K1) | **Durum:** Hâlâ açık

- `LEARNINGS.md` satır 105 (adım listesi): `... → SET_LAUNCHER → CLASSIFY_MODE → DONE`
- `LEARNINGS.md` satır 107 (kural): "Son iki adım: **SET_LAUNCHER → CLASSIFY_MODE** → DONE değiştirilemez"
- **Doğrusu:** CLAUDE.md §3: "Son 3 adım MUTLAKA **CLASSIFY_MODE → SET_LAUNCHER** → DONE" *(D120'de kullanıcı talebiyle SET_LAUNCHER sona alındı)*
- **Risk:** Claude LEARNINGS.md'ye bakıp yanlış sırayla onboarding değiştirirse uygulama bozulur.
- **Öneri:** LEARNINGS.md satır 105 ve 107 → sırayı düzelt.

---

### K2 — LEARNINGS.md + CLAUDE.md §5: AppClassifier Güncelleme Prosedürü YANLIŞ
**Kaynak:** MD_DENETIM_2026-06-21.md (K2) | **Durum:** Hâlâ açık

- Her iki dosyada hâlâ şöyle yazıyor:
  ```
  1. exactMatchMap'te doğru konuma ekle (alfabetik sıra)
  2. python scripts/check_duplicates.py AppClassifier.kt çalıştır
  ```
- **Gerçek durum (D115 sonrası):** `AppClassifier.kt` 4369 satır → 99 satıra indi. `exactMatchMap` artık `assets/app_categories.json`'da (3702 entry). `AppClassifierAssets.kt` ile lazy parse.
- **Risk:** Bu prosedürü izleyen Claude veya geliştirici KT dosyasında varolmayan yapıya eklemeye çalışır → sessiz başarısızlık, paket sınıflandırılmaz.
- **Öneri:** Güncelleme prosedürü → `assets/app_categories.json` düzenle + `scripts/export_classifier_json.py` aracına yönlendir.

---

## 🟡 Orta Öncelik (4 sorun)

### O1 — ROADMAP.md: Stale içerik (3 tamamlanan görev açık görünüyor)
**Kaynak:** MD_DENETIM_2026-06-21.md (O1) | **Durum:** Hâlâ açık

| Stale Bilgi | Gerçek Durum |
|-------------|-------------|
| "Şu An Ne Yapılıyor (D108)" + Multi-language "🔄 D108" | HISTORY Döngü 108-109 + D113 — **tamamlandı** |
| "AppClassifier → JSON asset — Tartışma ⚠️" | D115'te **tamamlandı** |
| "Hilt DI kurulumu — Bekliyor" | D117'de **tamamlandı** |

**Öneri:** 3 görevi ROADMAP'tan sil → HISTORY.md Tamamlananlar Arşivi'ne taşı. "Şu An Ne Yapılıyor" bölümünü temizle.

---

### O2 — harcananvakit.md: D119–D123 logları eksik
**Kaynak:** MD_DENETIM_2026-06-21.md (O2) | **Durum:** Hâlâ açık

Son kayıtlı log: Döngü 118 (2026-06-21 08:30-10:05). D119-D123 (klavye fix, onboarding redesign, Privacy Policy, iOS+AMOLED tema, görsel kalite) loglanmamış = ~5 döngülük kör nokta.

**Öneri:** HISTORY.md döngü notlarından retrospektif zaman tahmini ekle.

---

### O3 — Paket sayısı tutarsızlığı: 3717 vs 3702
**Kaynak:** MD_DENETIM_2026-06-21.md (O3) | **Durum:** Hâlâ açık

| Dosya | Değer |
|-------|-------|
| CLAUDE.md §7 (2 yer) | **3717** |
| LEARNINGS.md AppClassifier Mimarisi | **3717** |
| assets/app_categories.json (gerçek kaynak) | **3702** |

D115 JSON export sırasında 15 paket kaybedildi. CLAUDE.md + LEARNINGS.md hâlâ 3717 söylüyor.

**Öneri:** CLAUDE.md + LEARNINGS.md → "3717" → "3702". Kayıp 15 paketi bulmak için `git diff fe0fce8^..fe0fce8 -- app/src/.../AppClassifier.kt` + JSON diff.

---

### O4 — LEARNINGS.md: AppClassifier Mimarisi D115 sonrası güncel değil
**Kaynak:** MD_DENETIM_2026-06-21.md (O4) | **Durum:** Hâlâ açık

LEARNINGS.md "AppClassifier Mimarisi" bölümü `AppClassifierAssets.kt` singleton'ını ve JSON asset geçişini hiç belgelemiyor. D117'de `utils/CategoryLLMFallback.kt` (14 kategori) silindi — bunu da yansıtmıyor.

**Öneri:** Bölümü D115+D117 sonrası mimariyle yeniden yaz: `AppClassifierAssets.kt` (thread-safe double-check lazy, `assets/app_categories.json` 122KB) + `domain/usecase/classify/CategoryLLMFallback` (Hilt inject, 32 kategori).

---

## 🟢 Düşük Öncelik (6 sorun)

| # | Sorun | Dosya | Öneri |
|---|-------|-------|-------|
| D1 | D110/D111 ters kronoloji | HISTORY.md | Okunabilirlik sorunu, düşük öncelik |
| D2 | İki ayrı "Tamamlananlar Arşivi" bölümü (satır ~1196 ve ~1239) | HISTORY.md | Küçük bölümü büyüğe merge et |
| D3 | "Onboarding Adım Listesi (14 adım)" — CLASSIFY_MODE eksik | HISTORY.md satır 147 | `(D105 itibarıyla 16 adım — bkz. LEARNINGS.md)` notu ekle |
| D4 | AGENTS.md içeriği kontrol edilmedi | AGENTS.md | Aktif agent listesiyle senkron mu kontrol et |
| D5 | "Dark mode → Bekliyor" ve "Multi-language → Bekliyor" (her ikisi TAMAMLANDI D114/D113) | FİKİRLER.md Düşük Öncelik tablosu | `[TAMAMLANDI D114]` ve `[TAMAMLANDI D113]` olarak güncelle |
| D6 | Sprint Özeti son satırı "D107-D111" — D112-D123 arası 12 döngü eksik | HISTORY.md | Yeni satır ekle: `2026-06-21 · D112-D123 · HomeScreen refactor, OnboardingScreen dil, JSON asset 3702, 156 test, iOS+AMOLED tema` |

---

## 🆕 Bu Denetimde Yeni Tespit

### N1 — CLAUDE.md footer: "Son güncelleme: 2026-06-20" ama D120 (2026-06-21) kuralı güncelledi [DÜŞÜK]
CLAUDE.md son satırı "Son güncelleme: 2026-06-20" yazıyor. D120'de onboarding sırası (CLASSIFY_MODE → SET_LAUNCHER) kuralı CLAUDE.md §3'e eklendi (2026-06-21). Footer en az 1 gün geride.

**Öneri:** Footer tarihi 2026-06-21 olarak güncelle, v5 notuna D120 kuralı ekle.

---

## 📊 Özet

| Öncelik | Adet | Durum |
|---------|------|-------|
| 🔴 Kritik | 2 | K1 + K2 — Hüseyin onayı bekliyor |
| 🟡 Orta | 4 | O1–O4 — Hüseyin onayı bekliyor |
| 🟢 Düşük | 7 | D1–D6 + N1 — Fırsat bulununca |
| **✅ Çözülen (toplam önceki rapordan)** | **17** | Kapatıldı |

**En acil:**
1. **K2** — AppClassifier prosedürü aktif olarak yanlış yazılı → yeni paket eklenirse hata.
2. **K1** — LEARNINGS.md onboarding sırası ters → CLASSIFY_MODE / SET_LAUNCHER karıştırılabilir.
3. **O3** — Paket sayısı 3717 yazıyor ama 3702 (15 kayıp paket açıklanmamış).

---

## 📋 Eylem Planı (Onay Gelince)

| Sıra | Sorun | Dosya | Eylem |
|------|-------|-------|-------|
| 1 | K2 | CLAUDE.md §5 + LEARNINGS.md | AppClassifier Güncelleme Prosedürü → JSON-bazlı |
| 2 | K1 | LEARNINGS.md satır 105+107 | Onboarding sıra düzelt: CLASSIFY_MODE → SET_LAUNCHER |
| 3 | O3 | CLAUDE.md §7 + LEARNINGS.md | 3717 → 3702 |
| 4 | O4 | LEARNINGS.md | AppClassifier Mimarisi bölümü yeniden yaz |
| 5 | O1 | ROADMAP.md | 3 tamamlanan görevi sil, "Şu An" güncelle |
| 6 | O2 | harcananvakit.md | D119–D123 retrospektif loglar |
| 7 | D2+D3+D6 | HISTORY.md | Arşiv merge, 14→16 adım notu, Sprint Özeti satırı |
| 8 | D5 | FİKİRLER.md | Düşük Öncelik etiketleri güncelle |
| 9 | N1 | CLAUDE.md | Footer tarihi 2026-06-21 |

---

> **Telegram engelli (bu ortamda)** — rapor commit mesajına yazıldı ve GitHub MCP ile push edildi.
> Rutin: 2026-06-22 | Telegram: Engellenmiş | GitHub commit ile iletildi.
