# Denetim Fix ve Sadeleştirme Planı

> **Oluşturma:** 2026-07-19 · **Kaynak:** Harici kapsamlı denetim raporu (Fable triyajı ile) · **Kural:** Her madde önce KODA KARŞI DOĞRULANIR, sonra düzeltilir — rapor statik analizdi, yanlış alarm olabilir (bkz. A3 dersi). Tamamlanan bölüm HISTORY'ye taşınır + silinir; dosya boşalınca silinir. Her 6 döngüde APK.

## PAKET F — Güvenlik & Mantık Fix'leri (öncelikli)

> ✅ **F1 + F2 + F3 + F4 tamamlandı (2026-07-19)** — iddialar doğrulandı ve kapatıldı; HISTORY.md'de.

### F5 — Focus süresi gece yarısı bölünmesi (P1)
İddia (Fable onaylı — G3a eksiği): 23:50-00:20 oturumu 30dk'yı yeni güne yazıyor.
Yapılacak: endFocusSession süreyi gün sınırında böl; getFocusMinutesToday devam eden oturumda yalnız bugünün 00:00 sonrasını saysın; sınır testleri.
Durum: ⏳ Bekliyor

### F6 — Çekmece kategori onClick + Firebase arama event düzeltmesi (P1, küçük)
İddia A: arama sonucundaki kategori satırı tıklanabilir görünüyor ama onClick boş → klasörü açsın.
İddia B: arama eventi her sorgu değişiminde tetikleniyor + sourceMix=APPS_ONLY yanlış → debounce sonrası/sonuç açılınca tek event, gerçek kaynak karışımı.
Durum: ⏳ Bekliyor

### F7 — Küçük tutarlılık temizliği (P2)
`if (false && ticker...)` ölü kodu; home pager varsayılan yorum/kod çelişkisi (KEY_HOME_PAGER_V2 vs KEY_SMART_DASHBOARD default'ları — tek doğruluk kaynağına indir); Firebase bağlantı testi metnini dürüstleştir ("SDK yapılandırması başarılı; panel görünürlüğü ayrıca doğrulanmalı").
Durum: ⏳ Bekliyor

## PAKET S — Sadeleştirme (F bitince)

### S1 — Tek "BUGÜN" kartı
HomeIntelligenceCardsRow (2 kart) + Bugün Yüklendi + asistan satırları → TEK bağlamsal kart: öncelik sırası (kritik izin > riskli görev > klasör incelemesi > rapor hazır > denge özeti). M07 birincil-aksiyon seçici temel alınır. Eski kartlar toggle'larla geri açılabilir (güç kullanıcı).
Durum: ⏳ Bekliyor

### S2 — Ödül yüzeyi sadeleştirme
Görevler ekranı başlığında yalnız: Yıldız + Seviye + Seri. Görev puanı/altın seri/dondurma → görev detay/genişletilebilir bölüm. Görev puanı kullanıcı ekonomisi olarak gizlenir (pulse girdisi kalır). Usta (100⭐) seviyesine gerçek ödül: özel saat stili + klasör teması.
Durum: ⏳ Bekliyor

### S3 — Çekmece sadeleştirme
Varsayılan: arama + liste + alfabetik sidebar. Filtre/sıralama chip satırları → tek overflow/sort butonu. Son yüklenenler/bildirim gelenler çekmecede kalır, ana ekran tekrarı S1 ile zaten kalkar.
Durum: ⏳ Bekliyor

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
