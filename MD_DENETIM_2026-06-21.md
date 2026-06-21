# MD Denetim Raporu — 2026-06-21

> Otomatik denetim: CLAUDE.md · LEARNINGS.md · ROADMAP.md · HISTORY.md · FİKİRLER.md · harcananvakit.md
> Son kod commit inceleme kapsamı: D104 → D123 | Taze çalışma: 2026-06-21 (ikinci rutin)
> **ONAY GEREKİYOR** — değişiklik yapılmadı, sadece rapor güncellendi.
> Son git commit: `74053b2` — önceki denetim. D123'ten bu yana yeni kod değişikliği yok.

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

### K2 — CLAUDE.md §5 + LEARNINGS.md: AppClassifier Güncelleme Prosedürü AKTİF YANLIŞ ⭐ 2. rutin YENİ

- **Sorun:** D115'te AppClassifier.kt `exactMatchMap` (4369 satır) → `assets/app_categories.json` (3702 entry, `AppClassifierAssets.kt`) aktarıldı. Ancak CLAUDE.md §5 ve LEARNINGS.md'deki "Güncelleme Prosedürü" hâlâ şöyle yazıyor:
  ```
  1. exactMatchMap'te doğru konuma ekle (alfabetik sıra)
  2. python scripts/check_duplicates.py AppClassifier.kt çalıştır
  ```
- **Risk:** Bu prosedürü izleyen Claude veya geliştirici AppClassifier.kt'de VAR OLMAYAN bir yapıya eklemeye çalışır — sessiz başarısızlık, paket sınıflandırılmaz.
- `check_duplicates.py AppClassifier.kt` de artık anlamsız (99 satır, içerik yok).
- **Doğru prosedür (D115 sonrası):** `assets/app_categories.json` dosyasını düzenle veya `scripts/export_classifier_json.py` aracını kullan.
- **Etki:** CLAUDE.md §5 Güncelleme Prosedürü (2 adım yanlış) + LEARNINGS.md AppClassifier Mimarisi bölümü (aynı yanlış prosedür).
- **Öneri:** Her iki dosyada prosedürü JSON-bazlı yeni akışla güncelle.

---

### K1 — LEARNINGS.md: Onboarding adım sırası CLAUDE.md ile çelişiyor ⭐ D112-D123 YENİ

