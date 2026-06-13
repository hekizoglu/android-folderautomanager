"""
AppOrganizer kaynak kodunu NotebookLM klasörüne aktarır.
Her döngü sonunda çalıştırılır:
  python scripts/update_notebooklm.py
"""
import os, glob, datetime, sys

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUTPUT_DIR = os.path.join(os.path.expanduser("~"), "Desktop", "notebooklm_apporganizer")
OUTPUT_FILE = os.path.join(OUTPUT_DIR, "app_source.txt")

os.makedirs(OUTPUT_DIR, exist_ok=True)

kt_files = sorted(glob.glob(os.path.join(BASE, "app/src/main/java/**/*.kt"), recursive=True))
extra_files = [
    os.path.join(BASE, "app/src/main/AndroidManifest.xml"),
    os.path.join(BASE, "app/build.gradle"),
    os.path.join(BASE, "app/build.gradle.kts"),
    os.path.join(BASE, "build.gradle"),
    os.path.join(BASE, "build.gradle.kts"),
    os.path.join(BASE, "CLAUDE.md"),
]

lines = []
lines.append("# AppOrganizer — Tam Kaynak Kodu (NotebookLM)")
lines.append(f"# Guncelleme: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M')}")
lines.append(f"# Kotlin dosya sayisi: {len(kt_files)}")
lines.append("=" * 80)

for f in kt_files:
    rel = os.path.relpath(f, os.path.join(BASE, "app/src/main/java")).replace("\\", "/")
    lines.append(f"\n{'='*60}\nFILE: {rel}\n{'='*60}")
    try:
        lines.append(open(f, encoding="utf-8", errors="replace").read())
    except Exception as e:
        lines.append(f"[OKUNAMADI: {e}]")

for f in extra_files:
    if os.path.exists(f):
        rel = os.path.relpath(f, BASE).replace("\\", "/")
        lines.append(f"\n{'='*60}\nFILE: {rel}\n{'='*60}")
        try:
            lines.append(open(f, encoding="utf-8", errors="replace").read())
        except Exception as e:
            lines.append(f"[OKUNAMADI: {e}]")

content = "\n".join(lines)
open(OUTPUT_FILE, "w", encoding="utf-8").write(content)
size_kb = os.path.getsize(OUTPUT_FILE) // 1024
print(f"[NotebookLM] {len(kt_files)} kt dosyasi — {size_kb} KB — {OUTPUT_FILE}")
