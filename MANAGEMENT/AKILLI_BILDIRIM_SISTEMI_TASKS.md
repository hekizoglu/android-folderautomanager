# AppOrganizer — Akıllı Bildirim Sistemi (Smart Notification Engine) Yapılacaklar Dosyası

> **Meta:** Yalnızca Akıllı Bildirim Sistemi'ne özel teknik mimari, UX/UI, analiz motoru ve aşamalı modül geliştirme yapılacaklar listesi.  
> **Gizlilik İlkesi:** Zero-Data Leakage — Tüm bildirim analizleri ve sınıflandırmaları %100 cihaz üzerinde (On-Device) çalışır.  
> **Son Güncelleme:** 2026-07-26  

---

## 🎯 Modül Kapsamı ve Hedefler

- **Amaç:** Bildirim yorgunluğunu ve kırmızı nokta (badge) baskısını azaltmak; kargo, banka, acil mesaj ve reklam bildirimlerini yapay zeka/regex motoru ile ayıklayarak kullanıcıya anlamlı özet sunmak.
- **Mimari Katmanlar:**
  - `service/` → `AppNotificationListenerService.kt`
  - `domain/usecase/notification/` → `NotificationClassifierUseCase.kt`, `NotificationPriorityScorer.kt`
  - `presentation/ui/launcher/` → `SmartAccessCard.kt`, `FolderTile.kt`
  - `presentation/ui/screens/` → `SettingsNotificationsScreen.kt`, `NotificationReportScreen.kt`

---

## 📋 Aşamalı Yapılacaklar Listesi (To-Do)

### 🧱 Aşama 1: Veri Modeli ve Domain Mantığı (Engine Core)

- [ ] **AK-1.1: Bildirim Kategori Enum ve Model Tanımı**
  - **Dosyalar:** `domain/models/NotificationCategory.kt`, `domain/models/SmartNotification.kt`
  - **İçerik:** `MESSAGING`, `DELIVERY`, `FINANCE`, `PROMOTION`, `REMINDER`, `SYSTEM` kategorileri.
  - **Öncelik Puanı:** 18 ⭐

- [ ] **AK-1.2: On-Device Regex & Kelime Sınıflandırma Motoru (Classifier UseCase)**
  - **Dosyalar:** `domain/usecase/notification/NotificationClassifierUseCase.kt`
  - **İçerik:** Türkçe ve İngilizce kelime matrisi (Örn: "kargo", "teslim", "kod", "bakiye", "indirim", "fırsat").
  - **Öncelik Puanı:** 18 ⭐

- [ ] **AK-1.3: Önem Skoru ve Spam Ayıklama Algoritması (Priority Scorer)**
  - **Dosyalar:** `domain/usecase/notification/NotificationPriorityScorer.kt`
  - **İçerik:** Bildirimin önem derecesini (0-100) hesaplayan ve promosyon bildirimlerini rozet sayısından düşen kurgu.
  - **Öncelik Puanı:** 17 ⭐

---

### ⚙️ Aşama 2: Servis ve Arka Plan Entegrasyonu (Service Layer)

- [ ] **AK-2.1: AppNotificationListenerService Entegrasyonu**
  - **Dosyalar:** `service/AppNotificationListenerService.kt`
  - **İçerik:** Gelen `StatusBarNotification` verilerini anında `NotificationClassifierUseCase` ile işleme ve Flow ile UI'a yayınlama.
  - **Öncelik Puanı:** 18 ⭐

- [ ] **AK-2.2: Oda Veritabanı ve Geçmiş Temizliği (Room Persistence)**
  - **Dosyalar:** `data/local/NotificationEventDao.kt`, `data/local/AppDatabase.kt`
  - **İçerik:** Bildirim istatistiklerinin (hangi uygulama kaç kez böldü) saklanması ve 30 günün sonundaki verilerin otomatik silinmesi.
  - **Öncelik Puanı:** 16 ⭐

---

### 🎨 Aşama 3: UI ve UX Geliştirmeleri (Presentation Layer)

- [ ] **AK-3.1: Klasör Üstü Akıllı Renkli Rozetler (Smart Notification Badges)**
  - **Dosyalar:** `presentation/ui/launcher/FolderTile.kt`, `presentation/ui/launcher/BadgeColorEngine.kt`
  - **İçerik:** Mesaj için Mavi, Kargo için Yeşil, Banka/Güvenlik için Sarı/Kırmızı dinamik rozet gösterimi.
  - **Öncelik Puanı:** 17 ⭐

- [ ] **AK-3.2: Smart Access Card "Bildirimler" Sekmesi Yenilemesi**
  - **Dosyalar:** `presentation/ui/launcher/hero/SmartAccessCard.kt`
  - **İçerik:** Kategorilere ayrılmış akıllı özet kartları (Örn: `💬 3 Sohbet · 📦 1 Kargo`).
  - **Öncelik Puanı:** 18 ⭐

- [ ] **AK-3.3: Klasör Altı Akıllı Bildirim Şeridi (Smart Notification Ticker)**
  - **Dosyalar:** `presentation/ui/launcher/FolderTile.kt`
  - **İçerik:** Klasörün altında en yeni ve en önemli tek bildirimi canlı kayan yazı/şerit halinde gösterme.
  - **Öncelik Puanı:** 16 ⭐

---

### 🛠️ Aşama 4: Ayarlar ve Gizlilik Kontrolleri (Settings & Privacy)

- [ ] **AK-4.1: Bildirim Filtreleme ve Gizlilik Ayarları Ekranı**
  - **Dosyalar:** `presentation/ui/screens/SettingsNotificationsScreen.kt`, `utils/AppPrefs.kt`
  - **İçerik:**
    - `[Toggle]` Promosyon Bildirimlerini Rozetten Düş (Spam Filtresi)
    - `[Toggle]` Hassas Mesaj İçeriğini Gizle (Örn: "WhatsApp: 1 Yeni Mesaj")
    - `[Multi-Select]` Gösterilecek Bildirim Kategorileri Seçimi
  - **Öncelik Puanı:** 18 ⭐

- [ ] **AK-4.2: Bildirim Raporu ve Dijital Sağlık Analizi Ekranı**
  - **Dosyalar:** `presentation/ui/screens/NotificationReportScreen.kt`
  - **İçerik:** Kullanıcıyı en çok bölen uygulamaların grafiksel dağılımı.
  - **Öncelik Puanı:** 15 ⭐

---

## 🧪 Aşama 5: Test ve Doğrulama (QA & Verification)

- [ ] **AK-5.1: Unit Testler**
  - `NotificationClassifierUseCaseTest.kt` ile Türkçe/İngilizce kargo, banka ve mesaj örneklerinin doğru sınıflandırıldığını doğrulama.
- [ ] **AK-5.2: Fiziki Cihaz Testi**
  - Gerçek WhatsApp, Trendyol, Garanti BBVA vb. bildirimleri gönderilerek rozet rengi ve akıllı özet davranışı test edilecek.
