"""
AppClassifier.kt'deki exactMatchMap'i app_categories.json olarak dışa aktarır.
JSON değerleri Category.CAT_XXX sabitlerinin gerçek string değerleridir.
Çalıştır: python scripts/export_classifier_json.py
"""
import re, json, pathlib

ROOT = pathlib.Path(__file__).parent.parent
SRC  = ROOT / "app/src/main/java/com/armutlu/apporganizer/domain/usecase/classify/AppClassifier.kt"
CAT  = ROOT / "app/src/main/java/com/armutlu/apporganizer/domain/models/Category.kt"
OUT  = ROOT / "app/src/main/assets/app_categories.json"

# Category.kt'den CAT_XXX → "actual_value" eşlemesi çıkar
CAT_RE = re.compile(r'const val (\w+)\s*=\s*"([^"]+)"')
cat_map = {}
with open(CAT, encoding="utf-8") as f:
    for line in f:
        m = CAT_RE.search(line)
        if m:
            cat_map[m.group(1)] = m.group(2)  # "CAT_SOCIAL" -> "social"

# AppClassifier.kt'den exactMatchMap entryleri çıkar
ENTRY_RE = re.compile(r'"([^"]+)"\s+to\s+Category\.(\w+)')

entries = {}
in_exact = False
brace_depth = 0

with open(SRC, encoding="utf-8") as f:
    for line in f:
        stripped = line.strip()
        if "private val exactMatchMap" in stripped:
            in_exact = True
        if in_exact:
            brace_depth += stripped.count("(") - stripped.count(")")
            m = ENTRY_RE.search(stripped)
            if m:
                pkg, cat_const = m.group(1), m.group(2)
                resolved = cat_map.get(cat_const, cat_const.lower().replace("cat_", ""))
                entries[pkg] = resolved
            if in_exact and brace_depth <= 0 and ")" in stripped:
                in_exact = False

OUT.parent.mkdir(parents=True, exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(entries, f, ensure_ascii=False, indent=None, separators=(",", ":"))

print(f"OK: {len(entries)} entries, cats: {len(set(entries.values()))}")
print(f"Output: {OUT}")
