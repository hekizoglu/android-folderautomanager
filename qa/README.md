# QA Denetim Sistemi — Genel Bakış

> Bu klasör, AppOrganizer için çok katmanlı kalite denetim sistemini tanımlar.
> Amaç: Hatayı build’den önce veya PR aşamasında yakalamak.
> "Test sonrası öğrenme" yerine "önceden engelleme" stratejisi.

---

## Katmanlar

| Katman | Araç | Ne yakalar? | Puan |
|--------|------|-------------|------|
| Crash/ANR izleme | Firebase Crashlytics, Play Vitals | Kullanıcıda çökme, ANR, yavaş açılış | 10 |
| UI/Launcher akış testleri | Espresso, UI Automator, Maestro | Buton, home, drawer, arama, klasör, launcher akışı | 9 |
| Android Lint + statik analiz | Lint, detekt, ktlint, SonarQube | Null hatası, yanlış API, performans, güvenlik, kod kokusu | 9 |
| Görsel regresyon testi | Compose Screenshot, Roborazzi, Paparazzi | Tasarım bozulması, ikon kayması, tema/screen-size | 8 |
| Performans/startup testi | Macrobenchmark, Baseline Profiles | Startup süresi, scroll jank, RAM, pil | 8 |
| Güvenlik/izin analizi | MobSF, CodeQL, OWASP MASTG | Sızan izinler, riskli bağımlılık, APK güvenliği | 7 |
| AI kod inceleme | SonarQube for IDE, Copilot Review | Kalite, güvenlik, best practice ihlali | 6 |

---

## Build Kapısı (Build Gate)

Her APK üretilmeden önce şu komut geçmeli:
```bash
./gradlew clean check lintDebug testDebugUnitTest detekt ktlintCheck logicAuditFast
```

Eğer Compose kullanılıyorsa ekle:
```bash
./gradlew validateDebugScreenshotTest
```

---

## Döngü

- VS Code = kodlama + hızlı kontrol
- Gradle = otomatik denetim motoru
- Android Studio = profil, APK, crash, UI inceleme
- GitHub Actions = CI kalite kapısı
- local_denetim raporu = döngüsel sonuç takibi

## Logic Sentinel

- Hızlı tarama: `./gradlew logicAuditFast`
- Semantik tarama: `./gradlew logicAuditSemantic`
- Derin bölüm bazlı tarama: `./gradlew logicAuditDeep`
- Kural seti: `qa/logic-rules.md`
- Rapor şeması: `qa/report-schema.md`

---

*Oluşturma: 2026-06-27 | Katman: QA denetim sistemi*
