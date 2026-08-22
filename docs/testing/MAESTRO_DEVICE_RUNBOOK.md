# Maestro Cihaz Doğrulama Runbook (Home V2)

Bu runbook, Home V2 ana ekranının **gerçek cihazda/emülatörde** uçtan uca ve görsel
doğrulaması için adım adım talimat verir. Otomatik katman (Robolectric görsel testleri)
CI'da zaten koşar; bu akış cihaz-özgün sorunları (wallpaper, OEM davranışı, gerçek
dokunma jestleri, animasyon zamanlaması) yakalamak içindir.

## 1. Ön koşullar

```bash
# Maestro kurulumu
curl -Ls "https://get.maestro.mobile.dev" | bash        # macOS/Linux
scoop install maestro                                    # Windows

# adb'nin cihazı gördüğünden emin olun
adb devices

# APK'yi derleyip kurun
./gradlew assembleDebug -PskipGoogleServices             # Linux/macOS
.\gradlew assembleDebug -PskipGoogleServices             # Windows PowerShell
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Uygulamayı varsayılan launcher yapın (cihazda elle veya):
adb shell cmd package set-home-activity com.armutlu.apporganizer/.presentation.ui.launcher.LauncherActivity
```

## 2. Akışları çalıştırma

```bash
# Home V2 görsel kanıt akışı (öncelikli)
maestro test .maestro/07_home_v2_visual_check.yaml

# Tam regresyon (7 akış)
maestro test .maestro/

# Belirli cihaz/emülatör ile
maestro test --device <serial> .maestro/07_home_v2_visual_check.yaml
```

Sonuçlar ve ekran görüntüleri: `~/.maestro/tests/<run-id>/`
(Görseller: `homev2_01_home_first_paint` … `homev2_06_final_state`)

## 3. Görsel inceleme kontrol listesi

| Görsel | Kontrol |
|--------|---------|
| `homev2_01` | İlk açılış: saat, nabız şeridi, klasör kartları, dock görünüyor mu? Hiçbir öğe ekran dışına taşıyor mu? |
| `homev2_02` | Çekmece: arama çubuğu, A-Z sidebar ve liste ekrana sığıyor mu? |
| `homev2_03` | Çekmece kapanınca ana ekran eksiksiz geri geliyor mu (dock dahil)? |
| `homev2_04` | Uzun basma menüsü: tüm satırlar görünüyor mu, taşma yok mu? |
| `homev2_05` | Header bölgesi: saat metni büyük fontta taşıyor mu, çipler hizalı mı? |
| `homev2_06` | Genel son durum: duvar kağıdı/arka plan seçimi doğru mu? |

Ek senaryolar (elle):
- Cihaz font ölçeğini **%150 ve %200** yapıp akışı tekrar koşun (erişilebilirlik taşma testi).
- Cihazı **yatay** çevirip ana ekranı açın; grid'in adaptif sütun sayısı artmalı.
- **Koyu/açık tema** değiştirip kart kontrastlarını kontrol edin.
- Duvar kağıdı seçiliyken dock ve kartların okunurluğunu kontrol edin.

## 4. Jest doğrulaması (elle, akış kapsamı dışındaki jestler)

| Jest | Beklenen |
|------|----------|
| Klasör kartında hızlı yukarı kaydırma | Klasörün en sık kullanılan uygulaması başlar |
| Klasör kartına uzun bas + sürükle | Kart lifted görünür, hedef kart vurgulanır, bırakınca sıra kalıcı değişir |
| Dock/önizleme ikonuna uzun basma | Bağlam menüsü açılır (başlat/dock/kategori/gizle/not/favori) |
| Boş alanda yukarı kaydırma | Uygulama çekmecesi açılır |
| Boş alana uzun basma | Yönetim menüsü açılır |
| Ana ekranda sola/sağa kaydırma | Hero → Widget → Klasör sayfaları geçişi |

## 5. Sorun bulunduğunda izlenecek yol

1. Sorunu ekran görüntüsü + cihaz/OS/font ölçeği bilgisiyle not edin.
2. Mümkünse Robolectric tarafında reproduce edin (`HomeV2VisualUiTest`'e yeni test):
   - Taşma sorunları → `assertNoOverflow` dedektörüyle yakalanır.
   - Jest sorunları → `performTouchInput` desenleriyle (tur 9 notları: olay zamanları
     blok içinde +16ms artar; uzun basış bölünmüş çağrılarla simüle edilir).
3. Düzeltmeyi küçük ve testli yapın; `testDebugUnitTest` yeşil olmadan push etmeyin.
4. Cihazda akışı tekrar koşup görseli karşılaştırın.

## 6. CI entegrasyonu notu

Maestro akışları cihaz gerektirdiğinden GitHub Actions'a otomatik bağlanmamıştır.
Önerilen: release öncesi kapı olarak fiziksel cihazda `maestro test .maestro/`
çıktısının `qa/reports/` altına kanıt olarak eklenmesi (mevcut build-gate pratiğiyle uyumlu).
