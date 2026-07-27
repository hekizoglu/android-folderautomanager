# Antigravity — Bağımsız Doğrulama: Pil/UsageStats Fix + Adaptif Hedefler Roadmap

> Bu dosyayı Antigravity'nin agent/chat penceresine yapıştır. Amaç: Claude Code oturumunun
> yaptığı iki iş kolunu (pil optimizasyonu + Akıllı Dijital Görevler/Adaptif Kategori Hedefleri
> roadmap'i) **bağımsız bir ikinci göz** olarak gözden geçirmek. Bu bir kod TARAMASI değil,
> **doğrulama/denetim** görevidir — amaç Claude'un raporladığı sonuçların gerçekten koda
> yansıdığını, testlerin gerçekten geçtiğini ve iddia edilen davranışın doğru olduğunu bağımsız
> kanıtla teyit etmek (D240 "halüsinasyon denetimi" disiplini — bkz. CLAUDE.md/LEARNINGS.md).

---

## Bağlam

Proje: `c:\Users\hekizoglu\Documents\AppOrganizer` (Android/Kotlin, Jetpack Compose launcher).

Son commit'ler (`git log --oneline -10` ile teyit et):
- Pil/UsageStats throttle değişikliği: `app/src/main/java/com/armutlu/apporganizer/utils/AppPrefs.kt`
  içinde `USAGE_SYNC_INTERVAL_MS` sabiti 30 dakikadan 2 saate çıkarıldı (henüz commit
  edilmemiş olabilir — `git status` ile kontrol et, uncommitted ise önce onu da incele).
- Ana ekran kart genişliği fix'i: `HomeMissionCard.kt`, `TodayCard.kt` (`GlassCard` çağrılarına
  `fillMaxWidth()` eklendi).
- Büyük roadmap turu: "Akıllı Dijital Görevler + Adaptif Kategori Hedefleri + Tavsiye Kartı" —
  `MANAGEMENT/AKILLI_DIJITAL_GOREVLER_ADAPTIF_KATEGORI_HEDEFLERI_ROADMAP.md` dosyasını oku, bu
  turda ne yapıldığının tam listesi orada (P0-P11 + P7b).

## Görev

Aşağıdaki 4 maddeyi bağımsız olarak doğrula. Her madde için: kodu oku, gerekiyorsa build/test
çalıştır, "doğrulandı" veya "sorun bulundu" de — Claude'un raporuna GÜVENME, kendi başına
kanıtla.

### 1. Pil/UsageStats throttle fix'i gerçekten doğru mu?

- `AppPrefs.kt` içinde `shouldSyncUsageStats`/`markUsageStatsSynced`/`USAGE_SYNC_INTERVAL_MS`
  fonksiyonlarını bul, gerçekten 2 saate (7_200_000 ms) ayarlandığını doğrula.
- `LauncherActivity.kt` içinde bu throttle'ın hem `onCreate` (satır ~209, APP_START, throttle'a
  tabi DEĞİL — her açılışta çalışmalı) hem `onResume` (satır ~324-339, throttle'lı) çağrı
  noktalarını bul, mantığın (`if (AppPrefs.shouldSyncUsageStats(this))`) hâlâ doğru sırada
  olduğunu teyit et.
- **Kritik soru:** Bu değişiklik `UsageStatsHelper.kt`'deki `getUsageCounts(days=30)` /
  `getLastUsedTimes(days=90)` gibi pahalı sorguların gerçek çağrı sıklığını azaltıyor mu, yoksa
  başka bir çağrı yolu (örn. `MissionMetricSnapshotProvider`, `DigitalPulseRepository`,
  `RealSmartTickerSource`) hâlâ kendi bağımsız UsageStats sorgularını YAPMAYA DEVAM mı ediyor?
  Eğer öyleyse, bu throttle fix'i sadece BİR çağrı yolunu (dock/klasör sıralaması için kullanılan
  `syncUsageStats`) kapsıyor, TÜM pil sorununu çözmüyor olabilir — bunu açıkça belirt.
- Diğer periyodik/tekrarlayan UsageStats tüketicilerini grep ile bul (`UsageStatsHelper.`,
  `queryEvents`, `queryUsageStats`) ve her birinin kendi cache/throttle mekanizması olup
  olmadığını listele (`DigitalPulseRepository` 15 dk cache'i biliniyor, MissionMetricSnapshotProvider
  ve RealSmartTickerSource için de aynı soruyu sor).

### 2. Ana ekran kart genişliği fix'i regresyon yaratmış mı?

- `HeroDashboardPage.kt` içindeki tüm kart çağrılarını (`HeroClockCard`, `HeroDigitalLifeCard`,
  `HomeMissionCard`, `TodayCard`, `SmartAccessCard`) tara — hepsinin artık tutarlı şekilde
  `fillMaxWidth()` (ya doğrudan kendi composable'ında ya da dıştaki `Column`'dan miras) aldığını
  doğrula. Gözden kaçmış başka bir kart (örn. `PendingClassificationBadge`, ileride eklenecek
  yeni bir Hero bloğu) var mı kontrol et.
