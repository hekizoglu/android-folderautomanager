# AppOrganizer — Dashboard, Global Arama ve Klasör Sayfaları Teknik Roadmap'i

> **Oluşturma tarihi:** 2026-07-17
> **Kapsam:** Launcher ana ekranının; tüm sayfalarda sabit **Her Şeyi Ara**, sabit dock, birinci sayfada Akıllı Ana Ekran/Dashboard ve devamındaki sayfalarda klasörler olacak biçimde yeniden kurulması.
> **Durum:** Tamamlanan 26 döngü (P00-P19, P22, P23) HISTORY.md arşivine taşındı (2026-07-19, bkz. "ARSIV: ANA_EKRAN_DASHBOARD_..." bloğu). Bu dosyada yalnız açık iş (P20, P21, P24, P25 — hepsi 🟡) kaldı.

Durum değerleri: `⏳ Bekliyor` · `🚧 Devam ediyor` · `🟡 Kısmen tamamlandı` (kod+otomatik testler var, gerçek cihaz/son kabul kanıtı eksik) · `✅ Tamamlandı` · `⛔ Bloke`.

Tamamlanan döngü satırı biçimi:

```text
**Durum:** ✅ Tamamlandı — Döngü PXX — commit: <SHA> — tarih: YYYY-MM-DD
```

---

# Açık döngüler (Faz K — Telemetri/sağlık/performans, Faz L — Rollout/temizlik)

## Döngü P20 — Telefon ve tablet adaptif düzen

**Amaç:** Dashboard ve klasör sayfalarının farklı ekranlarda uygun yoğunlukta çalışması.

**Telefon:**

- Tek Dashboard sayfası.
- 4 sütun klasör grid.
- All Apps tam ekran overlay.
- Search üst/alt tercihli.

**Küçük tablet:**

- Dashboard kartları iki kolonlu layout kullanabilir.
- 5 sütun klasör grid.
- All Apps sağ panel.

**Büyük tablet:**

- Dashboard `TwoPaneDashboardLayout` kullanabilir.
- 6 sütun klasör grid.
- Global search maksimum genişlik alır; tüm ekranı gereksiz uzatmaz.
- Dock maksimum genişlikle ortalanır.

**Test cihaz matrisi:**

- Küçük Android telefon.
- Standart telefon.
- Büyük telefon.
- 7–8 inç tablet.
- 10+ inç tablet.
- Portrait ve landscape.

**Kabul kriterleri:** Hiçbir cihazda arama/dock kaybolmaz; Dashboard ile klasör grid'i kırpılmaz.

**Bağımlılıklar:** P06, P19 (✅, bkz. HISTORY.md arşivi).

**Durum:** 🟡 Kısmen tamamlandı — Döngü P20 — tarih: 2026-07-18 — Not: HomeAdaptiveLayoutPolicy eklendi; 600/840dp kırılımları tek kaynağa alındı; HomeScreen kolon, tablet side panel, global arama ve dock max-width kararlarını bu politikadan türetiyor. `:app:compileDebugKotlin` yeşil. Odaklı test yeşil: `:app:testDebugUnitTest --tests HomeAdaptiveLayoutPolicyTest --tests HomeScreenNavigationContractTest`. FAZ A-1 (2026-07-19, cihaz: R92Y200CBKX Samsung SM-X210 tablet, Android 15): portrait ve landscape ekran görüntüleri kırpma/taşma olmadan doğrulandı (Dashboard kartları, 6 sütun klasör grid, dock, arama çubuğu hepsi düzgün render), ANCAK rotasyon değişimi hemen ardından folder-sayfası swipe'ı yapıldığında gerçek cihazda crash bulundu: `java.lang.IllegalArgumentException: measure is called on a deactivated node` (LazyGrid, androidx.compose.foundation.lazy.grid) — activity otomatik yeniden başladı, veri kaybı olmadı, uygulama içi Hata Raporları'na da düştü. Bu crash EX03'te (2026-07-19, Compose BOM 2024.09.03→2024.12.01) ayrı olarak düzeltildi, bkz. HISTORY.md. Kalan: fix sonrası kalan 3 cihazda (telefon, temiz kurulum, izin-kapalı) doğrulama.

---

