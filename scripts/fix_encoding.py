#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
fix_encoding.py — Kotlin/metin dosyalarında Edit tool kaynaklı encoding bozukluklarını düzeltir.

Düzelttiği sorunlar:
  1. Curly/smart quotes (" " ' ') → düz ASCII (" ')  → Kotlin "Expecting an expression" hatası
  2. Bozuk UTF-8 mojibake (â‚¬ vb. C3 A2 E2 82 AC dizileri) → temizleme
  3. Non-breaking space (\u00A0) → normal boşluk
  4. Zero-width karakterler (\u200B/\u200C/\u200D/\uFEFF) → kaldır

GÜVENLİK: Değişiklik yapılan her dosya için .bak yedeği alır.

Kullanım:
    python3 scripts/fix_encoding.py <dosya.kt>
    python3 scripts/fix_encoding.py app/src/.../SettingsScreen.kt app/src/.../HomeScreen.kt
    python3 scripts/fix_encoding.py --scan app/src   # sadece tara, raporla (yazma)
"""
import sys
import os
import shutil
from pathlib import Path

# Terminal cp1254 emoji hatasını önle
if sys.stdout.encoding and sys.stdout.encoding.lower() not in ('utf-8', 'utf-8-sig'):
    os.environ.setdefault('PYTHONIOENCODING', 'utf-8')
    sys.stdout.reconfigure(encoding='utf-8', errors='replace') if hasattr(sys.stdout, 'reconfigure') else None

# (bozuk, düzeltme) eşlemeleri
REPLACEMENTS = {
    "\u201c": '"',   # sol çift tırnak
    "\u201d": '"',   # sağ çift tırnak
    "\u2018": "'",   # sol tek tırnak
    "\u2019": "'",   # sağ tek tırnak / apostrof
    "\u00a0": " ",   # non-breaking space
    "\u200b": "",    # zero-width space
    "\u200c": "",    # zero-width non-joiner
    "\u200d": "",    # zero-width joiner
    "\ufeff": "",    # BOM / zero-width no-break space
    "\u2013": "-",   # en-dash (Kotlin string içinde sorun olmaz ama tutarlılık)
    "\u2014": "-",   # em-dash
}

# Yaygın mojibake dizileri (UTF-8 yanlış decode edilmiş) — düz eşleme
# Keys: UTF-8 bytes cp1252 ile yanlış okunmuş Unicode metin olarak
# Yaygın mojibake dizileri (UTF-8 yanlış decode edilmiş)
# Keys: UTF-8 byte dizisi cp1252 ile yanlış okunmuş — byte bazlı hesaplama
def _mb(*bs):
    return bytes(bs).decode('cp1252', errors='replace')

MOJIBAKE = {
    _mb(0xe2, 0x82, 0xac): chr(0x20ac),  # euro sign
    _mb(0xe2, 0x80, 0x9c): chr(0x201c),  # left double quotation mark
    _mb(0xe2, 0x80, 0x9d): chr(0x201d),  # right double quotation mark
    _mb(0xe2, 0x80, 0x99): chr(0x2019),  # right single quotation mark
    _mb(0xe2, 0x80, 0x93): chr(0x2013),  # en-dash
    _mb(0xe2, 0x80, 0x94): chr(0x2014),  # em-dash
}


# Türkçe double-encoded UTF-8 düzeltme tablosu
# Kaynak: Add-Content / PowerShell UTF-16LE → git → UTF-8 çift encode zinciri
# Her dizi: (bozuk_unicode_str, doğru_karakter)
TURKISH_DOUBLE_ENCODED = [
    ("Ã¶", "ö"),   # ö — U+00C3 U+00B6
    ("Ã¼", "ü"),   # ü
    ("Ä±", "ı"),   # ı — U+00C4 U+00B1
    ("Ã§", "ç"),   # ç
    ("Ä°", "İ"),   # İ
    ("ÄŸ", "ğ"),   # ğ — U+00C4 U+009F
    ("ÅŸ", "ş"),   # ş — U+00C5 U+0178
    ("Åž", "Ş"),   # Ş
    ("Ã–", "Ö"),   # Ö
    ("Ãœ", "Ü"),   # Ü
    ("Ã‡", "Ç"),   # Ç
    ("Ä\x9e", "Ğ"),  # Ğ
    ("Ã-", "Ö"),   # Ö (Ã\x96 → "-" ye dönüştükten sonra kalan)
    # Arrow ve özel semboller
    ("\xe2†\x27", "->"),  # â†' (→ triple encode)
    ("â†'", "->"),              # → bozuk
]

TEXT_EXT = {".kt", ".kts", ".java", ".xml", ".md", ".json", ".gradle", ".txt", ".py", ".ps1"}


def fix_content(text: str):
    changes = []
    # Türkçe double-encoded UTF-8 (PowerShell Add-Content kaynağı) — önce uygula
    for bad, good in TURKISH_DOUBLE_ENCODED:
        if bad in text:
            n = text.count(bad)
            text = text.replace(bad, good)
            changes.append(f"tr-encode {bad!r}→{good!r} ×{n}")
    for bad, good in MOJIBAKE.items():
        if bad in text:
            n = text.count(bad)
            text = text.replace(bad, good)
            changes.append(f"mojibake {bad!r}→{good!r} ×{n}")
    for bad, good in REPLACEMENTS.items():
        if bad in text:
            n = text.count(bad)
            text = text.replace(bad, good)
            name = {
                "\u201c": """, "\u201d": """, "\u2018": "'", "\u2019": "'",
                "\u00a0": "NBSP", "\u200b": "ZWSP", "\u200c": "ZWNJ",
                "\u200d": "ZWJ", "\ufeff": "BOM", "\u2013": "en-dash", "\u2014": "em-dash",
            }.get(bad, repr(bad))
            changes.append(f"{name}→{good!r} ×{n}")
    return text, changes


def gather_files(args):
    files = []
    for a in args:
        p = Path(a)
        if p.is_dir():
            files += [f for f in p.rglob("*") if f.suffix in TEXT_EXT]
        elif p.exists():
            files.append(p)
        else:
            print(f"⚠️  atlandı (yok): {a}")
    return files


def main():
    args = sys.argv[1:]
    if not args:
        sys.exit("Kullanım: python3 scripts/fix_encoding.py <dosya|dizin> [...]  |  --scan <dizin>")

    scan_only = "--scan" in args
    targets = [a for a in args if not a.startswith("--")]
    files = gather_files(targets)

    if not files:
        sys.exit("Hedef dosya bulunamadı.")

    total_fixed = 0
    for f in files:
        try:
            original = f.read_text(encoding="utf-8", errors="replace")
        except Exception as e:
            print(f"⚠️  okunamadı {f}: {e}")
            continue
        fixed, changes = fix_content(original)
        if changes:
            total_fixed += 1
            print(f"\n📄 {f}")
            for c in changes:
                print(f"   • {c}")
            if not scan_only:
                shutil.copy2(f, f.with_suffix(f.suffix + ".bak"))
                f.write_text(fixed, encoding="utf-8")
                print("   ✅ düzeltildi (.bak yedeklendi)")

    if total_fixed == 0:
        print("✅ Encoding sorunu bulunamadı — tüm dosyalar temiz.")
    else:
        action = "tespit edildi (scan)" if scan_only else "düzeltildi"
        print(f"\n{'🔍' if scan_only else '✅'} {total_fixed} dosyada sorun {action}.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
