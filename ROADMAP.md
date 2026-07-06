# ROADMAP.md — AppOrganizer Yol Haritası

> Son güncelleme: 2026-07-01 (D195). Puanlama → FİKİRLER.md. Yüksek puanlı + basit (EA≥4) → buraya.
> İnsan onayı gereken kararlar ⚠️ · Güvenlik kritik 🔒 · Puanlar FİKİRLER.md tablosundan (15+ = bu listeye girer)
> **Kural:** Tamamlanan görevler bu dosyadan silinir → HISTORY.md Tamamlananlar Arşivi'ne taşınır.
> **🔴 Kritik UX (U1-U10):** Kullanıcı talepleri 2026-07-01. Onay bekleniyor — kod yazılmadı.
> **🔴 Kritik Build (B1-B5):** Raporlardan (time_token_analysis + issue_mitigation). Öneri aşamasında.

---

## 🎯 Hedef

Play Store yayını → Production AAB v1.0.0 hazır ✅  
Kalan: Privacy Policy + görseller + content rating + QUERY_ALL_PACKAGES beyanı

---

## 📋 Bekleyen Görevler

### 🔴 Kritik — UX & Kararlılık (Kullanıcı Tarafından İstenenler)

> Eklenme tarihi: 2026-07-01. D201 (2026-07-06) denetimi: U2/U3/U4/U6/U8/U9 kodda zaten çözülmüş,
> U5 düzeltildi → HISTORY.md "Döngü 201". Kalanlar aşağıda.

| # | Görev | Sorun | Öneri |
|---|-------|-------|-------|
| **U1** | **Ayarlar tam alt-ekran hiyerarşisi** | D199 SettingsExpandableCard + bölüm başlıkları + Arama ayrı sayfa mevcut (kısmen çözüldü); "her kategori ayrı sayfa" kısmı kaldı | Görünüm/Bildirim/Güvenlik/Hakkında için ayrı route + ekran — büyük refactor, build'li bir döngüde yapılmalı |
| **U7** | **Ana ekran görselleştirme** | GlassCard/StatChip stili tutarlı, D201'de bant-grid boşluğu düzeltildi; kapsamlı redesign kaldı | U10 ile birlikte ele alınmalı (referans launcher analizi gerektirir) |
| **U10** | **Açık kaynak referans launcher ile ana ekran revizyonu** | Mevcut tasarım tutarsız; kullanıcı "yeniden tasarla" dedi — D201'de KAPSAM DIŞI bırakıldı (tasarım araştırması gerektirir) | Açık kaynak launcher'ları tara (KISS, Lawnchair, Kvaesitso); bizim yapıya en uygun olanı referans al; HomeScreen.kt revizyonu |

### 🔴 Kritik — Build & Ortam (Raporlardan)

> D201 denetimi: B1 (`org.gradle.vfs.watch=false`) ve B3 (`kotlin.build.report.output=file`)
> gradle.properties'te zaten mevcut; B2 incelendi — res'te hiç PNG yok, işlem gereksiz; B5 daha önce
> denenmiş, KAPT+Hilt uyumsuz notuyla kapalı (gradle.properties:15-16). B4 git config değişikliği
> güvenlik kuralı gereği yapılmadı — kullanıcı isterse manuel: `git config --global pull.rebase true`.

<!-- DOCS_SCORE_HIGH_START -->
### Kirmizi Kritik - Docs/Rapor Skor Taramasi (Otomatik)

> Kaynak: docs/internal/docs_backlog_score.md. Kural: KV+U+BR+EA >= 15 ROADMAP'e girer. scripts/score_docs_backlog.ps1 -UpdateRoadmap her dongude bu blogu yeniler.

| # | Puan | Kaynak | Gorev | Oneri | Durum |
|---|------|--------|-------|-------|-------|
<!-- DOCS_SCORE_HIGH_END -->

### 🔴 Kritik — Kararlılık (Play Store Öncesi Engel)

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
| 17p | **Ana Ekran İçgörü Kartı Çeşitliliği** — `InsightEngine.kt` 8 kart türü + rotation + son 3 kart hafızası, `AssistantInsightRow.kt` tıklanabilir, `LauncherViewModel` 15dk refresh [3afebec+] |
| 19p | **Onboarding 18→5 adım** — `OnboardingScreen.kt`, `OnboardingModels.kt` [865e... önceki] |
| 17p | **Rehber Arama İndeksi (C1)** — `ContactsIndexer.kt` + ContentObserver + `READ_CONTACTS` [865e964] |
| 17p | **Dosya Arama İndeksi (C2)** — `FilesIndexer.kt` + `FilesIndexWorker.kt` + WorkManager 24h [865e964] |
| 16p | **Arama Geçmişi UI** — chip row HomeScreen'e entegre, `SearchHistoryPrefs` [865e964] |
| 15p | **FTS5 Türkçe Arama Testi (A3)** — `TurkishSearchTest.kt` 20 unit test [865e964] |
| 18p | **AppOrganizer Dashboard** — `AppOrganizerDashboardScreen.kt` [TAMAMLANDI] |
| 17p | **Room FTS5 Backend İskeleti** — `SearchDocument`, `SearchDao`, `SearchIndexer`, `SearchRepository`, v8→v9 migration [D171] |

### 🟡 Orta Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **Contacts/Files arama opt-in izin dialog** — ContextualPermissionDialog entegrasyonu | `ContextualPermissionDialog.kt` + Settings | Tamamlandı |
| **Android 14 NotificationListenerService gerçek cihaz testi** | `AppNotificationListenerService.kt` | Bekliyor |
| **BLUR-4: Gerçek cihaz testi** | blur performansı + API 26 uyumu | Bekliyor |
| **Firebase Crashlytics kurulumu** | `google-services.json` + service account | Bekliyor |
| **`cycle.ps1` uçtan uca test** | build → push → Telegram yerel | Bekliyor |
| **AppNotificationListenerService ilk açılışta restart** | gerçek cihaz test gerekli | Bekliyor |
| **Klasör taşma (overflow) sorunu** | `FolderTile`, `HomeScreen` — çok fazla uygulama/klasör ekrandan taşıyor | Bekliyor |
| **Klasör değiştirmeden sonra görsel güncelleme kalıyor** | stale UI state sorunu | Bekliyor |
| **Geri tuşuyla ilk sayfa yenileniyor** | eski/yavaş cihazlarda istenmeyen yavaşlama | Bekliyor |
| **Token logu ekle** | issue_mitigation raporu — "ölçmediğin şeyi optimize edemezsin" | Bekliyor |
| **Rakip analiz — Smart Launcher / Niagara referans** | competitor_user_research raporu — tasarım karar belgesi için | Bekliyor |

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
