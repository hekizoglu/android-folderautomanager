from pathlib import Path

p = Path("HISTORY.md")
text = p.read_text(encoding="utf-8")

history_entry = """## Döngü M9 — 2026-07-26
**Yapılanlar:** Aktiviteler + navigasyon denetimi — MainActivity.kt (openBugReport ölü metodu silindi, installSplashScreen super.onCreate öncesine alındı, intent extra whitelist doğrulaması teyit edildi), LauncherActivity.kt (widget bind/configure akışları, safe mode, home tuşu komut dağıtımı), AppNavigation.kt (Routes whitelist, fromTickerRoute TEK nokta dönüştürücü) ve Onboarding adımları doğrulandı.
**Bug:** Yok — ölü private metod temizliği sonrası compileDebugKotlin 2m 21s içinde yeşil.
**Sonraki:** M10 (Global ölü kod süpürmesi: detekt raporu + cross-module unused sembol taraması).

"""

prefix = "# AppOrganizer — Döngü Geçmişi\n\n"
if text.startswith(prefix):
    new_text = prefix + history_entry + text[len(prefix):]
else:
    new_text = history_entry + text

p.write_text(new_text, encoding="utf-8")
print("Updated HISTORY.md for M9 successfully")
