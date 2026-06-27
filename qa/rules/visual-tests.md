# Görsel Regresyon Testleri

> Kaynak: [Android Developers — Screenshot Testing](https://developer.android.com/training/testing/ui-tests/screenshot), [Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing)

## Hedef

Önemli ekranların referans görselini sakla. Sonraki build’de aynı ekranı tekrar çeker. Pixel farkı varsa raporlar.

---

## Klasör Yapısı

```
screenshots/
  phone-small/        # 360dp genişlik
  phone-normal/       # 411dp genişlik (Pixel 6)
  tablet/             # 600dp+
  dark-mode/          # Sistem koyu tema
  large-font/         # Font scale 1.3+
  turkish/            # TR locale
  english/            # EN locale
```

---

## Araç Seçimi

| Araç | Kullanım |
|------|----------|
| Compose Preview Screenshot | Compose ekranları — Google resmi |
| Roborazzi | Robolectric tabanlı, JVM testi |
| Paparazzi | Emulator olmadan render |
| Paparazzi | Android ekranlarını emülatörsüz test eder |

---

## Test Edilecek Ekranlar

| Ekran | Görsel öğe |
|-------|------------|
| HomeScreen | İkon grid, dock, page indicator |
| AllAppsDrawer | Liste, arama çubuğu, history chips |
| FolderTile | Klasör ikonu, swipe hint, içerik |
| AppListScreen | Kategori çipleri, liste, filtreler |
| SettingsScreen | Toggle, satır, dialog |
| OnboardingScreen | Adım geçişi, butonlar |
| CategoryPicker | Bottom sheet, seçim durumu |
| WidgetArea | Drag handle, yeniden sıralama |

---

## Komutlar

```bash
# Compose screenshot test
./gradlew validateDebugScreenshotTest

# Roborazzi (View/XML tabanlı)
./gradlew verifyRoborazziDebug
```

---

*Kural seti: qa/rules/visual-tests.md | Puan: 8/10*
