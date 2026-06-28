# Maestro UI Test Flows

AppOrganizer için Maestro tabanlı UI test akışları.

## Kurulum

```bash
# macOS/Linux
curl -Ls "https://get.maestro.mobile.dev" | bash

# Windows (Scoop)
scoop install maestro
```

## Çalıştırma

```bash
# Tek flow çalıştır
maestro test .maestro/01_home_screen.yaml

# Tüm flowları sırayla çalıştır
maestro test .maestro/

# Emülatör üzerinde
maestro test --device emulator-5554 .maestro/02_all_apps_drawer.yaml
```

## Test Akışları

| Flow | Kapsam |
|------|--------|
| `01_home_screen.yaml` | HomeScreen yükleme, klasör listesi |
| `02_all_apps_drawer.yaml` | Swipe ile drawer açma, arama |
| `03_settings_navigation.yaml` | Uzun basış menü, Ayarlar navigasyonu |

## Notlar

- APK emülatörde kurulu olmalı: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Launcher olarak set edilmiş olmalı
- Test sonuçları: `~/.maestro/tests/` klasöründe
