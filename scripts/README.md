# scripts/ — AppOrganizer Yardımcı Araçlar

| Script | Kullanım | Açıklama |
|--------|----------|----------|
| `fix_encoding.py` | `python scripts/fix_encoding.py <dosya>` | Curly quote, mojibake, Türkçe double-encode düzeltir. `.bak` yedek alır. |
| `fix_denetim_encoding.ps1` | `.\scripts\fix_denetim_encoding.ps1` | KiloCode'un bozduğu local_denetim_*.md dosyalarını toplu düzeltir. |
| `check_duplicates.py` | `python scripts/check_duplicates.py assets/app_categories.json` | JSON'daki duplicate key'leri tespit eder (pre-commit hook tarafından çalıştırılır). |
| `dedup_classifier.py` | `python scripts/dedup_classifier.py` | app_categories.json'dan duplicate entry'leri temizler, merge conflict sonrası kullan. |
| `export_classifier_json.py` | `python scripts/export_classifier_json.py` | AppClassifier Kotlin kodundan JSON'a export eder (tek seferlik migrasyon). |
| `strip_exact_map.py` | `python scripts/strip_exact_map.py` | Kotlin haritasından exact match satırlarını kaldırır. |
| `local_ai.py` | `python scripts/local_ai.py "soru"` | Lokal AI gateway (localhost:20128) üzerinden model sorgusu. Offline ise hata verir. |
| `audit.ps1` | `.\scripts\audit.ps1` | 15 dakikalık odak rotasyonlu statik kod denetimi. Çıktı: local_denetim_raporu.md |
| `version_bump.ps1` | `.\scripts\version_bump.ps1 patch` | versionCode+1, versionName patch/minor/major artırır. app/build.gradle.kts günceller. |
| `score_docs_backlog.ps1` | `.\scripts\score_docs_backlog.ps1 [-UpdateRoadmap]` | `docs/` altındaki raporları KV+U+BR+EA puanlar, `docs/internal/docs_backlog_score.md` üretir; `-UpdateRoadmap` ile ROADMAP.md'deki `DOCS_SCORE_HIGH` bloğunu (15+ puanlılar) senkronize eder. |
| `log_cycle_time.ps1` | `.\scripts\log_cycle_time.ps1 -StartTime "14:05" -EndTime "14:22" -TokenLevel orta -WorkType BUILD -Note "assembleDebug baseline"` | `harcananvakit.md`'ye mevcut tablo formatına uygun tek satır append eder. `-StartTime` verilmezse `-DurationMinutes` ile geriye hesaplar. `-TokenLevel`: dusuk/orta/yuksek, `-WorkType`: KOD/BUILD/GIT/ORTAM/DOKUMAN/HATA_GIDER/ARASTIRMA/TEST. |
| `benchmark_build.ps1` | `.\scripts\benchmark_build.ps1 [-SkipProfile] [-SkipConfigurationCache]` | `--profile --rerun-tasks assembleDebug` ve configuration-cache'li `compileDebugKotlin` çalıştırır, `docs/internal/build_benchmark_latest.md` üretir. |

### CS-3 Build Kilidi (Windows Defender) Araçları

| Script | Ne zaman kullan | Admin gerekir mi | Ne yapar |
|--------|-----------------|-------------------|----------|
| `add_defender_exclusion.ps1` | **Kalıcı çözüm** — build kilidi tekrar tekrar oluşuyorsa bir kez çalıştır | ✅ Evet (UAC self-elevation) | `app\build`, `.gradle`, `.android` klasörlerini Windows Defender gerçek zamanlı taramadan kalıcı olarak dışlar. |
| `clear_build_lock.ps1` | **Acil workaround** — build şu an kilitliyse, kalıcı çözümü henüz çalıştırmadıysan | ❌ Hayır | Sadece bu projenin `app\build` klasörünü siler + `java.exe` süreçlerini durdurur; kaynak koda/git'e dokunmaz. Sonrasında `.\gradlew assembleDebug` ile yeniden build al. |

## Notlar

- `fix_encoding.py` Windows cp1254 terminalinde doğrudan çalışır (`sys.stdout.reconfigure` ile).
- `check_duplicates.py` pre-commit hook `.githooks/pre-commit` tarafından otomatik çalıştırılır.
- Hook aktifleştirme: `git config core.hooksPath .githooks`
