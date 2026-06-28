#!/usr/bin/env python3
"""
Compose Compiler rapor özetleyici.
build/compose_compiler/ altındaki CSV ve TXT dosyalarını okur,
skippable olmayan composable'ları ve unstable class'ları listeler.

Kullanım: python scripts/compose_stability_report.py
"""
import os, csv, sys

REPORT_DIR = os.path.join("app", "build", "compose_compiler")

def main():
    if not os.path.isdir(REPORT_DIR):
        print(f"Rapor dizini bulunamadi: {REPORT_DIR}")
        print("Once './gradlew assembleDebug' calistirin.")
        sys.exit(1)

    files = os.listdir(REPORT_DIR)
    print(f"=== Compose Compiler Raporu ({len(files)} dosya) ===\n")

    # composables.csv — skippable olmayan fonksiyonlar
    for f in sorted(files):
        if f.endswith("composables.csv"):
            path = os.path.join(REPORT_DIR, f)
            non_skippable = []
            try:
                with open(path, encoding="utf-8") as fh:
                    reader = csv.DictReader(fh)
                    for row in reader:
                        if row.get("skippable", "1") == "0":
                            non_skippable.append(row.get("name", "?"))
            except Exception as e:
                print(f"  Hata ({f}): {e}")
                continue
            if non_skippable:
                print(f"[UYARI] Skippable olmayan composable'lar ({f}):")
                for name in non_skippable[:20]:
                    print(f"  - {name}")
                if len(non_skippable) > 20:
                    print(f"  ... ve {len(non_skippable)-20} tane daha")
            else:
                print(f"[OK] Tum composable'lar skippable: {f}")
            print()

    # classes.csv — unstable class'lar
    for f in sorted(files):
        if f.endswith("classes.csv"):
            path = os.path.join(REPORT_DIR, f)
            unstable = []
            try:
                with open(path, encoding="utf-8") as fh:
                    reader = csv.DictReader(fh)
                    for row in reader:
                        if row.get("stable", "1") == "0":
                            unstable.append(row.get("name", "?"))
            except Exception as e:
                print(f"  Hata ({f}): {e}")
                continue
            if unstable:
                print(f"[UYARI] Unstable class'lar ({f}):")
                for name in unstable[:20]:
                    print(f"  - {name}")
                if len(unstable) > 20:
                    print(f"  ... ve {len(unstable)-20} tane daha")
            else:
                print(f"[OK] Tum class'lar stable: {f}")
            print()

    # module summary TXT
    for f in sorted(files):
        if f.endswith("-module.txt") or f == "module.txt":
            path = os.path.join(REPORT_DIR, f)
            try:
                content = open(path, encoding="utf-8").read()
                print(f"=== Module Özeti ({f}) ===")
                print(content[:1000])
                print()
            except Exception:
                pass

if __name__ == "__main__":
    main()
