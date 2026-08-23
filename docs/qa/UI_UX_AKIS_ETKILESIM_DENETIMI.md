# UI/UX Akış ve Etkileşim Denetimi — Home V2

**Tarih:** 23 Ağustos 2026 · **Kapsam:** Home V2 ana ekranı, klasör sayfaları, uygulama çekmecesi, dock, bağlam menüsü, geri tuşu akışları
**Yöntem:** Kod düzeyinde sistematik etkileşim denetimi (jest çakışmaları, geri tuşu akışları, boş/yükleme/hata durumları, erişilebilirlik, tutarlılık) + Robolectric görsel test çapraz doğrulaması

## Özet

| Önem | Bulgu | Durum |
|------|-------|-------|
| **P0** | B-01: Geri tuşu koruması eksik (OEM predictive-back regresyonu) | ✅ Düzeltildi |
| P1 | B-02: Sınıflandırma banner'ı yanlış hedefe yönlendiriyor | ✅ Düzeltildi |
| P1 | B-03: Nabız/görev çipleri tıklanamıyor (ölü etkileşim) | ✅ Düzeltildi |
| P2 | B-04: Yükleme durumu açıklamasız spinner | ✅ Düzeltildi |
| P2 | B-05: Klasör satırında hızlı başlat eşiği vs çekmece swipe'ı | 📝 Tasarım notu |
| P2 | B-06: Klasör sayfası tamamen boşsa yönlendirme yok | 📝 Tasarım notu |

---

## Bulgular

### B-01 (P0) — Geri tuşu koruması eksik ✅ DÜZELTİLDİ

**Bulgu:** Legacy HomeScreen'de daima aktif `BackHandler(enabled = true)` vardı: çekmece açıksa kapatır, kökte geri tuşunu yutardı. Legacy yorumu açık uyarı içeriyordu: *"Android 13+ predictive back / MIUI-HyperOS'ta BackHandler yoksa sistem geri tuşu LauncherActivity'yi sonlandırabiliyor — sonraki HOME basışında sıfırdan onCreate/reload"*. HomeV2'ye bu koruma taşınmamıştı.

**Risk:** Kullanıcı çekmecede geri tuşuna basınca çekmece kapanmıyor; kökte bazı OEM'lerde launcher Activity'si sonlanabiliyor (soğuk yeniden başlatma hissi).

**Düzeltme:** `HomeV2Screen`'e daima aktif `BackHandler` eklendi: çekmece açıksa `vm.closeAllApps()`, kökte geri tuşu yutulur. ModalBottomSheet'ler (bağlam menüsü/kategori seçici) kompozisyonda daha derinde oldukları için kendi BackHandler'larıyla öncelikli kapanır.

### B-02 (P1) — Sınıflandırma banner'ı yanlış hedef ✅ DÜZELTİLDİ

**Bulgu:** "N uygulama sınıflandırma bekliyor" banner'ının eylemi `vm.openAllApps()` idi — kullanıcıyı çekmeceye götürüyor, oysa beklenen akış sınıflandırma inceleme ekranı.

**Düzeltme:** `BANNER_ID_PENDING_CLASSIFICATIONS` eylemi `Routes.APP_LIST_UNCERTAIN` rotasına yönlendiriliyor. Bildirim izni banner'ı sistem ayarlarına (mevcut davranış korunur).

### B-03 (P1) — Nabız/görev çipleri ölü etkileşim ✅ DÜZELTİLDİ

**Bulgu:** `PulseStripV2` çipleri bilgi gösteriyordu ama tıklanamıyordu; legacy Hero kartları ilgili ekranlara açılıyordu.

**Düzeltme:** Çipler `Surface(onClick)` ile tıklanabilir yapıldı: nabız çipi → `Routes.WRAPPED_REPORT` (dijital yaşam raporu), görev çipi → `Routes.MISSIONS`. `onPulseClick/onMissionClick` opsiyonel parametreler — null ise çip pasif kalır (geriye uyumlu).

### B-04 (P2) — Açıklamasız spinner ✅ DÜZELTİLDİ

**Bulgu:** İlk yüklemede yalnız spinner görünüyordu; kullanıcı neyin yüklendiğini bilmiyordu.

**Düzeltme:** Spinner altına "Klasörler hazırlanıyor…" etiketi eklendi.

### B-05 (P2) — Hızlı başlat eşiği vs çekmece swipe'ı 📝 TASARIM NOTU

**Bulgu:** Klasör satırında hızlı yukarı kaydırma eşiği satır yüksekliğinin %35'i (~31dp); çekmece swipe eşiği 72dp. Satır üzerinde 31dp'yi aşan her yukarı kaydırma hızlı başlat tetikler — satır üzerinden çekmeceye swipe ulaşılamaz (kullanıcı satır araları/üst/alt boşluk, çift tık veya "Tüm uygulamalar" ipucunu kullanır).

**Değerlendirme:** Kasıtlı tasarım (hızlı başlat erişilebilir olmalı). Çekmecenin 4 alternatif girişi var. Eşik değiştirilmedi; cihaz testinde kullanıcı şaşkınlığı gözlenirse %50'ye yükseltilmesi önerilir.

### B-06 (P2) — Boş klasör sayfası yönlendirmesi 📝 TASARIM NOTU

**Bulgu:** Tüm klasörler boş/gizli ise klasör sayfası içerik göstermez (Hero + nabız şeridi kalır). Kurulu uygulama olmadan klasör oluşmaz.

**Değerlendirme:** Nadir durum (cihazda uygulama varsa klasör oluşur). Boş cihaz senaryosu için çekmece zaten boş durum mesajı gösteriyor. İyileştirme önerisi: klasör sayfası boşsa "Uygulamalarınız otomatik klasörlenecek" ipucu.

---

## Doğrulanan olumlu durumlar

| Alan | Durum |
|------|-------|
| Jest ayrışması | Satır başına TEK pointerInput; dokun/sürükle/hızlı başlat çakışmasız (birim + Robolectric testli) |
| Çift tık / uzun bas / swipe | Gesture arena fiziği doğru: tıklanabilir çocuklar kendi dokunuşunu üstlenir, kök jestleri boş alanda çalışır |
| Geri tuşu (klasör/başlık editörü) | FolderScreen + HomeLayoutEditor BackHandler'ları korunuyor |
| Erişilebilirlik | AppIconView `contentDescription = app.appName`; satır başlıkları okunabilir; çipler tıklanabilir |
| Boş arama durumu | "Sonuç yok" + web/Play fallback + dosya izni kısayolu mevcut |
| Boş bölümler | Çekmece bölümleri boşsa render edilmez (sessiz, doğru) |
| Yükleme durumu | Spinner + açıklayıcı etiket (bu denetimde eklendi) |
| Görsel taşma | Robolectric taşma dedektörü 6 konfigürasyonda yeşil (dar ekran, %150/%200 font ölçeği dahil) |

## Kalan öneriler (cihaz doğrulaması sonrası)

1. Cihazda OEM bazlı geri tuşu testi (MIUI/HyperOS, Samsung One UI, Pixel predictive back).
2. Hızlı başlat eşiğinin gerçek kullanımda gözlemi (B-05).
3. Klasör satırında sürükle-sıralama hissinin (hedef vurgu + haptic) cihazda doğrulanması.
4. Çekmece arama odaklama akışının klavye ile cihazda doğrulanması (focusSearchOnOpen).
