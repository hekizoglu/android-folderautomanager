# ROADMAP.md — AppOrganizer Yol Haritası

> Son güncelleme: 2026-07-07 (v1.2.0 döngüsü). Puanlama → FİKİRLER.md. Yüksek puanlı + basit (EA≥4) → buraya.
> İnsan onayı gereken kararlar ⚠️ · Güvenlik kritik 🔒 · Puanlar FİKİRLER.md tablosundan (15+ = bu listeye girer)
> **Kural:** Tamamlanan görevler bu dosyadan silinir → HISTORY.md Tamamlananlar Arşivi'ne taşınır.

---

## 🎯 Hedef

Play Store yayını → Production AAB v1.0.0 hazır ✅
Kalan: Privacy Policy + görseller + content rating + QUERY_ALL_PACKAGES beyanı

---

## 📋 Bekleyen Görevler

### 🔴 Kritik — Kullanıcı Talepleri (2026-07-07)

| # | Puan | Görev | Öneri |
|---|------|-------|-------|
| **S1** | **18⭐** | **Birleşik "her şeyi ara" ana ekran araması** — tek arama çubuğu uygulama + kategori + klasör + rehber kişisi + dosya arasın; "Uygulama / Klasör" sekmesi ana ekrandan KALDIRILSIN | `HomeAppSearchBar`'ı `SearchRepository` (FTS5 çok-kaynak) ile birleştir; klasör filtresi ayrı sekme değil sonuç grubu olsun; kaynak kapatma SearchSettingsScreen'de zaten var |
| **S2** | **16⭐** | **Rehber kişisi araması ana ekranda default etkin** — kullanıcı arama yaptığında kişiler de sonuçlarda çıksın (izin verilmişse) | S1 içinde `ContactsIndexer` sonuçlarını HomeAppSearchBar'a bağla; izin yoksa sonuç grubunda "izin ver" kısayolu |
| **K1** | **17⭐** | **KAPT → KSP geçişi** — build kilitlerinin ve yavaşlığın ana kaynağı; Hilt 2.52 + Room KSP destekli (Kotlin 1.9.25) | Ayrı döngüde tek başına: plugin değişimi + `kapt(...)`→`ksp(...)` + tam build + emülatör smoke; sonra configuration cache açılabilir |

### 🔴 Kritik — Kararlılık (Play Store Öncesi Engel)

| Görev | Neden Kritik | Durum |
|-------|-------------|-------|
| **QUERY_ALL_PACKAGES Play Store beyanı** | Göndermeden önce zorunlu — eksikse APK reddedilir | ⚠️ Bekliyor |
| **Privacy Policy sayfası** | Play Store şart — GitHub Pages `/docs/privacy_policy.html` hazır, Pages aktifleştirilmeli | ⚠️ Onay gerekli |
| **Content rating anketi** | Play Store — göndermeden önce doldurulmalı | ⚠️ Bekliyor |
| **Screenshots** | Play Store — Pixel 6 emülatörü, light + dark mode | Bekliyor |
| **Release keystore (`release.jks`)** | AAB imzalamak için şart | ⚠️ Kullanıcı oluşturmalı |
| **`google-services.json`** | Firebase/Crashlytics/FCM aktivasyonu — kod hazır, dosya bekleniyor | ⚠️ Kullanıcı indirmeli |

### 🟡 Orta Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **U1: Ayarlar tam alt-ekran hiyerarşisi** | Görünüm/Bildirim/Güvenlik/Hakkında ayrı route + ekran — büyük refactor | Bekliyor |
| **U10: Açık kaynak referans launcher ile ana ekran revizyonu** | KISS/Lawnchair/Kvaesitso analizi + HomeScreen revizyonu (U7 dahil) | Bekliyor |
| **Ölü kod: Room `search_history` tablosu kaldırma** | UI kullanmıyor (SharedPreferences'ta yaşıyor); v13 migration gerekir | Bekliyor |
| **CLAUDE.md sadeleştirme** | ~390 satır → ~250; rutinleşen bölümler LEARNINGS'e | Bekliyor |
| **Android 14 NotificationListenerService gerçek cihaz testi** | `AppNotificationListenerService.kt` | Bekliyor |
| **BLUR-4: Gerçek cihaz testi** | blur performansı + API 26 uyumu | Bekliyor |
| **`cycle.ps1` uçtan uca test** | build → push → Telegram yerel | Bekliyor |
| **Geri tuşuyla ilk sayfa yenileniyor** | eski/yavaş cihazlarda istenmeyen yavaşlama | Bekliyor |
| **Token logu ekle** | issue_mitigation raporu — "ölçmediğin şeyi optimize edemezsin" | Bekliyor |
| **Rakip analiz — Smart Launcher / Niagara referans** | tasarım karar belgesi için | Bekliyor |

<!-- DOCS_SCORE_HIGH_START -->
### Kirmizi Kritik - Docs/Rapor Skor Taramasi (Otomatik)

> Kaynak: docs/internal/docs_backlog_score.md. Kural: KV+U+BR+EA >= 15 ROADMAP'e girer. scripts/score_docs_backlog.ps1 -UpdateRoadmap her dongude bu blogu yeniler.

| # | Puan | Kaynak | Gorev | Oneri | Durum |
|---|------|--------|-------|-------|-------|
<!-- DOCS_SCORE_HIGH_END -->

### 🟢 Düşük Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **AllApps double-tap gerçek cihaz testi** | emülatörde doğrulanamadı | Bekliyor |
| **Üretici kategorileri gerçek cihaz testi** | 9 yeni kategori (CAT_GOOGLE vb.) | Bekliyor |

### 🔵 Uzun Vade

- Kendi sunucu API'si (`packageName → category` endpoint) — DeepSeek fallback'e alternatif
- Wear OS companion app · Widget ekranı genişletme

---

## ⚠️ Onay Bekleyen Kararlar

| Karar | Bağlam | Durum |
|-------|--------|-------|
| Privacy Policy içeriği | Hangi veri toplandığı netleşmeli (NotificationListener, UsageStats, notification_events) | Bekliyor |
| Gemini API key | LLM fallback için, kullanıcı sağlarsa eklenir | Bekliyor |

---

> Tamamlananların tam listesi → **HISTORY.md** (✅ Tamamlananlar Arşivi bölümü)
