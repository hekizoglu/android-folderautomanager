# ROADMAP.md — AppOrganizer Yol Haritası

> Son güncelleme: 2026-06-30 (D192). Puanlama → FİKİRLER.md. Yüksek puanlı + basit (EA≥4) → buraya.
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

Uygulama sırası: bağımlılık zincirine göre.

#### 🔴 Sprint 0: Onboarding Kurtarma (kritik — kullanıcı kaçağı)

| # | Puan | Görev | EA | Durum |
|---|------|-------|-----|-------|
| O1 | **19⭐** | **Onboarding 18→5 adım radikal kesme** — Mevcut 18 adımlı onboarding kullanıcıların ~%72'sini kaybediyor. 5 adıma indir: (1) Hoşgeldin+değer önerisi, (2) Varsayılan launcher yap, (3) Tema seç, (4) Hızlı ayarlar (widget/öneri/arama/blur), (5) Varsayılan tarayıcı seç + Tamam. Tüm izinler ve özellik turları → contextual prompt'a taşınsın. (KV:5 · U:4 · BR:4 · EA:3 = **16p** + browser yenilik bonusu **19p**) | EA:3 | Bekliyor |
| O2 | **15⭐** | **Varsayılan Tarayıcı Seçimi (DONE adımı)** — Setup sonunda `RoleManager.ROLE_BROWSER` ile sistem diyaloğu açılsın. Kullanıcı Chrome/Firefox/Brave/Vivaldi arasından seçsin. `RoleManager.createRequestRoleIntent()` + API 29+ sistem UI. Cihazdaki yüklü tarayıcılar listelensin, "Sonra seçerim" skippable. (KV:4 · U:4 · BR:4 · EA:4 = **16p**) | EA:4 | Bekliyor |
| O3 | **14⭐** | **Contextual Permission Priming** — Kesilen 13 adımdaki izinler, ilgili özellik ilk kullanıldığında özel açıklama ekranı + OS diyaloğu ile sorulsun. İzin öncesi "Neden gerekli?" kartı → 20-40% daha yüksek opt-in. (KV:4 · U:3 · BR:3 · EA:4 = **14p**) | EA:4 | Bekliyor |

#### Sprint A: FTS5 Canlıya Alma (tamamlandı)

| # | Puan | Görev | EA | Durum |
|---|------|-------|-----|-------|
| A1 | **17⭐** | **FTS5 Bootstrap Tetikleme** — app+kategori indeksi sync sonrası oluşur. | EA:5 | Tamamlandı |
| A2 | **16⭐** | **App Install/Uninstall → Anlık FTS Delta** — `PackageChangeReceiver` → `searchRepository.indexApp/removeApp`. | EA:5 | Tamamlandı |
| A3 | **15⭐** | **FTS5 Türkçe Arama Testi** — Türkçe karakter ve tırnaklı sorgu doğrulaması. | EA:5 | Bekliyor |

#### Sprint B: Arama UX (A1 sonrası)

| # | Puan | Görev | EA | Durum |
|---|------|-------|-----|-------|
| B1 | **16⭐** | **Arama Geçmişi (Room tabanlı)** — `SearchHistory` entity, son 20 arama chip row, tek tuşla temizleme. | EA:5 | Bekliyor |
| B2 | **15⭐** | **Arama Kaynakları Ayar Bölümü** — Settings'te kaynak toggle'ları + indeks durum chip'i. | EA:4 | Bekliyor |

#### Sprint C: Birleşik Arama Genişletme (orta efor)

| # | Puan | Görev | EA | Durum |
|---|------|-------|-----|-------|
| C1 | **17⭐** | **Yerel Arama İndeksi v1 — Contacts** — `READ_CONTACTS` + ContentObserver + opt-in. Sprint 2. | EA:3 | Bekliyor |
| C2 | **17⭐** | **Yerel Arama İndeksi v1 — Files** — `MediaStore` + SAF + WorkManager periodic. Sprint 3. | EA:3 | Bekliyor |

#### ✅ Tamamlanan Yüksek Puanlı

| Puan | Görev |
|------|-------|
| 18p | **AppOrganizer Dashboard** — `AppOrganizerDashboardScreen.kt` [TAMAMLANDI] |
| 17p | **Room FTS5 Backend İskeleti** — `SearchDocument`, `SearchDao`, `SearchIndexer`, `SearchRepository`, v8→v9 migration [D171] |

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
