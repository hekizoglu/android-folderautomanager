#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
update_notebooklm.py — AppOrganizer kaynak kodunu tek bir metin dosyasına toplar.
NotebookLM'e kaynak olarak yüklenecek dosyayı günceller.

Çıktı: Masaüstü/notebooklm_apporganizer/app_source.txt
       (yoksa oluşturur)

Kullanım:
    python3 scripts/update_notebooklm.py
    python3 scripts/update_notebooklm.py --out custom/path.txt
    python3 scripts/update_notebooklm.py --dry-run   # sadece istatistik, yazma
"""
import os
import sys
import re
from pathlib import Path
from datetime import datetime

# --- Konfigürasyon ---
SRC_ROOT = Path("app/src/main/java/com/armutlu/apporganizer")

# Dahil edilecek dosya uzantıları
INCLUDE_EXT = {".kt", ".kts"}

# Dahil edilecek dizin/dosya keyword'leri (None = hepsi)
INCLUDE_DIRS = None

# Hariç tutulacaklar (test, generated, build çıktıları)
EXCLUDE_PATTERNS = [
    r".*Test\.kt$",
    r".*/build/.*",
    r".*\.generated\..*",
]

MAX_FILE_CHARS = 50_000   # Tek dosya karakter limiti (çok büyük dosyaları kırp)
MAX_TOTAL_CHARS = 500_000 # Toplam çıktı limiti

def get_output_path(arg_out: str | None) -> Path:
    if arg_out:
        return Path(arg_out)
    desktop = Path.home() / "Desktop"
    out_dir = desktop / "notebooklm_apporganizer"
    out_dir.mkdir(parents=True, exist_ok=True)
    return out_dir / "app_source.txt"

def should_exclude(path: Path) -> bool:
    s = str(path).replace("\\", "/")
    return any(re.search(p, s) for p in EXCLUDE_PATTERNS)

def collect_files(root: Path) -> list[Path]:
    files = []
    for f in sorted(root.rglob("*")):
        if f.suffix in INCLUDE_EXT and f.is_file() and not should_exclude(f):
            files.append(f)
    return files

def build_output(files: list[Path], dry: bool) -> str:
    lines = []
    total = 0
    skipped = 0

    header = (
        f"# AppOrganizer — Kaynak Kodu Özeti\n"
        f"# Oluşturulma: {datetime.now().strftime('%Y-%m-%d %H:%M')}\n"
        f"# Dosya sayısı: {len(files)}\n"
        f"# NotebookLM kaynağı — Claude tarafından üretildi\n\n"
    )
    lines.append(header)
    total += len(header)

    for f in files:
        try:
            content = f.read_text(encoding="utf-8", errors="replace")
        except Exception as e:
            lines.append(f"# HATA: {f} okunamadı: {e}\n\n")
            continue

        if len(content) > MAX_FILE_CHARS:
            content = content[:MAX_FILE_CHARS] + f"\n# ... ({len(content) - MAX_FILE_CHARS} karakter kırpıldı)\n"

        section = f"### {f}\n\n```kotlin\n{content}\n```\n\n"

        if total + len(section) > MAX_TOTAL_CHARS:
            skipped += 1
            continue

        lines.append(section)
        total += len(section)

    if skipped:
        lines.append(f"\n# Not: {skipped} dosya toplam karakter limiti ({MAX_TOTAL_CHARS:,}) nedeniyle atlandı.\n")

    return "".join(lines)

def main():
    args = sys.argv[1:]
    dry = "--dry-run" in args
    out_arg = None
    for i, a in enumerate(args):
        if a == "--out" and i + 1 < len(args):
            out_arg = args[i + 1]

    if not SRC_ROOT.exists():
        sys.exit(f"[ERR] Kaynak dizin bulunamadı: {SRC_ROOT}\n   Proje kökünden çalıştır.")

    files = collect_files(SRC_ROOT)
    print(f"[+] {len(files)} Kotlin dosyası bulundu.")

    output = build_output(files, dry)
    total_chars = len(output)
    print(f"[*] Toplam: {total_chars:,} karakter ({total_chars // 1024} KB)")

    if dry:
        print("(dry-run — dosya yazılmadı)")
        return 0

    out_path = get_output_path(out_arg)
    out_path.write_text(output, encoding="utf-8")
    print(f"[OK] Yazıldı: {out_path}")
    print(f"   -> NotebookLM'e bu dosyayı kaynak olarak ekle/güncelle.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
