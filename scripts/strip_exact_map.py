"""
AppClassifier.kt'den _exactMatchMapLegacy bloğunu çıkarır.
Çalıştır: python scripts/strip_exact_map.py
"""
import pathlib, re

ROOT = pathlib.Path(__file__).parent.parent
SRC  = ROOT / "app/src/main/java/com/armutlu/apporganizer/domain/usecase/classify/AppClassifier.kt"

lines = SRC.read_text(encoding="utf-8").splitlines(keepends=True)

out = []
skip = False
depth = 0

for line in lines:
    stripped = line.strip()
    if not skip and "_exactMatchMapLegacy = mapOf(" in stripped:
        skip = True
        depth = 1
        continue  # bu satiri atla
    if skip:
        depth += stripped.count("(") - stripped.count(")")
        if depth <= 0:
            skip = False  # kapanan ) bulundu, bu satiri da atla
        continue
    out.append(line)

SRC.write_text("".join(out), encoding="utf-8")
print(f"Done: {len(lines)} -> {len(out)} lines (removed {len(lines)-len(out)})")
