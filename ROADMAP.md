# ROADMAP.md — AppOrganizer Yol Haritası

> Son güncelleme: 2026-07-08 (v1.2.0 döngüsü). Puanlama → FİKİRLER.md. Yüksek puanlı + basit (EA≥4) → buraya.
> İnsan onayı gereken kararlar ⚠️ · Güvenlik kritik 🔒 · Puanlar FİKİRLER.md tablosundan (15+ = bu listeye girer)
> **Kural:** Tamamlanan görevler bu dosyadan silinir → HISTORY.md Tamamlananlar Arşivi'ne taşınır.

---

## 🎯 Hedef

Play Store yayını → Production AAB v1.0.0 hazır ✅
Kalan: Privacy Policy + görseller + content rating + QUERY_ALL_PACKAGES beyanı

---

## 📋 Bekleyen Görevler

### 🔴 Kritik — Kararlılık (Play Store Öncesi Engel)

| Görev | Neden Kritik | Durum |
|-------|-------------|-------|
| **QUERY_ALL_PACKAGES Play Store beyanı** | Göndermeden önce zorunlu — eksikse APK reddedilir | ⚠️ Bekliyor |
| **Privacy Policy sayfası** | Play Store şart — GitHub Pages'te yayında, URL curl ile 200 doğrulandı (Döngü 214) | ✅ Yayında — Play Console'a girilmeli |
| **Data Safety + Privacy Policy uyum paketi** | Kod tarafı tamamlandı (Döngü 214-215): Firebase/Crashlytics, kişi rehberi, bildirim metni, Accessibility Service artık doğru anlatılıyor. Kalan: Play Console Data Safety **formunun** doldurulması | ⚠️ Bekliyor — dış aksiyon (Play Console erişimi) |
| **Content rating anketi** | Play Store — göndermeden önce doldurulmalı | ⚠️ Bekliyor |
| **Screenshots** | Play Store — Pixel 6 emülatörü, light + dark mode | Bekliyor |
| **Release keystore (`release.jks`)** | AAB imzalamak için şart | ⚠️ Kullanıcı oluşturmalı |

### 🟡 Orta Öncelik

| Görev | Alan | Durum |
|-------|------|-------|
| **U10: Açık kaynak referans launcher ile ana ekran revizyonu** | KISS/Lawnchair/Kvaesitso analizi + HomeScreen revizyonu (U7 dahil) | Bekliyor |
| **Android 14 NotificationListenerService gerçek cihaz testi** | `AppNotificationListenerService.kt` | Bekliyor |
| **Play öncesi gerçek cihaz QA paketi** | NotificationListener, screenshot smoke, backup/restore, worker schedule, blur/API26 kanıtlı test | Bekliyor |
| **Akıllı Bildirim Analiz Sistemi (15p — bkz. R7 ve FİKİRLER.md ⭐)** | NotificationListener + `notification_events` + `NotificationAnalyzer` + `SmartInsightWorker` hattı privacy-first rapor, öneri ve günlük akıllı bildirim sistemine tamamlanacak. Detay ve kabul kriterleri aşağıda. | Bekliyor |
| **BLUR-4: Gerçek cihaz testi** | blur performansı + API 26 uyumu | Bekliyor |

### 🧠 Akıllı Bildirim Analiz Sistemi — Detay

**Puan/kaynak:** 15p (KV:4 · U:3 · BR:4 · EA:4) — bkz. FİKİRLER.md ⭐ Yüksek Puanlı, DOCS_SCORE_HIGH R7 ile eşleşir.

**Amaç:** Kullanıcıya "hangi uygulama ne kadar bildirim gönderiyor, hangisi gerçekten rahatsız ediyor, hangisi dikkat dağıtıyor, hangisi sessize alınmalı" sorularının cevabını yerel ve anlaşılır şekilde vermek. Sistem Play Store tarafında hassas izin ve veri beyanı riski taşıdığı için varsayılan tasarım privacy-first olmalı.

**Mevcut temel:** `AppNotificationListenerService` bildirim olaylarını yakalıyor, `notification_events` tablosu paket/zaman verisini tutuyor, `NotificationAnalyzer` çok konuşan/rahatsız eden/dikkat dağıtan kategorilerini üretiyor, `NotificationReportScreen` raporu gösteriyor, `SmartInsightWorker` ise günlük akıllı bildirim üretmek için kullanılabiliyor.

**Veri ilkesi:**
- Analiz için ana veri paket adı, bildirim zamanı ve sayım olmalı; bildirim içeriği analitik kayıt olarak saklanmamalı.
- Bildirim metni sadece launcher badge/son bildirim gösterimi gibi anlık UI ihtiyacı için tutuluyorsa privacy policy ve Data Safety metinleri buna göre netleştirilmeli.
- Kullanıcı ayarlardan bildirim analizini kapatabilmeli ve kayıtları temizleyebilmelidir.
- Eski kayıtlar otomatik silinmeli; mevcut 30 gün temizleme davranışı kabul kriteri olarak korunmalı.

**Analiz modülleri:**
- Çok konuşan uygulamalar: Son 7/30 gün bildirim sayısı ve günlük ortalama.
- Rahatsız eden uygulamalar: Gece, odak saati veya kısa aralıkta tekrar eden bildirim kümeleri.
- Dikkat dağıtan uygulamalar: Bildirim sayısı yüksek ama kullanım/etkileşim karşılığı düşük uygulamalar.
- Sessize alma önerileri: Uygulama bazlı "bildirimleri azalt", "sessize almayı düşün", "sadece önemli kategorileri açık bırak" önerileri.
- Trend özeti: Bu hafta geçen haftaya göre artan/azalan bildirim yükü.

