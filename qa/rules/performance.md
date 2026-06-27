# Performans ve Startup Testleri

> Kaynak: [Android Developers — Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview), [Baseline Profiles](https://developer.android.com/topic/performance/baselineprofiles/create-baselineprofile)

## Hedef

- Cold startup: mümkün olduğunca düşük
- Home’a dönüş süresi: çok hızlı
- App drawer açılışı: takılmasız
- Scroll jank: minimum
- RAM kullanımı: düşük
- Pil tüketimi: agresif değil
- ANR: sıfıra yakın

---

## Macrobenchmark Örnek Test

```kotlin
@Test
fun coldStartup() = benchmarkRule.measureRepeated(
    packageName = "com.armutlu.apporganizer",
    metrics = listOf(StartupTimingMetric()),
    iterations = 10,
    startupMode = StartupMode.COLD
) {
    pressHome()
    startActivityAndWait()
}
```

## Hedef Metrikler

| Metrik | Hedef |
|--------|-------|
| Cold startup | < 1s |
| Home → drawer | < 200ms |
| Scroll jank | < 5% |
| RAM (5 dk sonra) | < 180MB |
| Pil (arika plan) | agresif değil |

---

## Düzenli Kontrol Listesi

- `PackageManager` sorguları her recomposition’da tekrar tekrar yapılıyor mu?
- `Flow.collect` ana thread’de mi?
- `remember` / `derivedStateOf` / `produceState` kullanımı doğru mu?
- `LazyColumn` / `LazyRow` için `key` parametresi var mı?
- Bellek sızıntısı: `DisposableEffect`, `Flow.collect`, listener kaydı

---

*Kural seti: qa/rules/performance.md | Puan: 8/10*