- **Sorun:** `LEARNINGS.md` satır 107: "Son iki adım: **SET_LAUNCHER → CLASSIFY_MODE** → DONE sırası değiştirilemez" yazıyor.
- **Doğrusu:** CLAUDE.md §3: "son 3 adım MUTLAKA **CLASSIFY_MODE → SET_LAUNCHER** → DONE" (D120'de kullanıcı talebiyle SET_LAUNCHER sona alındı).
- **Risk:** Claude LEARNINGS.md'ye bakıp yanlış sırayı uygularsa onboarding bozulur.
- **Öneri:** LEARNINGS.md satır 107: "SET_LAUNCHER → CLASSIFY_MODE" → "CLASSIFY_MODE → SET_LAUNCHER" olarak düzelt.

---

## 🟡 Orta Öncelik

### O1 — ROADMAP.md: Multi-language support tamamlandı ama hâlâ açık görünüyor
- **Sorun:** ROADMAP.md `Şu An Ne Yapılıyor (D108)` bölümü "SettingsScreen + SettingsAppearanceSection sırada" diyor. ⭐ Yüksek Puanlı tablosunda `🔄 D108 devam ediyor` yazıyor.
- **Gerçek durum:** HISTORY.md Döngü 108-109 + D113 OnboardingScreen — toplam 60+ key, görev BİTTİ.
- **Öneri:** ROADMAP.md `Şu An Ne Yapılıyor` bölümünü temizle. ⭐ tablosundan sil. HISTORY.md Tamamlananlar'a `| D108-113 | 17 | Multi-language support (TR/EN) |` ekle.

### O2 — harcananvakit.md: D119–D123 logları eksik (D93-D118 arası da eksik)
- **Sorun:** Son log satırı D118 (2026-06-21 08:30-10:05). D119-D123 (klavye fix, onboarding redesign, Privacy Policy, iOS+AMOLED tema, görsel kalite) loglanmamış.
- **Öneri:** Retrospektif olarak HISTORY.md döngü loglarından özet çıkarılarak eklenebilir.

### O3 — Paket sayısı tutarsızlığı: 3717 vs 3702 ⭐ D112-D123 YENİ
- **Sorun:** HISTORY.md Döngü 115: "exactMatchMap **(3702 entry)** assets/app_categories.json'a taşındı." Ancak CLAUDE.md §7, LEARNINGS.md AppClassifier Mimarisi hâlâ "**3717** benzersiz paket" diyor.
- **Doğrulandı:** `assets/app_categories.json` → **3702 entry** (python ile sayıldı). 15 paket D115 export sırasında kayboldu.
- **Etki:** CLAUDE.md §7 (2 yer), LEARNINGS.md AppClassifier Mimarisi, HISTORY.md Tamamlananlar Arşivi — hepsi "3717" diyor, doğrusu 3702.
- **Öneri:** CLAUDE.md + LEARNINGS.md'de "3717" → "3702" güncelle. Kayıp 15 paketi bulmak için AppClassifier.kt geçmiş commit + JSON diff al.

### O4 — LEARNINGS.md AppClassifier Mimarisi D115 sonrası güncel değil ⭐ D112-D123 YENİ
- **Sorun:** D115'te büyük mimari değişiklik: AppClassifier.kt 4369→99 satır, `AppClassifierAssets.kt` (YENİ singleton) oluşturuldu, exactMatchMap `assets/app_categories.json`'a taşındı. LEARNINGS.md "AppClassifier Mimarisi" bölümü hâlâ eski KT dosyası anlatımıyla yazılı; `AppClassifierAssets.kt` sınıfı hiç belgelenmemiş.
- **Öneri:** LEARNINGS.md "AppClassifier Mimarisi" → "D115 itibarıyla `AppClassifierAssets.kt` singleton + `assets/app_categories.json` (122 KB); thread-safe double-check lazy parse." notu ekle.

---

## 🟢 Düşük Öncelik

### D5 — FİKİRLER.md: Düşük Öncelik tablosu çelişkili durum etiketleri ⭐ D112-D123 YENİ
- **Sorun:** "Düşük Öncelik" tablosu (satır 56-57):
  - "Dark mode tam uyum audit → **Bekliyor**" ama Puanlama tablosu #7: "✅ **[TAMAMLANDI D114]**"
  - "Multi-language support (TR/EN) → **Bekliyor**" ama Puanlama tablosu #5: "✅ **[TAMAMLANDI D113]**"
- **Öneri:** FİKİRLER.md Düşük Öncelik tablosunda bu satırları "[TAMAMLANDI D114]" ve "[TAMAMLANDI D113]" olarak güncelle.

### D6 — HISTORY.md: Sprint Özeti D112-D123 eksik ⭐ D112-D123 YENİ
- **Sorun:** Sprint Özeti tablosunun son satırı "D107-D111". D112-D123 arası 12 döngü (HomeScreen refactor, OnboardingScreen dil, JSON asset dönüşümü, 156 test, onboarding redesign, iOS+AMOLED tema, görsel kalite) tabloda yok.
- **Öneri:** `| 2026-06-21 | D112-D123 | HomeScreen refactor, OnboardingScreen dil, JSON asset 3702, 156 test, iOS+AMOLED tema, görsel kalite |` satırı ekle.

### D1 — HISTORY.md: D110/D111 ters kronoloji
- **Sorun:** HISTORY.md'de Döngü 111 (satır ~1213) Döngü 110'dan (satır ~1217) önce geliyor. D110 retroaktif eklendi.
- **Öneri:** Anlam kaybı yok, olduğu gibi bırakılabilir.

### D2 — HISTORY.md: İki ayrı "Tamamlananlar Arşivi" bölümü
- **Sorun:** HISTORY.md'de satır ~1196 ve ~1239'da aynı başlıkla iki ayrı Tamamlananlar Arşivi bölümü var.
- **Öneri:** Küçük bölümü büyük bölüme merge et.

### D3 — HISTORY.md: "Mimari Notlar" bölümünde "14 adım" stale
- **Sorun:** HISTORY.md satır 147'deki "Mimari Notlar" bölümü `Onboarding Adım Listesi (14 adım)` diyor; CLASSIFY_MODE yok.
- **Öneri:** `(D105 itibarıyla 16 adım — bkz. LEARNINGS.md)` notu ekle.

### D4 — AGENTS.md içeriği kontrol edilmedi
- **Sorun:** `AGENTS.md` dosyası var, aktif agent listesiyle senkron mu bilinmiyor.
- **Öneri:** Sonraki döngüde kontrol et.

---

## 📊 Paket Sayısı Tutarlılık Kontrolü

| Dosya | Paket Sayısı | Durum |
|-------|-------------|-------|
| CLAUDE.md §7 | 3717 | ⚠️ Güncellenmeli |
| LEARNINGS.md Mimari Kararlar | 3717 | ⚠️ Güncellenmeli |
| assets/app_categories.json | **3702** | ✅ Gerçek kaynak |
| HISTORY.md D67 | 3717 | ℹ️ Tarihi kayıt |

**Sonuç: D115 JSON export sonrası gerçek sayı 3702. CLAUDE.md + LEARNINGS.md güncellenmeli (O3).**

---

## 📊 Özet

| Öncelik | Adet | Durum |
|---------|------|-------|
| 🔴 Kritik | 2 | **K2 (YENİ 2. rutin) + K1 — Onay bekliyor (acil)** |
| 🟡 Orta | 4 | Onay bekliyor |
| 🟢 Düşük | 6 | Zaman bulununca |
| **✅ Çözülen (toplam)** | **17** | Kapatıldı |

**En acil:** K2 (YENİ) — AppClassifier Güncelleme Prosedürü aktif olarak yanlış (D115 sonrası exactMatchMap yok, JSON gerekiyor). K1 — LEARNINGS.md onboarding sıra çelişkisi (CLASSIFY_MODE ↔ SET_LAUNCHER ters yazılmış). Ardından O3 — paket sayısı 3717 değil 3702.

---

*Denetim güncellemesi: 2026-06-21 otomatik rutin (2. çalışma) | Kapsam: D105-D123 | Telegram engelli, GitHub commit ile iletildi. YENİ: K2 eklendi (AppClassifier prosedürü aktif yanlış).*