**UI akışı:**
- Ayarlar > Bildirimler içinde ana anahtar, alt analiz türleri ve günlük bildirim saati net kalmalı.
- Rapor ekranında özet kartları, uygulama listeleri, boş durum ve veri izni kapalı durumu ayrı gösterilmeli.
- Ana ekranda yalnızca düşük gürültülü bir "akıllı içgörü" kartı/ticker kullanılmalı; sistem kendi başına yeni bildirim kalabalığı üretmemeli.

**Worker ve zamanlama:**
- `SmartInsightWorker` tekil WorkManager işi olarak çalışmalı; tekrar eden işlerde duplicate notification üretilmemeli.
- `KEY_SMART_NOTIF_ENABLED`, alt toggle'lar ve `KEY_SMART_NOTIF_HOUR` birlikte hesaba katılmalı.
- Saat değişince iş yeniden planlanmalı, master kapatılınca iptal edilmeli.
- Android 13+ `POST_NOTIFICATIONS` izni yoksa kullanıcıya bildirim göndermeye çalışmadan sessizce rapor üretmeye devam etmeli.

**Play Store ve izin uyumu:**
- Notification access isteği ekranda bağlamıyla anlatılmalı: amaç bildirim yoğunluğunu ölçmek ve kullanıcıya yerel rapor sunmak.
- Privacy Policy, Data Safety ve uygulama içi açıklamalar aynı cümlelerle uyumlu olmalı; "bildirim içeriği okunmaz/saklanmaz" iddiası kod gerçeğiyle birebir doğrulanmadan kullanılmamalı.
- UsageStats + NotificationListener birleşimi profil çıkarma gibi algılanabileceği için tüm öneriler cihaz içinde kalmalı, dış servise gönderilmemeli.

**Kabul kriterleri:**
- Android 14 gerçek cihazda NotificationListener açma/kapama, olay yazma ve 30 gün temizlik testi geçer.
- Analiz kapalıyken `notification_events` yeni kayıt almaz.
- Rapor ekranı veri yok, izin kapalı, analiz kapalı ve normal veri senaryolarında doğru görünür.
- Günlük akıllı bildirim seçilen saatte ve tek kopya gelir.
- Privacy Policy + Data Safety + ayar ekranı açıklamaları kodla çelişmez.
- Build ve en az bir gerçek cihaz smoke testi tamamlanmadan Play öncesi tamamlandı sayılmaz.

**Alt görevler (Döngü 215'te kod incelemesiyle bölündü):**
1. ✅ DB kayıt ilkesi doğrulandı — `notification_events` yalnızca `packageName`+`postedAt` tutuyor, içerik yok (`NotificationEvent.kt`).
2. ⏳ Analiz kapalıyken yeni kayıt yazılmadığı testi — kod `AppNotificationListenerService.kt`'de ayarlanabilir gibi görünüyor, gerçek cihazda doğrulanmadı.
3. ✅ 30 gün temizlik davranışı kodda var (`deleteOlderThan`), gerçek cihazda uzun süreli testi yapılmadı.
4. ⏳ `NotificationAnalyzer` çok konuşan/rahatsız eden/dikkat dağıtan senaryoları test edilmedi.
5. ✅ `SmartInsightWorker` duplicate notification riski incelendi (GÖREV 4) — `enqueueUniquePeriodicWork` + `REPLACE` policy ile risk düşük.
6. ⏳ `POST_NOTIFICATIONS` izni yoksa worker'ın sessiz davrandığı doğrulanmadı.
7. ⏳ UI'da boş durum, izin kapalı durum, analiz kapalı durum ayrımı `NotificationReportScreen`'de gözden geçirilmedi.

<!-- DOCS_SCORE_HIGH_START -->
### Kirmizi Kritik - Docs/Rapor Skor Taramasi (Otomatik)

> Kaynak: docs/internal/docs_backlog_score.md. Kural: KV+U+BR+EA >= 15 ROADMAP'e girer. scripts/score_docs_backlog.ps1 -UpdateRoadmap her dongude bu blogu yeniler.

| # | Puan | Kaynak | Gorev | Oneri | Durum |
|---|------|--------|-------|-------|-------|
| **R1** | **18** | FIKIRLER.md; HISTORY.md Dongu 214-215 | **Play Store Privacy/Data Safety uyum paketi** | Privacy policy, Data Safety, Firebase/Crashlytics/Analytics/FCM, DeepSeek, Drive/SAF ve package inventory beyanlari ayni hikayeye cekilsin. Kod tarafi tamamlandi (Dongu 214-215); Play Console Data Safety formu doldurma dis aksiyon olarak kaliyor. | Bekliyor |
| **R4** | **16** | ROADMAP.md | **Play Store release imza ve submission kapisi** | Release keystore, content rating, QUERY_ALL_PACKAGES declaration ve final AAB temiz committen build akisina baglansin. | Bekliyor |
| **R6** | **15** | ROADMAP.md | **Gercek cihaz Play-oncesi QA paketi** | Android 14 NotificationListener, screenshot smoke, backup/restore, worker schedule ve blur/API26 tek kanitli pakette kosulsun. | Bekliyor |
| **R7** | **15** | ROADMAP.md; FIKIRLER.md | **Akilli Bildirim Analiz Sistemi** | NotificationListener + notification_events + NotificationAnalyzer + SmartInsightWorker hatti privacy-first rapor, oneri ve gunluk akilli bildirim sistemine tamamlanmali. Detay ve kabul kriterleri ROADMAP.md'deki Detay bolumunde. | Bekliyor |
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