- `GlassCard.kt` composable'ının kendisinin hâlâ genişlik zorlamadığını (bilinçli tasarım) teyit
  et — bu, gelecekte yeni bir kart eklenirse aynı hatanın (dar/intrinsic genişlik) tekrar
  edebileceği anlamına gelir. Bunu bir LEARNINGS.md notu olarak öner (kod değiştirme, sadece not
  düş): "Yeni Hero kart composable'ı eklerken GlassCard çağrısına `fillMaxWidth()` eklemeyi
  unutma — GlassCard kendisi genişlik zorlamıyor."

### 3. Adaptif Kategori Hedefleri roadmap'i — rastgele 3 dosya derinlemesine incele

Roadmap dosyasında "TAMAMLANDI" denen P0-P11 maddelerinden **kendi seçtiğin 3 tanesini**
(hepsini değil, zaman/token tasarrufu için) derinlemesine kontrol et:
- Kod gerçekten var mı (dosya yolu roadmap'te belirtildiği gibi mi)?
- Testler gerçekten o davranışı test ediyor mu, yoksa yüzeysel/anlamsız assertion mı var?
- `AdaptiveCategoryTargetCalculator` seçersen: §16'daki 10 test senaryosunun matematiğini elle
  hesaplayıp (blend formülü + %15 klips + 5dk yuvarlama) testteki beklenen değerlerle gerçekten
  eşleştiğini doğrula.
- `WeeklyGoal` Room migration seçersen: `MIGRATION_24_25`'in gerçekten additive olduğunu (DROP/
  CREATE TABLE yok) ve `WeeklyGoalMigrationTest`'in gerçekten emülatörde geçtiğini iddia eden
  HISTORY.md notunu (varsa) kontrol et — mümkünse kendi emülatöründe de çalıştır.
- `DigitalAdviceEngine` seçersen: 8 öncelik seviyesinin gerçekten sırayla (ilk eşleşen döner)
  çalıştığını, %20+30dk çifte eşiğinin doğru uygulandığını kontrol et.

### 4. Genel tutarlılık — roadmap dosyasının kendisi doğru mu?

`MANAGEMENT/AKILLI_DIJITAL_GOREVLER_ADAPTIF_KATEGORI_HEDEFLERI_ROADMAP.md` dosyasının en
altındaki "Uygulama İlerleme Kaydı" bölümünü oku. Orada iddia edilen şeylerin (1353 test, 0 hata,
emülatör smoke test sonucu, WeeklyGoalMigrationTest'in gerçek cihazda geçtiği) `git log` ve
mevcut kod ile tutarlı olduğunu doğrula — tarih/commit hash tutarsızlığı, iddia edilen ama
gerçekte olmayan bir dosya/test var mı kontrol et.

## Rapor Formatı

Bulgularını `MANAGEMENT/ANTIGRAVITY_DOGRULAMA_RAPORU_<tarih>.md` adında yeni bir dosyaya yaz:

```
# Antigravity Bağımsız Doğrulama Raporu — <tarih>

## 1. Pil/UsageStats Fix
[doğrulandı / sorun bulundu + açıklama]

## 2. Kart Genişliği Fix
[doğrulandı / sorun bulundu + açıklama]

## 3. Roadmap Derin İnceleme (seçtiğin 3 madde)
[her madde için: doğrulandı / sorun bulundu + kanıt (dosya:satır, test adı)]

## 4. Roadmap Dosyası Tutarlılığı
[doğrulandı / tutarsızlık bulundu + açıklama]

## Genel Sonuç
[kritik bulgu var mı, yoksa "tüm incelenen maddeler doğrulandı" mı]
```

Sonra `HISTORY.md`'nin en üstüne 3 satırlık özet ekle (`**Yapılanlar:**`/`**Bug:**`/`**Sonraki:**`).
Kod DEĞİŞTİRME (bu bir doğrulama görevi, düzeltme değil) — eğer kritik bir hata bulursan onu
raporda net şekilde işaretle, ama düzeltmeyi Claude'un yapması için bırak, kendin fix yazma.
Commit atma — sadece rapor dosyasını ve HISTORY.md'yi commit+push et (mesajda çift tırnak
kullanma, sonu `Co-Authored-By: Antigravity <noreply@antigravity.google>`).

## Sert Kısıtlar

- Kendi başına yeni alt-agent/alt-görev spawn etme.
- Build/test iddia etmeden önce gerçekten çalıştır — çıktısını rapora yapıştır (özet yeterli,
  tam log gerekmez).
- Çözemediğin/anlayamadığın bir kısım varsa raporda açıkça "incelenemedi, sebep: X" yaz —
  uydurma veya atlama.
- CLAUDE.md ve MANAGEMENT/LEARNINGS.md dosyalarını oku, özellikle D240 (halüsinasyon denetimi)
  bölümünü — bu görevin ruhu tam olarak o disiplin.