## Döngü P21 — Anonim sayfa kullanım telemetrisi

**Amaç:** Yeni mimarinin gerçekten kullanılıp kullanılmadığını kişisel veri toplamadan ölçmek.

**Event'ler:**

```text
home_page_viewed
home_page_swiped
home_start_mode_changed
smart_dashboard_toggled
home_search_opened
all_apps_opened_from_page
home_button_navigation
```

**Güvenli parametreler:**

```text
page_type = dashboard | folder
page_position_bucket = first | middle | last
navigation_source = swipe | indicator | home_button | restore
search_position = top | bottom
start_mode = dashboard | first_folder | last_visited
device_class = phone | compact_tablet | expanded_tablet
```

**Gönderilmeyecekler:** Klasör adı, kategori adı/id, uygulama adı/paket adı, search query, kişi veya dosya bilgisi.

**Kabul kriterleri:** Dashboard benimsenmesi ve klasör erişim etkisi anonim olarak ölçülebilir.

**Bağımlılıklar:** Telemetri roadmap'inin merkezi manager döngüsü, P05 (✅).

**Durum:** 🟡 Kısmen tamamlandı — Döngü P21 — tarih: 2026-07-18 — Not: Privacy-safe home page telemetry event katalogu eklendi (`home_page_viewed`, `home_page_swiped`, start/toggle/search/drawer/home-button event şemaları); HomePageTelemetryPolicy saf mapper eklendi; HomeScreen settled page impression'ı `RESTORE`, sonraki page değişimlerini `SWIPE` olarak logluyor. Global search, All Apps drawer açılışları, home button navigation, başlangıç modu değişimi ve Smart Dashboard toggle call-site'ları privacy-safe parametrelerle bağlandı. Odaklı test yeşil: `:app:testDebugUnitTest --tests TelemetryEventValidatorTest --tests HomePageTelemetryEventValidatorTest --tests HomePageTelemetryPolicyTest`. `scripts/clear_test_locks.ps1` ile Windows generated test kilitleri temizlenebilir. FAZ A-1 (2026-07-19, cihaz: R92Y200CBKX): telemetry consent kararı verilmemiş halde (fail-closed, `isTelemetryEnabled=false` varsayılan) ana ekran gezinme (dashboard swipe, klasör aç, global arama, home button) yapıldı; tam logcat taraması (2210 satır) `home_page_viewed/swiped`, `logEvent`, `setUserProperty` veya Firebase `FA` tag aktivitesi göstermedi — consent-off fail-closed davranışı cihazda kanıtlandı. Kalan: Firebase DebugView/remote konsolunda no-op teyidi (bu adb-only turun kapsamı dışında, ayrı Firebase erişimi gerektirir).

---

## Döngü P24 — Feature flag ile kontrollü geçiş

**Amaç:** Büyük ana ekran değişikliğini geri dönüş yolu olmadan tüm kullanıcılara açmamak.

**Kabul kriterleri:** Yeni ana ekran sorun çıkarırsa kullanıcı verisi silmeden eski görünüme dönülebilir.

**Bağımlılıklar:** P03–P23 (P20/P21/P23 hariç ✅, bkz. yukarı ve HISTORY.md arşivi).

**Durum:** 🟡 Kısmen tamamlandı — Döngü P24 — tarih: 2026-07-18 — Not: `AppPrefs` içine kapalı varsayılan `KEY_HOME_PAGER_V2_ENABLED` ve `KEY_HOME_PAGER_V2_SAFE_MODE` eklendi; HomeScreen flag'i reaktif dinliyor; Debug/Geliştirici bölümüne demo toggle'ları eklendi; sağlık raporu ve AI tanı paketine rollout/safe-mode durumu eklendi. Politika testi ve Kotlin derlemesi yeşil. FAZ A-1 (2026-07-19, cihaz: R92Y200CBKX) tek cihazda tam senaryo kanıtlandı: pager v2 OFF → HOME legacy folder-grid'e (Dashboard yok, sayfa noktası yok) reaktif geçti, crashsiz; ON → Dashboard geri geldi; pager v2 ON + safe mode ON → HOME yine legacy görünüme düştü ve ayar satırı "Safe mode aktif; eski davranış korunuyor" olarak güncellendi, crashsiz; her geçişte logcat FATAL taraması temiz. Kalan: kalan 3 cihazda (telefon, temiz kurulum, izin-kapalı) aynı senaryonun doğrulanması; fallback davranışı P25'e kadar korunacak.

