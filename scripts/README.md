# scripts/ - AppOrganizer Yardimci Araclar

| Script | Kullanim | Aciklama |
|--------|----------|----------|
| `fix_encoding.py` | `python scripts/fix_encoding.py <dosya>` | Curly quote, mojibake ve Turkce double-encode duzeltir. `.bak` yedek alir. |
| `fix_denetim_encoding.ps1` | `.\scripts\fix_denetim_encoding.ps1` | Local denetim markdown dosyalarindaki encoding bozulmalarini toplu duzeltir. |
| `check_duplicates.py` | `python scripts/check_duplicates.py assets/app_categories.json` | JSON duplicate key tespiti yapar; pre-commit hook tarafindan calistirilir. |
| `dedup_classifier.py` | `python scripts/dedup_classifier.py` | `app_categories.json` duplicate entry temizligi icin kullanilir. |
| `export_classifier_json.py` | `python scripts/export_classifier_json.py` | AppClassifier Kotlin kodundan JSON export eder. |
| `strip_exact_map.py` | `python scripts/strip_exact_map.py` | Kotlin haritasindan exact-match satirlarini kaldirir. |
| `local_ai.py` | `python scripts/local_ai.py "soru"` | Lokal AI gateway uzerinden model sorgusu yapar. |
| `audit.ps1` | `.\scripts\audit.ps1` | Odak rotasyonlu statik kod denetimi uretir. |
| `version_bump.ps1` | `.\scripts\version_bump.ps1 patch` | `versionCode` ve `versionName` artirir. |
| `score_docs_backlog.ps1` | `.\scripts\score_docs_backlog.ps1 [-UpdateRoadmap] [-WriteReport]` | Script icindeki aday listesini puanlar; varsayilan davranis `ROADMAP.md` dosyasini tek aktif kaynak tutmaktir. `-WriteReport` verilirse ayrica `docs/internal/docs_backlog_score.md` snapshot'i uretir. |
| `create_release_keystore.ps1` | `.\scripts\create_release_keystore.ps1` | Release imzalama icin yerel `release.jks` ve gitignore kapsamindaki `keystore.properties` dosyasini interaktif sifreyle uretir. |

## CS-3 Build Kilidi Araclari

| Script | Ne zaman kullan | Admin gerekir mi | Ne yapar |
|--------|-----------------|-------------------|----------|
| `add_defender_exclusion.ps1` | Kalici cozum; build kilidi tekrar tekrar olusuyorsa bir kez calistir. | Evet, UAC self-elevation kullanir. | `app\build`, `.gradle`, `.android` klasorlerini Windows Defender real-time taramasindan dislar. `-CheckOnly` admin gerektirmeden path kontrolu yapar. |
| `clear_build_lock.ps1` | Acil workaround; build su an kilitliyse kullan. | Hayir. | `gradlew --stop` ile Gradle daemon'u durdurur ve sadece bu projenin `app\build` klasorunu siler. Kaynak koda ve git'e dokunmaz. |

## Notlar

- Hook aktifi: `git config core.hooksPath .githooks`
- `release.jks` ve `keystore.properties` commitlenmemelidir; `.gitignore` kapsaminda kalir.
