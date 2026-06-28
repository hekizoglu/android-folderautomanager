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
| `add_defender_exclusion.ps1` | Çift tıkla → UAC Evet | Windows Defender'dan Gradle build dizinini çıkarır (CS-3 için). |

## Notlar

- `fix_encoding.py` Windows cp1254 terminalinde doğrudan çalışır (`sys.stdout.reconfigure` ile).
- `check_duplicates.py` pre-commit hook `.githooks/pre-commit` tarafından otomatik çalıştırılır.
- Hook aktifleştirme: `git config core.hooksPath .githooks`
