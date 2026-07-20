# Denetim Fix ve Sadeleştirme Planı

> **Oluşturma:** 2026-07-19 · **Kaynak:** Harici kapsamlı denetim raporu (Fable triyajı ile) · **Kural:** Her madde önce KODA KARŞI DOĞRULANIR, sonra düzeltilir — rapor statik analizdi, yanlış alarm olabilir (bkz. A3 dersi). Tamamlanan bölüm HISTORY'ye taşınır + silinir; dosya boşalınca silinir. Her 6 döngüde APK.

## PAKET F — Güvenlik & Mantık Fix'leri

> ✅ **PAKET F TAMAMLANDI (2026-07-19, F1-F7)** — tüm iddialar koda karşı doğrulandı ve kapatıldı; ayrıntılar HISTORY.md'de.

## PAKET S — Sadeleştirme

> ✅ **S1 + S2 + S3 tamamlandı (2026-07-20)** — HISTORY.md'de.

### S4 — Ayarlar yeniden yapısı
Rapordaki 6 hub: Ana Ekran / Arama-Çekmece / Otomatik Düzenleme / Dijital Yaşam / Gizlilik-Veri / Gelişmiş-Destek. DeepSeek anahtarı Gizlilik-Veri'ye taşınır (F1 ile koordineli — AI koçu açıkken anahtar alanı HER ZAMAN erişilebilir). Sağlık raporu + Firebase araçları Gelişmiş'e.
Durum: ⏳ Bekliyor

### S5 — Rapor birleştirme
5 rapor: Haftalık Özet / Uygulama Düzeni / Bildirimler / Gizlilik / Teknik Tanılama (destek aracı olarak ayrık). Genel Bakış+Kullanım+Haftalık tekrar tekilleştirilir.
Durum: ⏳ Bekliyor

### S6 — FCM kararı + koç görünürlük kuralları
FCM: aktif backend yoksa çıkar → günlük WorkManager DB güncellemesi (zaten açılışta var — haftalık periodic yeterli). Koç: haftalık raporda max 2 öneri, ana ekranda konuşmaz, "tüm veriler cihazda" metni düzeltilir (AI açıkken özet DeepSeek'e gider ibaresi).
Durum: ⏳ Bekliyor

### S7 — Wrapped/AI koç anahtar akışı
AI koçu açık + anahtar yok → yönlendirici durum ("Anahtar gerekli → Ayarlar") WrappedViewModel'de; sessiz başarısızlık kalkar.
Durum: ⏳ Bekliyor

## Kırmızı çizgiler
Onboarding sırası değişmez · her görünür değişiklik toggle'lı · silme yerine arka plana alma (kullanıcı özellikleri kaybolmaz) · uygulama adları telemetriye sızmaz · doğrulamadan düzeltme yok.
