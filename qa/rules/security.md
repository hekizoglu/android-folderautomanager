# Güvenlik ve İzin Analizi

> Kaynak: [OWASP MASTG](https://mas.owasp.org/MASTG/), [MobSF](https://github.com/mobsf/mobile-security-framework-mobsf)

## Kontrol Listesi

| Alan | Kontrol | Öncelik |
|------|---------|---------|
| QUERY_ALL_PACKAGES | Gerekçe yoksa kullanılmamalı | P0 |
| POST_NOTIFICATIONS | Açıklama ve fallback var mı? | P1 |
| PACKAGE_USAGE_STATS | UsageStats fallback ve izin akışı | P1 |
| READ_CONTACTS | Kişi araması için izin | P2 (roadmap) |
| Hardcoded API key | Kod/JSON içinde sabit anahtar yok | P0 |
| DataStore güvenliği | Hassas veri şifreli mi? | P1 |
| Backup/restore | JSON içinde gizli veri sızması | P1 |
| Bağımlılık | Sürüm sabitlenmiş mi? checksum doğrulama | P2 |
| Probar | Kod/veri sızması var mı? | P1 |
| Log | `Timber` loglarda hassas veri var mı? | P2 |

---

## Araçlar

- MobSF: APK statik/dinamik güvenlik analizi
- CodeQL: Java/Kotlin güvenlik açıkları
- Gradle Dependency Verification: Bağımlılık bütünlüğü
- Play policy kontrolü: QUERY_ALL_PACKAGES beyanı

---

## Komutlar

```bash
# Bağımlılık imza doğrulama
./gradlew dependencies --configuration debugRuntimeClasspath

# APK güvenlik taraması
mobsfscan app/build/outputs/apk/debug/app-debug.apk
```

---

*Kural seti: qa/rules/security.md | Puan: 7/10*
