#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
dedup_classifier.py — AppClassifier.kt exactMatchMap duplicate temizleyici.

Kotlin'in davranışını taklit eder: duplicate paket için SON entry kazanır.
Duplicate olan paketlerin ilk geçişlerini siler, sadece sonuncuyu bırakır.
Yorum satırlarını ve map dışı içeriği KORUR — yalnızca duplicate entry satırlarını dokunur.

GÜVENLİK: Çalışmadan önce <dosya>.bak yedeği alır.
Her zaman dedup sonrası `python3 scripts/check_duplicates.py` ile doğrula.

Kullanım:
    python3 scripts/dedup_classifier.py                       # varsayılan yolu dene
    python3 scripts/dedup_classifier.py <dosya.kt>
    python3 scripts/dedup_classifier.py <dosya.kt> --dry-run  # sadece göster, yazma
"""
import re
import sys
import shutil
from collections import Counter
from pathlib import Path

DEFAULT_CANDIDATES = [
    "app/src/main/java/com/armutlu/apporganizer/data/AppClassifier.kt",
    "app/src/main/java/com/armutlu/apporganizer/domain/AppClassifier.kt",
    "AppClassifier.kt",
]

ENTRY_RE = re.compile(r'["\']([\w.]+)["\']\s+to\s+(CAT_\w+|\w+)')


def find_target(arg_path):
    if arg_path and not arg_path.startswith("--"):
        p = Path(arg_path)
        if p.exists():
            return p
        sys.exit(f"HATA: dosya bulunamadı: {arg_path}")
    for c in DEFAULT_CANDIDATES:
        if Path(c).exists():
            return Path(c)
    sys.exit("HATA: AppClassifier.kt bulunamadı. Yol ver.")


def main():
    args = sys.argv[1:]
    dry = "--dry-run" in args
    pos = [a for a in args if not a.startswith("--")]
    path = find_target(pos[0] if pos else None)

    lines = path.read_text(encoding="utf-8", errors="replace").splitlines(keepends=True)

    # 1. Geçiş: her paketin kaç kez geçtiğini ve hangi satırlarda olduğunu bul
    pkg_lines = {}  # pkg -> [satır indexleri]
    for i, line in enumerate(lines):
        m = ENTRY_RE.search(line)
        if m and "." in m.group(1):
            pkg_lines.setdefault(m.group(1), []).append(i)

    dups = {p: idxs for p, idxs in pkg_lines.items() if len(idxs) > 1}
    if not dups:
        print(f"✅ {path}: duplicate yok, yapılacak iş yok.")
        return 0

    # Silinecek satır indexleri: her duplicate paketin SON geçişi hariç hepsi
    to_remove = set()
    for p, idxs in dups.items():
        for i in idxs[:-1]:  # sonuncuyu koru
            to_remove.add(i)

    print(f"📄 {path}")
    print(f"⚠️  {len(dups)} duplicate paket, {len(to_remove)} satır silinecek (son entry korunur):")
    for p, idxs in sorted(dups.items()):
        print(f"   {p}  →  satır {[i+1 for i in idxs]} ({len(idxs)}x), korunan: {idxs[-1]+1}")

    if dry:
        print("\n(dry-run — dosya değiştirilmedi)")
        return 0

    # Yedek al
    backup = path.with_suffix(path.suffix + ".bak")
    shutil.copy2(path, backup)
    print(f"\n💾 Yedek: {backup}")

    # Satırları sil
    new_lines = [ln for i, ln in enumerate(lines) if i not in to_remove]
    path.write_text("".join(new_lines), encoding="utf-8")

    # Doğrula
    after = path.read_text(encoding="utf-8")
    remaining = Counter(
        m.group(1) for m in ENTRY_RE.finditer(after) if "." in m.group(1)
    )
    still_dup = {p: c for p, c in remaining.items() if c > 1}
    print(f"✅ Temizlendi. Kalan benzersiz paket: {len(remaining)}")
    if still_dup:
        print(f"❗ DİKKAT: hâlâ {len(still_dup)} duplicate var (çok satırlı entry olabilir): {list(still_dup)[:5]}")
        return 1
    print("✅ 0 duplicate doğrulandı.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