---

## Döngü P25 — Eski folder-only pager ve legacy kod temizliği

**Amaç:** Yeni yol doğrulandıktan sonra iki paralel ana ekran taşımamak.

**Silinecek/deprecate edilecek adaylar:**

- Eski `FolderPager` pager sahibi API'si.
- `AppPrefs.getLastHomePage/setLastHomePage` ham index kullanımı.
- `HomeScreen.kt` içindeki eski folder-only pager bloğu.
- Legacy `FOLDER_GRID` layout section kaydı.
- Kullanılmayan focus-mode özel branch'leri.
- Duplicate searchBarSection lambda.
- Eski page indicator.

**Nasıl yapılmalı:**

1. Repo-wide usage search yap.
2. Backup migration en az bir sürüm çalıştıysa legacy okuma yalnız restore uyumluluğu için tutulur.
3. Test fixture'ları yeni modellere taşınır.
4. Detekt baseline'daki eski HomeScreen complexity maddeleri gözden geçirilir.
5. `HomeScreen.kt` hedef olarak orchestration dosyası seviyesine indirilir.
6. HISTORY.md'ye migration ve kullanıcı etkisi yazılır.
7. Roadmap durumları commitlerle güncellenir.

**Kabul kriterleri:** Üretimde yalnız tek ana page mimarisi bulunur; eski kod yolu aktif değildir.

**Bağımlılıklar:** P24 gerçek cihaz doğrulaması (🟡, yukarıda).

**Durum:** 🟡 Kısmen tamamlandı — Döngü P25 — tarih: 2026-07-18 — Not: Repo-wide kullanım araması yapıldı. `HomeScreenFolderPager.kt` artık eski pager değil, tek sayfa grid renderer; `HomeScreenPageIndicator.kt` yeni indicator; `FOLDER_GRID` layout modelinde hâlâ gerekli. Runtime'da semantic anchor yazılırken ham `last_home_page` indeksini tekrar yazan legacy senkron kaldırıldı; eski anahtar yalnız migration/restore uyumluluğu için tutuluyor. FAZ A-1 (2026-07-19, cihaz: R92Y200CBKX) genel smoke testi crashsiz geçti: ana ekran, All Apps drawer (tablette yan panel olarak render), global arama ("meet" sorgusu doğru sonuç döndürdü), klasör açma hepsi sorunsuz; `last_home_page` anahtarının hâlâ sadece migration/restore için tutulduğu kod tarafında teyit edildi (yeni kullanım yok). Kalan: dört cihaz kanıtı tamamlanmadan (bu turda sadece 1/4 cihaz test edildi) diğer legacy API ve test fixture temizliği başlatılamaz.

---

# Net mimari karar özeti

```text
Her Şeyi Ara = Global ve sabit
Dock = Global ve sabit
Dashboard = Pager sayfa 0
Klasörler = Pager sayfa 1..N
Uygulama çekmecesi = Her sayfada swipe-up
Yatay pager = Yalnız bir tane
Home tek basış = Başlangıç sayfası
Home çift basış = Uygulama çekmecesi
Sayfa restore = Ham index değil semantic anchor
Dashboard bölümleri = HomeLayout CONTENT zone
Search konumu = Global HEADER veya FOOTER
```

Bu yapı AppOrganizer'ı üç ayrı erişim yöntemini birlikte kullanan bir launcher'a dönüştürür:

1. **Biliyorsan ara:** Her Şeyi Ara.
2. **Kategorisini biliyorsan klasörden aç:** Klasör sayfaları.
3. **Listeyi görmek istiyorsan yukarı kaydır:** Uygulama çekmecesi.

Dashboard uygulamalara erişimi engelleyen bir ara ekran değil; arama, dock ve çekmece sürekli erişilebilirken kullanıcının günlük durumunu gösteren ilk sayfadır.

---

> **Son güncelleme tarihi:** 2026-07-19 — 26 tamamlanan döngü HISTORY.md arşivine taşındı; yalnız P20/P21/P24/P25 (hepsi 🟡) açık kaldı.
