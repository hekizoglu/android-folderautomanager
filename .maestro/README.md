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
| `04_folder_interaction.yaml` | Klasör tıklama (FolderSheet), uzun basış context menü |
| `05_dock_edit.yaml` | Dock uzun basış, DockEditSheet açma/kapama |
| `06_notification_badge.yaml` | Badge görünürlük: HomeScreen + AllAppsDrawer + sayfa 2 |
| `07_home_v2_visual_check.yaml` | Home V2 görsel kanıt: 6 ekran görüntüsü (açılış, çekmece, uzun basma menüsü, header) — taşma/hizalama insan gözüyle incelenir |
| `08_full_app_smoke.yaml` | Tam uygulama smoke: çekmece aç/kapa, yönetim menüsü, sayfa geçişi, dock'tan uygulama başlatma, eve dönüş — 6 kanıt ekran görüntüsü |

## Görsel Doğrulama (07) Nasıl Kullanılır?

1. `maestro test .maestro/07_home_v2_visual_check.yaml` çalıştırılır.
2. Ekran görüntüleri `~/.maestro/tests/<run-id>/` altına `homev2_01..06` adlarıyla kaydedilir.
3. Her görüntü için kontrol listesi:
   - Metinler kart/ekran dışına taşıyor mu? (özellikle uzun klasör adları)
   - Klasör kartları eşit boyutta mı, önizleme ikonları hizalı mı?
   - Dock pill'i ekran genişliğine sığıyor mu, ikonlar kesiliyor mu?
   - Saat başlığı büyük font ölçeğinde (cihaz ayarı %150/%200 yapıp tekrar koşun) taşıyor mu?
   - Uzun basma menüsü ve çekmece animasyonları düzgün kapanıyor mu?
4. Sorun bulunursa Robolectric görsel testleriyle (`HomeV2VisualUiTest`) reproduce edilir,
   düzeltme sonrası hem otomatik testler hem bu akış tekrar koşulur.

## Notlar

- APK emülatörde kurulu olmalı: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Launcher olarak set edilmiş olmalı
- Test sonuçları: `~/.maestro/tests/` klasöründe
