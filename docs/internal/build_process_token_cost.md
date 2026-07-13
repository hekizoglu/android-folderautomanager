# Build, Surec ve Token Maliyeti

> Son guncelleme: 2026-07-13

Bu not, `ROADMAP.md` altindaki "Build, Surec ve Token Maliyeti" maddesini tek yerde operasyonel hale getirir.

## 1. Build rutini

- Hedef debug build: `.\build.ps1`
- Temiz tekrar denemeli debug build: `.\build.ps1 -Clean`
- Release bundle: `.\build.ps1 -Release`
- Profil/benchmark: `.\scripts\benchmark_build.ps1`

Notlar:
- `build.ps1` ve `benchmark_build.ps1` ayni global mutex'i kullanir: `Global\AppOrganizerGradleBuild`.
- Configuration cache kalici olarak acik degil; sadece CLI benchmark akisi icin denenir.
- `benchmark_build.ps1` sonucu `docs/internal/build_benchmark_latest.md` altina yazilir.
- 2026-07-13 son benchmark: profile assembleDebug rerun 211.1s, configuration-cache compileDebugKotlin 5.5s, ikisi de exit 0.

## 2. Surec rutini

- Tam dongu orkestrasyonu: `.\cycle.ps1 "mesaj"`
- Denetim: `.\scripts\audit.ps1`
- Zaman/token logu: `.\scripts\log_cycle_time.ps1`

Beklenen siralama:
1. Kod degisikligi
2. `.\build.ps1`
3. Gerekirse `.\scripts\benchmark_build.ps1`
4. `.\scripts\audit.ps1`
5. Dongu sonunda `.\scripts\log_cycle_time.ps1`

## 3. Token maliyeti yaklasimi

Bu repoda iki farkli maliyet sinifi var:

- Yerel/isletim maliyeti: Gradle build, test, audit suresi
- Harici API maliyeti: ozellikle DeepSeek LLM fallback ve gelecekte eklenecek model cagrilari

Prensipler:
- Varsayilan olarak en kisa yeterli prompt kullan.
- Ayni paket icin tekrar LLM cagrisi yapmamak adina `AppPrefs` icindeki kalici cache'i kullan.
- Build/audit raporunu gereksiz log satirlariyla sisirme.
- Gercek faturalama gerektiren entegrasyonlarda fiyatlari koda gommek yerine dokumanda tarihli referansla tut.

## 4. 2026-07-12 referans kaynaklari

- Android `NotificationListenerService` / bildirim modeli: `developer.android.com`
- WorkManager unique periodic work ve schedule yonetimi: `developer.android.com`
- OpenAI API fiyatlari: `https://openai.com/api/pricing/`
- Ayrintili token fiyat dokumu: `https://developers.openai.com/api/docs/pricing`
- Firebase Cloud Messaging fiyatlandirma: `https://firebase.google.com/products/cloud-messaging` (FCM ucretsiz)

## 5. Karar ozeti

- Smart insight ve benzeri kullaniciya gorunen isler, izin yoksa `retry` yerine sessiz `success` ile cikmali.
- Build performans kararlari benchmark raporuyla verilmelidir; tek seferlik hissi gozlem yeterli degildir.
- Token maliyeti notlari kategori bazli (`dusuk/orta/yuksek`) loglanmali, fiyatlar ise tarihli referansla dokumante edilmelidir.
