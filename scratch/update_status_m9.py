from pathlib import Path

p = Path("MANAGEMENT/KOD_TARAMA_DURUM.md")
text = p.read_text(encoding="utf-8")

# Update table status for M9 and M10
old_table_m9 = "| M9 | Aktiviteler + navigasyon | MainActivity, LauncherActivity, Routes, onboarding | DEVAM |"
new_table_m9 = "| M9 | Aktiviteler + navigasyon | MainActivity, LauncherActivity, Routes, onboarding | TAMAM |"

old_table_m10 = "| M10 | Global ölü kod süpürmesi | detekt raporu + cross-module unused sembol taraması | BEKLEMEDE |"
new_table_m10 = "| M10 | Global ölü kod süpürmesi | detekt raporu + cross-module unused sembol taraması | DEVAM |"

text = text.replace(old_table_m9, new_table_m9)
text = text.replace(old_table_m10, new_table_m10)

log_entry = """

### 2026-07-26 — M9 (Antigravity)

Kapsam: `MainActivity.kt`, `LauncherActivity.kt`, `presentation/navigation/AppNavigation.kt` (`Routes` nesnesi dahil), `presentation/ui/screens/OnboardingScreen.kt` & `OnboardingModels.kt` & `OnboardingStepContent.kt`. Talimat gereği alt-agent SPAWN EDİLMEDİ, tamamı şef tarafından tek oturumda doğrudan Read/Grep/Edit ile işlendi.

**Silinen semboller (ölü kod, 0 caller kanıtlandı):**
- `MainActivity.openBugReport()` (`MainActivity.kt:129-148`) — hiçbir UI, menü veya event handler tarafından çağrılmayan ölü private metod (0-caller kanıtlandı). ~20 satır silindi.

**Doğrulanan sağlam desenler (dokunulmadı):**
- `MainActivity.kt`: `installSplashScreen()` çağrısının `super.onCreate()` öncesinde çağrılması (D234 gri başlık çubuğu fix'i), `Routes.isValid(route)` whitelist güvenlik kontrolü ile dışarıdan Intent yönlendirme koruması, `applyOpenCategoryIntent` ve `scanApps()` reaktif ve güvenli.
- `LauncherActivity.kt`: Launcher olarak `HOME`+`DEFAULT` intent-filter yapılandırması, `WidgetHostManager` ve `ActivityResultLauncher` ile güvenli widget bağlama akışı (`widgetBindLauncher`, `widgetConfigureLauncher`), `StartupHealthPrefs.markReady` soğuk başlangıç takibi, `checkSafeMode` güvenlik mekanizması.
- `AppNavigation.kt` & `Routes`: `Routes.ALL` whitelist doğrulama seti, `Routes.fromTickerRoute` TEK nokta route dönüştürücüsü, `AppNavigation` içindeki `LaunchedEffect(externalRoute)` ile güvenli ve reaktif rota yönetimi.
- `OnboardingScreen`: `OnboardingModels`, `OnboardingStepContent` adımları, `AppPrefs.isOnboardingDone` ve `markOnboardingDone` bayrak yönetimi tam uyumlu ve sağlam.

**Build:**
- `compileDebugKotlin` doğrulandı: **BUILD SUCCESSFUL in 2m 21s**, 0 hata.

**Sayılar:** silinen 1 ölü private metod (`MainActivity.openBugReport`), bağlanan 0 kopuk halka, 0 ertelenen bulgu.

**Değişen dosyalar:**
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/MainActivity.kt` (-20 satır, `openBugReport` silindi)

**Sonraki modül:** M10 — Global ölü kod süpürmesi (detekt raporu + cross-module unused sembol taraması).
"""

text += log_entry
p.write_text(text, encoding="utf-8")
print("Updated KOD_TARAMA_DURUM.md for M9 successfully")
