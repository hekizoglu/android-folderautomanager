#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
check_duplicates.py — AppClassifier.kt exactMatchMap duplicate paket tarayıcı.

Kotlin mapOf() duplicate key'de hata VERMEZ, sessizce son entry'i kullanır.
Bu script AppClassifier (ve istenirse KeywordDatabase) içindeki
'"paket.adı" to CAT_X' satırlarını tarar, tekrar eden paket adlarını raporlar.

Kullanım:
    python3 scripts/check_duplicates.py
    python3 scripts/check_duplicates.py <dosya.kt>
    python3 scripts/check_duplicates.py --kategori   # kategori dağılımını da göster

Çıkış kodu: 0 = temiz, 1 = duplicate bulundu (CI/pre-commit için).
"""
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

# Projeye göre düzenle — birden fazla aday yol denenir
DEFAULT_CANDIDATES = [
    "app/src/main/java/com/armutlu/apporganizer/data/AppClassifier.kt",
    "app/src/main/java/com/armutlu/apporganizer/domain/AppClassifier.kt",
    "AppClassifier.kt",
]

# "paket.adı" to CAT_X   (boşluk/tab esnek, tek veya çift tırnak)
ENTRY_RE = re.compile(r'["\']([\w.]+)["\']\s+to\s+(CAT_\w+|\w+)')


def find_target(arg_path: str | None) -> Path:
    if arg_path and not arg_path.startswith("--"):
        p = Path(arg_path)
        if p.exists():
            return p
        sys.exit(f"HATA: dosya bulunamadı: {arg_path}")
    for c in DEFAULT_CANDIDATES:
        p = Path(c)
        if p.exists():
            return p
    sys.exit(
        "HATA: AppClassifier.kt bulunamadı. Yol ver:\n"
        "  python3 scripts/check_duplicates.py app/src/.../AppClassifier.kt"
    )


def scan(path: Path):
    text = path.read_text(encoding="utf-8", errors="replace")
    pkgs = []          # sıralı paket listesi
    pkg_to_cats = defaultdict(list)
    for m in ENTRY_RE.finditer(text):
        pkg, cat = m.group(1), m.group(2)
        # Sadece paket gibi görünenleri al (en az bir nokta) — false positive azalt
        if "." in pkg:
            pkgs.append(pkg)
            pkg_to_cats[pkg].append(cat)
    return pkgs, pkg_to_cats


def main():
    args = sys.argv[1:]
    show_cat = "--kategori" in args
    path = find_target(args[0] if args else None)

    pkgs, pkg_to_cats = scan(path)
    total = len(pkgs)
    unique = len(set(pkgs))
    counts = Counter(pkgs)
    dups = {p: c for p, c in counts.items() if c > 1}

    print(f"📄 Dosya: {path}")
    print(f"📦 Toplam entry: {total}  |  Benzersiz: {unique}  |  Duplicate: {total - unique}")

    if show_cat:
        cat_counter = Counter()
        for p in set(pkgs):
            # Bir paketin son kategorisi (Kotlin'in seçeceği)
            cat_counter[pkg_to_cats[p][-1]] += 1
        print("\n📊 Kategori dağılımı (benzersiz paketler):")
        for cat, n in sorted(cat_counter.items(), key=lambda x: -x[1]):
            print(f"   {cat:<24} {n}")

    if not dups:
        print("\n✅ Duplicate YOK — temiz.")
        return 0

    print(f"\n⚠️  {len(dups)} duplicate paket bulundu:")
    for pkg, c in sorted(dups.items(), key=lambda x: -x[1]):
        cats = pkg_to_cats[pkg]
        cat_note = ""
        if len(set(cats)) > 1:
            cat_note = f"  ❗ FARKLI KATEGORİLER: {' / '.join(cats)} (son kazanır: {cats[-1]})"
        else:
            cat_note = f"  (kategori: {cats[-1]})"
        print(f"   {c}x  {pkg}{cat_note}")

    print(f"\n💡 Temizlemek için: python3 scripts/dedup_classifier.py {path}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
