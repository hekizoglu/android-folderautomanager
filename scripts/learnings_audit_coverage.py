#!/usr/bin/env python3
"""
LEARNINGS.md E1-E16 hatalari ile audit.ps1 kurallari arasindaki
coverage matrix'i olusturur.

Kullanim: python scripts/learnings_audit_coverage.py
"""
import re, sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')

LEARNINGS_ERRORS = {
    "E1":  ("Smart cast impossible", "by delegate nullable — icon?.let pattern"),
    "E2":  ("Curly quote derleme hatasi", "Edit tool curly quote"),
    "E3":  ("mapOf() duplicate sessiz kayip", "check_duplicates.py pre-commit"),
    "E4":  ("KeywordDatabase kategori kaybi", "mapOf() ayni key iki kez"),
    "E5":  ("Turkce arama bulamiyor", "ignoreCase=true I/İ hatalari"),
    "E6":  ("Settings donus UI eski", "remember{} bir kez hesaplar — DisposableEffect"),
    "E7":  ("Onboarding her acilista tekrar", "Yanlis prefs key"),
    "E8":  ("Badge silinince kalmaya devam", "isNotEmpty guard"),
    "E9":  ("isLoadingApps race condition", "@Volatile bilesik operasyon korumaz"),
    "E10": ("Git push non-fast-forward", "git pull --rebase"),
    "E11": ("Merge conflict AppClassifier", "Python ile birlestir, set dedup"),
    "E12": ("PowerShell heredoc syntax hatasi", "PS 5.1 bash heredoc"),
    "E13": ("VerifyError DVM register limit", "Buyuk composable 300+ satir"),
    "E14": ("derivedStateOf plain String reaktif degil", "remember(key) ile invalidation"),
    "E15": ("fix_encoding.py MOJIBAKE curly-quote", "Dict byte bazli olustur"),
    "E16": ("fix_encoding.py terminal cp1254 emoji", "sys.stdout.reconfigure utf-8"),
}

# audit.ps1 kurallarinin hangi LEARNINGS hatasina karsilik geldigi
AUDIT_COVERAGE = {
    "E1":  ("NONE", "No direct audit rule for smart cast pattern"),
    "E2":  ("NONE", "Encoding/curly quote — fix_encoding.py'da, audit.ps1'de degil"),
    "E3":  ("pre-commit", "pre-commit hook check_duplicates.py"),
    "E4":  ("NONE", "KeywordDatabase duplicate — audit.ps1'de kural yok"),
    "E5":  ("Y1", "Y1: lowercase() locale belirtilmemis"),
    "E6":  ("NONE", "Settings reaktiflik — audit.ps1'de kural yok"),
    "E7":  ("NONE", "Onboarding prefs key — audit.ps1'de kural yok"),
    "E8":  ("NONE", "Badge guard — audit.ps1'de kural yok"),
    "E9":  ("CE6", "CE6: @Volatile bilesik operasyon — AtomicBoolean"),
    "E10": ("NONE", "Git operasyon — audit.ps1 kapsaminda degil"),
    "E11": ("pre-commit", "pre-commit hook check_duplicates.py"),
    "E12": ("NONE", "PowerShell syntax — audit.ps1 kapsaminda degil"),
    "E13": ("NONE", "File size kontrolü — audit.ps1'de kural yok"),
    "E14": ("CE4", "CE4: derivedStateOf unstable input riski"),
    "E15": ("NONE", "Script hatasi — audit.ps1 kapsaminda degil"),
    "E16": ("NONE", "Script hatasi — audit.ps1 kapsaminda degil"),
}

# Kapsanmayan ama audit.ps1'e eklenebilecekler
ACTIONABLE_GAPS = {
    "E6":  'Path HomeScreen/SettingsScreen; Pattern \'remember \\{[^}]*AppPrefs\\.\'; Desc "AppPrefs remember{} keysiz — DisposableEffect kullanilmali."',
    "E13": 'Path **/*.kt; line count > 300 check; Desc "300+ satir @Composable — VerifyError riski."',
}

def main():
    covered = sum(1 for v in AUDIT_COVERAGE.values() if v[0] != "NONE")
    total = len(AUDIT_COVERAGE)
    pct = round(covered / total * 100)

    print("=" * 60)
    print("LEARNINGS → audit.ps1 Coverage Matrix")
    print("=" * 60)
    print(f"Coverage: {covered}/{total} ({pct}%)\n")

    print(f"{'ID':<5} {'Audit Kural':<12} {'Aciklama'}")
    print("-" * 60)
    for eid, (errdesc, _fix) in LEARNINGS_ERRORS.items():
        rule, note = AUDIT_COVERAGE[eid]
        icon = "✅" if rule != "NONE" else "❌"
        rule_display = rule if rule != "NONE" else "—"
        print(f"{eid:<5} {rule_display:<12} {icon} {errdesc[:35]}")

    print("\n" + "=" * 60)
    print("Kapsamdisi + Eklenebilecek Kurallar:")
    print("-" * 60)
    for eid, suggestion in ACTIONABLE_GAPS.items():
        errdesc = LEARNINGS_ERRORS[eid][0]
        print(f"  {eid}: {errdesc}")
        print(f"    Oneri: {suggestion[:80]}")
    print()

    # Gap kurallarini audit.ps1'e ekleme onerileri
    gaps_not_scriptable = [
        eid for eid, (rule, _) in AUDIT_COVERAGE.items()
        if rule == "NONE" and eid not in ACTIONABLE_GAPS
    ]
    print(f"audit.ps1 disinda kalan (tool/git/script hatalari): {', '.join(gaps_not_scriptable)}")
    print(f"\nSonuc: {pct}% coverage ({covered}/{total})")
    if ACTIONABLE_GAPS:
        print(f"  +{len(ACTIONABLE_GAPS)} kural audit.ps1'e eklenebilir -> %{round((covered+len(ACTIONABLE_GAPS))/total*100)} olur")

if __name__ == "__main__":
    main()
