# UI ve Launcher Akış Testleri

> Kaynak: [Android Developers — Espresso](https://developer.android.com/training/testing/espresso), [UI Automator](https://developer.android.com/training/testing/other-components/ui-automator), [Maestro](https://docs.maestro.dev/get-started/supported-platform/android)

## Mutlaka Test Edilecek Senaryolar

| Alan | Test | Öncelik |
|------|------|---------|
| Varsayılan launcher | Kullanıcı uygulamayı varsayılan launcher yapabiliyor mu? | P0 |
| Home tuşu | Home'a basınca launcher açılıyor mu? | P0 |
| Back tuşu | Back ile boş ekrana düşüyor mu? | P0 |
| App drawer | Tüm uygulamalar doğru listeleniyor mu? | P0 |
| Android 11+ görünürlük | Yüklü uygulamaları listeleme kısıtına takılıyor mu? | P1 |
| İkonlar | Uygulama ikonları eksik, bulanık veya yanlış mı? | P1 |
| Arama | Uygulama arama doğru sonuç veriyor mu? | P0 |
| Klasörler | Klasör oluşturma/silme/taşıma bozuluyor mu? | P1 |
| Widget | Widget ekleme, silme, yeniden başlatma sonrası korunuyor mu? | P1 |
| Ekran döndürme | State kaybı oluyor mu? | P2 |
| Tema | Açık/koyu tema bozuluyor mu? | P2 |
| Font büyütme | Büyük yazı boyutunda tasarım taşıyor mu? | P2 |
| Pil/performans | Launcher arka planda gereksiz çalışıyor mu? | P2 |
| İlk kurulum | Kullanıcı ne yapacağını anlıyor mu? | P1 |

---

## Test Araçları Karar.Matrix

| Senaryo | Espresso | UI Automator | Maestro |
|---------|----------|--------------|---------|
| Uygulama içi buton/liste | ✅ | | ✅ |
| Home tuşu, sistem dialog | | ✅ | ✅ |
| Varsayılan launcher seçimi | | ✅ | ✅ |
| Hızlı E2E akış (YAML) | | | ✅ |
| Crash/ANR simülasyonu | | ✅ | |

---

## Komutlar

```bash
# Unit test
./gradlew testDebugUnitTest

# Instrumented UI test
./gradlew connectedDebugAndroidTest

# Maestro (yüklenmiş olmalı)
maestro test qa/checklists/launcher-maestro.yaml
```

---

*Kural seti: qa/rules/ui-tests.md | Puan: 9/10*
