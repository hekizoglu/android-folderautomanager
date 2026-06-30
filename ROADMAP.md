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

#### ✅ Sprint 0: Onboarding Kurtarma — Tamamlandı

| # | Puan | Görev | Durum |
|---|------|-------|-------|
| O1 | **19⭐** | Onboarding 18→5 adım | ✅ Tamamlandı |
| O2 | **15⭐** | Varsayılan Tarayıcı Seçimi (RoleManager.ROLE_BROWSER) | ✅ Tamamlandı |
| O3 | **14⭐** | ContextualPermissionDialog | ✅ Tamamlandı |

#### Sprint A: FTS5 Canlıya Alma (tamamlandı)

| # | Puan | Görev | EA | Durum |
|---|------|-------|-----|-------|
| A1 | **17⭐** | **FTS5 Bootstrap Tetikleme** — app+kategori indeksi sync sonrası oluşur. | EA:5 | Tamamlandı |
| A2 | **16⭐** | **App Install/Uninstall → Anlık FTS Delta** — `PackageChangeReceiver` → `searchRepository.indexApp/removeApp`. | EA:5 | Tamamlandı |
#### Sprint B: Arama UX (A1 sonrası)

| # | Puan | Görev | EA | Durum |
|---|------|-------|-----|-------|
| B2 | **15⭐** | **Arama Kaynakları Ayar Bölümü** — Settings'te kaynak toggle'ları + indeks durum chip'i. | EA:4 | Tamamlandı (SearchSettingsScreen.kt) |

#### Sprint C: Birleşik Arama Genişletme (orta efor)

— Tüm Sprint C görevleri tamamlandı.

#### ✅ Tamamlanan Yüksek Puanlı

| Puan | Görev |
|------|-------|
| 19p | **Onboarding 18→5 adım** — `OnboardingScreen.kt`, `OnboardingModels.kt` [865e... önceki] |
| 17p | **Rehber Arama İndeksi (C1)** — `ContactsIndexer.kt` + ContentObserver + `READ_CONTACTS` [865e964] |
| 17p | **Dosya Arama İndeksi (C2)** — `FilesIndexer.kt` + `FilesIndexWorker.kt` + WorkManager 24h [865e964] |
| 16p | **Arama Geçmişi UI** — chip row HomeScreen'e entegre, `SearchHistoryPrefs` [865e964] |
| 15p | **FTS5 Türkçe Arama Testi (A3)** — `TurkishSearchTest.kt` 20 unit test [865e964] |
| 18p | **AppOrganizer Dashboard** — `AppOrganizerDashboardScreen.kt` [TAMAMLANDI] |
| 17p | **Room FTS5 Backend İskeleti** — `SearchDocument`, `SearchDao`, `SearchIndexer`, `SearchRepository`, v8→v9 migration [D171] |

### ⭐ Yeni Görev — Ana Ekran İçgörü Kartı Çeşitliliği (17p)

**Sorun:** `AssistantInsightRow` hep aynı içgörüyü gösteriyor ("en çok açılan uygulama: Telegram"). Statik ve tekrar eden içerik kullanıcının gözünde değersizleşiyor.

**Çözüm:** Her oturumda/her N dakikada bir rastgele içgörü türü seçilsin:

| İçgörü Türü | Örnek | Veri Kaynağı |
|-------------|-------|-------------|
| En çok açılan | "Bu hafta en çok Telegram kullandın" | `usageCount` |
| Hiç açılmayan | "Instagram'ı 30 gündür açmadın, silmeyi düşün?" | `usageCount == 0 \|\| lastUsed < 30d` |
| Yeni yüklenen | "Instagram geçen hafta kuruldu, bir bak?" | `installTime < 7d` |
| Büyük uygulama | "WhatsApp 120MB yer kaplıyor" | `appSizeBytes` |
| Bildirim yoğunu | "Email uygulaması bugün 14 bildirim gönderdi" | `notificationCount` |
| Kategori özeti | "Oyun klasöründe 8 uygulama var, kaçını açtın?" | `folders + usageCount` |
| Motivasyon/soru | "Bu hafta kaç farklı uygulama kullandın?" | `distinct usageCount > 0` |

**Teknik notlar:**
- `InsightEngine.kt` — `List<InsightCard>` üret, `Random.nextInt()` ile seç
- Aynı kart 3 kez üst üste çıkmasın → `SharedPrefs`'te son 3 kart ID sakla
- Kart tıklanabilir → ilgili uygulamayı aç veya klasöre git
- Refresh: her `onResume` + 15 dakikada bir `LaunchedEffect`

**Dosyalar:** `AssistantInsightRow.kt`, yeni `InsightEngine.kt`, `LauncherViewModel.kt` (insightCards flow)

**Puan:** KV:4 · U:4 · BR:4 · EA:4 = **16p** + UX özgünlük bonusu = **17p**

---

### 🟡 Orta Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **Contacts/Files arama opt-in izin dialog** — ContextualPermissionDialog entegrasyonu | `ContextualPermissionDialog.kt` + Settings | Tamamlandı |
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
