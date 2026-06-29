# ROADMAP.md — AppOrganizer Yol Haritası

> Son güncelleme: 2026-06-29 (D191). Yeni görevler → **FİKİRLER.md**. Bu dosya dondurulmus durumu gösterir.
> İnsan onayı gereken kararlar ⚠️ · Güvenlik kritik 🔒 · Puanlar FİKİRLER.md tablosundan (15+ = bu listeye girer)
> **Kural:** Tamamlanan görevler bu dosyadan silinir → HISTORY.md Tamamlananlar Arşivi'ne taşınır.

---

## 🎯 Hedef

Play Store yayını → Production AAB v1.0.0 hazır ✅  
Kalan: Privacy Policy + görseller + content rating + QUERY_ALL_PACKAGES beyanı

---

## 📋 Bekleyen Görevler

### 🔴 Kritik (Engel)

| Görev | Neden Kritik | Durum |
|-------|-------------|-------|
| **QUERY_ALL_PACKAGES Play Store beyanı** | Göndermeden önce zorunlu — eksikse APK reddedilir | ⚠️ Bekliyor |
| **Privacy Policy sayfası** | Play Store şart — GitHub Pages `/docs/privacy_policy.html` hazır, Pages aktifleştirilmeli | ⚠️ Onay gerekli |
| **Content rating anketi** | Play Store — göndermeden önce doldurulmalı | ⚠️ Bekliyor |
| **Screenshots** | Play Store — Pixel 6 emülatörü, light + dark mode | Bekliyor |

### ⭐ Yüksek Puanlı (FİKİRLER.md'den — ≥15p)

| Puan | Görev | Durum |
|------|-------|-------|
| 18p | **AppOrganizer Dashboard** — `AppOrganizerDashboardScreen.kt`: klasör/uygulama istatistikleri, top uygulama, UsageScore trend, swipe-up sayacı. Mevcut UsageReportScreen + InsightEngine verileri kullanılır. | [TAMAMLANDI] |
| 17p | **Yerel Arama İndeksi v1** — `MediaStore` + `ContactsContract` + Room FTS4 tabanlı birleşik arama; uygulama+kategori+kişi+dosya. Varsayılan: uygulama+kategori açık, kişi+dosya kapalı. | Bekliyor |
| 15p | **Arama Kaynakları Ayar Bölümü** — Settings'te ayrı arama bölümü, izin gerekçesi + indeks durum bilgisi. Yerel Arama İndeksi tamamlandıktan sonra. | Bekliyor |

### 🟡 Orta Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **Android 14 NotificationListenerService gerçek cihaz testi** | `AppNotificationListenerService.kt` | Bekliyor |
| **BLUR-4: Gerçek cihaz testi** | blur performansı + API 26 uyumu | Bekliyor |
| **Firebase Crashlytics kurulumu** | `google-services.json` + service account | Bekliyor |
| **`cycle.ps1` uçtan uca test** | build → push → Telegram yerel | Bekliyor |
| **AppNotificationListenerService ilk açılışta restart** | gerçek cihaz test gerekli | Bekliyor |
| **Klasör taşma (overflow) sorunu** | `FolderTile`, `HomeScreen` — çok fazla uygulama/klasör ekrandan taşıyor; adaptive layout ile kısmen cozuldu (D174 tablet), phone'a uyarlanmali | Bekliyor |
| **Klasör değiştirmeden sonra görsel güncelleme kalıyor** | Kategori değişikliği sonrası ilk sayfa yenilenene kadar eski görsel görünüyor; stale UI state sorunu | Bekliyor |
| **Geri tuşuyla ilk sayfa yenileniyor (eski/yavaş cihaz sorunu)** | Back press tetikliyor yenileme; eski/performans düşük cihazlarda istenmeyen yavaşlama/geçiş yaşanabilir | Bekliyor |

### 🟢 Düşük Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **AllApps double-tap gerçek cihaz testi** | emülatörde doğrulanamadı | Bekliyor |
| **Üretici kategorileri gerçek cihaz testi** | 9 yeni kategori (CAT_GOOGLE vb.) | Bekliyor |

### 🔵 Uzun Vade

- Kendi sunucu API'si (`packageName → category` endpoint) — DeepSeek fallback'e alternatif
- Wear OS companion app · Widget ekranı genişletme
- Arama genişletme: isteğe bağlı olarak telefon rehberindeki isimler de dahil edilebilsin (kişi araması)

---

## ⚠️ Onay Bekleyen Kararlar

| Karar | Bağlam | Durum |
|-------|--------|-------|
| Privacy Policy içeriği | Hangi veri toplandığı netleşmeli (NotificationListener, UsageStats) | Bekliyor |
| Gemini API key | LLM fallback için, kullanıcı sağlarsa eklenir | Bekliyor |

---

> Tamamlananların tam listesi → **HISTORY.md** (✅ Tamamlananlar Arşivi bölümü)
