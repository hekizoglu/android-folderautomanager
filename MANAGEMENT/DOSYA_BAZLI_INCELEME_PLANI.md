# Dosya Bazlı İnceleme Planı

**Başlangıç:** 2026-08-21  
**Dal:** `arena/01a022ef-android-folderautomanager`

## Çalışma kuralı

Her dosya veya birbirinden ayrılmayan küçük dosya grubu için:

- çağıranlar ve bağımlılıklar incelenir,
- bug/performance/security/lifecycle riski not edilir,
- mümkünse regression testi eklenir,
- tek amaçlı commit oluşturulur,
- build ortamı hazır olduğunda hedefli test çalıştırılır.

## Sıra

1. `domain/usecase/classify` — sınıflandırma ve öneri motoru
2. `data/local` — DAO, Room migration, indeks ve transaction
3. `data/repository` — hata sözleşmesi ve yan etkiler
4. `domain` — görev, bildirim, arama, öneri ve pulse use-case'leri
5. `presentation/viewmodel` — state/lifecycle/thread sınırları
6. `presentation/ui/launcher` — Home, pager, folder, dock, All Apps
7. `presentation/ui/screens` — ayarlar, onboarding, raporlar
8. `service`, `workers`, `receivers` — arka plan ve paket/notification event'leri
9. `utils`, `telemetry`, `di` — izin, gizlilik, cache ve dependency wiring
10. migration/instrumentation/release doğrulaması

## Durum

| Alan | Durum | Not |
|---|---|---|
| Paket lifecycle / repository yazma | ✅ | Commit `ef1d3ea` |
| `AppClassifier` vendor prefix sınırı | ✅ | Commit `130b46b` |
| `CategorySuggestionEngine` keyword sırası | ✅ | Commit `d243fa7` |
| `CategorySuggestionEngine` vendor prefix sınırı | ✅ | Commit `eb5549a` |
| `AppClassifier` locale determinismi | ✅ kod | Hedefli test/build bekliyor |
| `AppClassifier` tamamı | ⏳ | Sonraki inceleme paketi |
| Room DAO/migration | %85 | `operations` veri koruması + destructive fallback düzeltildi; cihaz doğrulaması bekliyor |
| Repository tamamı | ⏳ | Hata sözleşmesi taraması |
| Launcher/ViewModel | ⏳ | Büyük refactor öncesi analiz |
| UI/screens | ⏳ | Cihaz matrisi ile doğrulanacak |
| Worker/service/release | ⏳ | Son entegrasyon kapısı |

## Build engeli

Bu çalışma ortamında JDK 17 bulunmadığı için Gradle testleri henüz çalıştırılamıyor. JDK sağlandığında her dosya grubunun hedefli testi, ardından faz kapısında `testDebugUnitTest`, `lintDebug`, `detekt` ve `assembleDebug` çalıştırılacak.
