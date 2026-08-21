# Home V2 — Ana Ekran Yeniden Tasarımı

**Tarih:** 2026-08-21 · **Durum:** Uygulandı (feature/home-v2)

## 1. Amaç ve ürün vizyonu

AppOrganizer'ın ana ekranı "uygulamaları senin yerine düzenleyen akıllı launcher" fikrinin vitrinidir.
Kullanıcı ana ekranda üç şeyi anında yapabilmelidir:

1. **Doğru uygulamayı bulmak** — klasörler otomatik oluşur; arama tek noktadan (çekmece) girer.
2. **Bağlamına uygun uygulamaya tek dokunuşla ulaşmak** — dock, saat dilimine göre akıllı öneriler taşır (mevcut `buildContextualDockPackages` + UsageStats slot motoru korunur).
3. **Günün dijital nabzını görmek** — pulse skoru, görev ilerlemesi ve bildirim yükü göze batmadan üst şeritte yaşar.

## 2. Eski ekranın sorunları (SISTEM_INCELEME_RAPORU P1.1)

- `HomeScreen.kt` ~1700 satır; ~30 ayrı `collectAsState` + ~15 yerel pref state'i tek composable'da.
- Bölümler (saat, grid, dock, banner, ticker) iç içe; yeniden kullanım ve test zor.
- Davranış mantığı (klasör önizleme sırası, rozet toplama, dock çözünürlüğü) UI içinde dağınık.

## 3. Yeni mimari

```
presentation/ui/launcher/homev2/
├── HomeV2Screen.kt        — kompozisyon kökü (yalnız wiring, ~200 satır)
├── HomeV2State.kt         — @Immutable state modelleri + saf HomeV2Assembler
├── FolderQuickLaunch.kt   — saf hızlı-başlat seçici (roadmap: swipe-up ile en sık uygulama)
├── ClockHeaderV2.kt       — saat + tarih + nabz/görev şeridi
├── FolderGridV2.kt        — sayfalanmış adaptif klasör grid'i
├── FolderTileV2.kt        — tek klasör kartı (önizleme, rozet, hızlı başlat)
└── DockBarV2.kt           — buzlu-cam dock (mevcut contextual motor)
```

Kurallar:
- **Tek state girişi:** Ekran `HomeV2Assembler.assemble(...)` ürününü render eder; assembler saf
  fonksiyondur ve birim testleriyle kilitlenir.
- **Altyapı korunur:** `HomeShell` (jest/IME/z-order), `AllAppsDrawer`, `FolderScreen`,
  `AppIconView`, `HomeScreenPageIndicator` yeniden kullanılır; davranış contract'leri bozulmaz.
- **ViewModel'e dokunulmaz:** mevcut StateFlow'lar tüketilir; yeni akış eklenmez.

## 4. Yeni özellikler

| # | Özellik | Kaynak |
|---|---------|--------|
| 1 | **Klasörde hızlı başlat** — klasör kartında yukarı kaydırma, klasörün en sık kullanılan uygulamasını başlatır | README roadmap (yeni) |
| 2 | **Klasör bildirim halkası** — klasör kartında acil bildirim vurgusu (önem bazlı) | yeni |
| 3 | **Nabız şeridi** — pulse skoru + görev ilerlemesi tek kompakt çip satırında | yeni (Hero'nun özeti) |
| 4 | **Adaptif grid** — sütun sayısı ekran genişliğine, kart boyutu sayfa ayarına göre ölçeklenir | yeni |
| 5 | **Tek arama girişi** — arama çubuğu çekmeceye odaklı açılır; ana ekran sade kalır (Niagara yaklaşımı) | yeni |

## 5. Kapsam dışı (HomeV2 v2'ye)

- Widget alanı ve Hero Dashboard sayfası (ayrı sürümde geri taşınacak)
- Düzenleme merkezi ve klasör sürükleme
- Duvar kağıdı/arka plan stil ayarlarının tümü (tema + Material You korunur)

## 6. Test sözleşmesi

- `HomeV2AssemblerTest` — klasör önizleme sırası, rozet toplama, banner önceliği, boş durumlar
- `FolderQuickLaunchResolverTest` — kullanım sıralaması, gizli uygulama eleme, eşitlik kırma
- Mevcut 1411 unit test yeşil kalmalı (CI kanıtı bu döngüde tekrar alınacak)
