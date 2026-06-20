# ROADMAP.md — AppOrganizer Yol Haritası

> Son güncelleme: 2026-06-21 (D104 sonrası). Yeni görevler → **FİKİRLER.md**. Bu dosya aktif durumu gösterir.
> İnsan onayı gereken kararlar ⚠️ · Güvenlik kritik 🔒 · Puanlar FİKİRLER.md tablosundan (15+ = bu listeye girer)

---

## 🎯 Hedef

Play Store yayını → Production AAB v1.0.0 hazır ✅  
Kalan: Privacy Policy + görseller + content rating + QUERY_ALL_PACKAGES beyanı

---

## 🔥 Şu An Ne Yapılıyor (D105+)

Dark mode audit tamamlandı (D104). Sıradaki: Onboarding tutarsızlığı fix + Multi-language altyapısı.

---

## 📋 Bekleyen Görevler

### 🔴 Kritik (Engel)

| Görev | Neden Kritik | Durum |
|-------|-------------|-------|
| **QUERY_ALL_PACKAGES Play Store beyanı** | Göndermeden önce zorunlu — eksikse APK reddedilir | ⚠️ Bekliyor |
| **Privacy Policy sayfası** | Play Store şart — GitHub Pages `/docs/privacy_policy.html` hazır, Pages aktifleştirilmeli | ⚠️ Onay gerekli |
| **Content rating anketi** | Play Store — göndermeden önce doldurulmalı | ⚠️ Bekliyor |
| **Screenshots** | Play Store — Pixel 6 emülatörü, light + dark mode | Bekliyor |

### ⭐ Yüksek Puanlı — Kodlanabilir (FİKİRLER.md 15+ puan, Play Store dışı)

| Puan | Görev | Alan | Durum |
|------|-------|------|-------|
| 17 | **Onboarding adım sırası fix** | CLAUDE.md "14+2" ↔ LEARNINGS.md "14" tutarsızlığı + kod doğrulama | ✅ D105 — 16 adım, SET_LAUNCHER → CLASSIFY_MODE → DONE |
| 17 | **Multi-language support (TR/EN)** | `strings.xml` + `values-en/strings.xml` altyapısı | Bekliyor |
| 16 | **Klasör sıra numarasıyla yer değiştirme** | FolderSheet / HomeScreen — numara girerek taşıma | ✅ D106 |
| 16 | **Akıllı Uygulama Önerileri (30dk)** | `suggestedApps` — saat/kullanım alışkanlığına göre | ✅ D107 — Yaklaşım B: recency+freq+timeSlot |
| 16 | **Dark mode tam uyum audit** | Kalan 🟡 "İncelenmeli" renkler | ✅ Kısmen D104 |

### 🟡 Orta Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **Android 14 NotificationListenerService gerçek cihaz testi** | `AppNotificationListenerService.kt` | Bekliyor |
| **BLUR-4: Gerçek cihaz testi** | blur performansı + API 26 uyumu | Bekliyor |
| **Firebase Crashlytics kurulumu** | `google-services.json` + service account | Bekliyor |
| **`cycle.ps1` uçtan uca test** | build → push → Telegram yerel | Bekliyor |
| **AppNotificationListenerService ilk açılışta restart** | gerçek cihaz test gerekli | Bekliyor |

### 🟢 Düşük Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **Unit test coverage** | LauncherViewModel MockK testleri | Bekliyor |
| **Hilt DI kurulumu** | manuel `new()` çağrılarını temizle | Bekliyor |
| **AppClassifier → JSON asset** | `assets/app_categories.json` + runtime parse | Tartışma ⚠️ |
| **AllApps double-tap gerçek cihaz testi** | emülatörde doğrulanamadı | Bekliyor |
| **Üretici kategorileri gerçek cihaz testi** | 9 yeni kategori (CAT_GOOGLE vb.) | Bekliyor |

### 🔵 Uzun Vade

- Kendi sunucu API'si (`packageName → category` endpoint) — DeepSeek fallback'e alternatif
- Backup/restore: manuel export/import UI + bulut senkron
- Wear OS companion app · Tablet layout · Widget ekranı genişletme

---

## ⚠️ Onay Bekleyen Kararlar

| Karar | Bağlam | Durum |
|-------|--------|-------|
| Privacy Policy içeriği | Hangi veri toplandığı netleşmeli (NotificationListener, UsageStats) | Bekliyor |
| AppClassifier → JSON asset | Derleme süresi + duplicate riski azalır, runtime parse maliyeti artar | Tartışma |
| Gemini API key | LLM fallback için, kullanıcı sağlarsa eklenir | Bekliyor |

---

> Tamamlananların tam listesi → **HISTORY.md** (✅ Tamamlananlar Arşivi bölümü)
