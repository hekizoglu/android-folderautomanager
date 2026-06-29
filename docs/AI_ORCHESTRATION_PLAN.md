# AI Orkestrasyon Planı

## Amaç

Raporlar, arama ayarları, premium arama alanı ve ileride birleşik arama motoru çalışmalarını üç AI arasında çakışmadan yürütmek.

## Kaynak Raporlar

- `docs/UX_SEARCH_REPORTS_SPEC.md`: UX akışı, kabul kriterleri, ekran davranışları.
- `docs/search-architecture-report.md`: birleşik arama mimarisi, Room FTS5, ContactsContract, MediaStore + SAF.

## Ana Kural

- Aynı anda birden fazla AI aynı dosyayı değiştirmeyecek.
- Her AI sadece kendi paketindeki dosyaları inceleyecek.
- Büyük refactor yok; küçük, build alınabilir teslimler yapılacak.
- Dosya araması ilk sprintte gerçek indeksleme ile zorlanmayacak.

## Paket 1 — Codex

### Sorumluluk

- Repo orkestrasyonu.
- Build doğrulaması.
- Navigation, ayarlar ve raporlar entegrasyonu.
- Çakışma çözümü ve final merge.

### Dosyalar

- `app/src/main/java/com/armutlu/apporganizer/presentation/navigation/AppNavigation.kt`
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/MainActivity.kt`
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/ReportsCenterScreen.kt`
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SearchSettingsScreen.kt`
- `app/src/main/java/com/armutlu/apporganizer/utils/AppPrefs.kt`

### İlk Hedef

- Raporlar merkezi ve arama ayarları ekranlarının build geçen iskeletini korumak.
- Ana ekran istatistik tıklamalarını ilgili raporlara yönlendirmek.

## Paket 2 — Claude

### Sorumluluk

- UX kalite denetimi.
- Premium search bar davranışı.
- Drag/snap davranışının minimal UI planı.

### Dosyalar

- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreen.kt`
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreenOverlays.kt`
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/AllAppsDrawer.kt`

### İlk Hedef

- Mevcut arama girişinin görsel olarak daha premium hale getirilmesi.
- Drag/snap için önce `AppPrefs` konum kaydı ve statik konum seçimi; serbest sürükleme sonraya bırakılır.

## Paket 3 — DeepSeek Pro

### Sorumluluk

- Birleşik arama backend teknik planı.
- Room FTS5 veri modeli.
- Indexer ve repository sınıf sınırları.

### Dosyalar

- `app/src/main/java/com/armutlu/apporganizer/data/**`
- `app/src/main/java/com/armutlu/apporganizer/domain/**`
- `app/src/main/java/com/armutlu/apporganizer/utils/**`

### İlk Hedef

- Sadece App + Category kaynakları için FTS5 tasarımını netleştirmek.
- Contacts ve Files için izin/opt-in akışını tasarımda bırakmak; kodlamaya hemen başlamamak.

## Teslim Sırası

1. Codex: mevcut iskeletin build doğrulaması.
2. Claude: search bar UX önerisini dosya bazlı patch planına indirme.
3. DeepSeek Pro: Room FTS5 için entity/dao/repository planı.
4. Codex: çakışmasız ilk implementation commit’i.
5. Codex: build + local denetim + rapora işleme.

## Şimdilik Yapılmayacaklar

- Gerçek dosya indeksleme.
- `MANAGE_EXTERNAL_STORAGE`.
- Kişi araması için erken izin ekranı.
- Büyük navigation refactor.
- AllAppsDrawer tamamen yeniden yazımı.

## Doğrulama

- Her küçük teslimden sonra `.\gradlew.bat :app:compileDebugKotlin`.
- UI etkileyen teslimden sonra debug APK.
- Denetim raporuna tarih/saat ile sonuç ekleme.
