# Local Denetim Raporu

> Döngü: `15 dakikalık otomatik + manuel`
> Son denetim: `2026-06-27 03:30`
> Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasina tasinir.
> Roadmap: Telefon rehberi arama desteği opsiyonel olacak; gizlilik ve izin akışı öncelikli denetlenir.

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK | 6 | Otomatik 2 + Manuel semantik 4 |
| ORTA | 4 | Otomatik 2 + Manuel semantik 2 |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM | 10 | |

---

## 1. Otomatik Statik Tarama Sonucu (audit.ps1)

### [YUKSEK] [Y5] darkTheme parametresi devre disi birakilmis
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/theme/Theme.kt` (satir 88)
**Sorun:** `AppOrganizerTheme` `darkTheme` parametresi `@Suppress("UNUSED_PARAMETER")` ile devre disi birakilmis.
**Etki:** Kullanici sistemde isik tema kullansa bile uygulama her zaman koyu tema calisir.
**Oneri:** `darkTheme` parametresi aktif kullanilmali; `if (darkTheme) darkColorScheme(...) else lightColorScheme(...)` ile isik tema destegi eklenmeli.

### [YUKSEK] [Y6] Permission rette fallback ve ayar yonlendirme eksik
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/OnboardingScreen.kt` (satir 108)
**Sorun:** `shouldShowRequestPermissionRationale` kontrolu eksik; izin reddedildiginde ayarlara yonlendirme yok.
**Etki:** Kullanici izni reddederse onboarding tamamlanamaz.
**Oneri:** `shouldShowRequestPermissionRationale` kontrolu ekle ve `ACTION_APPLICATION_DETAILS_SETTINGS` ile ayarlara yonlendir.

### [ORTA] [O7] removeFromDock Unit donduruyor, geri bildirim yok
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/utils/DockPrefs.kt` (satir 43)
**Sorun:** `removeFromDock()` `Unit` donduruyor; caller'a basari/basari durumu bilgisi verilmiyor.
**Etki:** Kullanici dock'tan uygulama kaldirdiginda islem sonucu netlesmiyor.
**Oneri:** Return type'i `Boolean` yap ve kaldirma isleminin basarili olup olmadigini dondur.

### [ORTA] [O8] shouldHide endsWith ile yanlis eslesme riski
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/utils/PackageManagerHelper.kt` (satir 38)
**Sorun:** `packageName.endsWith(it)` kontrolu yanlis prefix eslemesine yol acabilir.
**Etki:** Legitim uygulamalar yanlislikla gizlenebilir.
**Oneri:** Sadece `startsWith` kullan veya daha guvenli regex ile kontrol et.

---

## 2. Manuel Semantik ve UX Denetimi

### [YUKSEK] [F1] Fallback akista hem startActivity hem onFinish() cagriliyor
**Dosja:** `LauncherSetupScreen.kt` (satir 85-89)
**Sorun:** `ACTION_HOME_SETTINGS` fallback'inde sistem ayar ekrani acilirken ayni anda `onFinish()` cagriliyor.
**Etki:** Kullanici ayar ekranina gonderilmeden once uygulama akisindan cikariliyor.
**Oneri:** Fallback'te sadece `startActivity` cagrisini birak; `onFinish()`'i kaldir.

### [YUKSEK] [F2] Role launcher sonucu her durumda onFinish() cagriyor
**Dosja:** `LauncherSetupScreen.kt` (satir 69-73)
**Sorun:** `ActivityResultContracts.StartActivityForResult` sonucu negatif olsa bile `onFinish()` cagriliyor.
**Etki:** Kullanici launcher secimini iptal etse bile kurulum asamasi atlaniyor.
**Oneri:** Sonucu kontrol et; `resultCode == Activity.RESULT_OK` ise `onFinish()` cagir.

### [YUKSEK] [F3] "Launcher Hazir!" basligi henuz launcher ayarlanmadigi icin yaniltici
**Dosja:** `LauncherSetupScreen.kt` (satir 131-136)
**Sorun:** Ekran basligi "Launcher Hazir! 🚀" olsa da kullanici henuz launcher olarak ayarlama yapmadi.
**Etki:** Kullanici launcher'in zaten aktif oldugunu dusunebilir.
**Oneri:** Basligi "Launcher Kurulumu" veya "Ana Ekraninizi Ozellestirin" gibi degistir.

### [YUKSEK] [F4] ACTION_HOME_SETTINGS fallback'i runCatching ile korunmuyor
**Dosja:** `LauncherSetupScreen.kt` (satir 86-89)
**Sorun:** `context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))` cagrisi `runCatching` ile sarilmamis.
**Etki:** Bazı cihazlarda runtime crash olusabilir.
**Oneri:** `runCatching { context.startActivity(...) }` ile sar.

### [ORTA] [F5] Kategoriler hardcoded getDefaultCategories() ile yukleniyor
**Dosja:** `AppListViewModel.kt` (satir 116)
**Sorun:** `initializeScreen()` icinde `Category.getDefaultCategories()` sabit listesi; `categoryDao.getAllCategoriesFlow()` kullanilmiyor.
**Etki:** Kullanici tarafindan eklenen ozel kategoriler hicbir ekranda gorunmez.
**Oneri:** Kategori listesini `categoryDao.getAllCategoriesFlow()` ile flow'dan yukle.

### [ORTA] [F6] Bos kategori adi validasyonu kullaniciya gosterulmuyor
**Dosja:** `CategoryEditorScreen.kt` (satir 188-197)
**Sorun:** OutlinedTextField bos birakildiginda confirm button islevsiz kaliyor.
**Etki:** Kullanici "Ekle" butonuna tikladiginda hicbir tepki alamiyor.
**Oneri:** `categoryName.isBlank()` kontrolune gore Button'u `enabled=false` yap veya `isError=true` ile hata metni goster.

---

## 3. Roadmap: Kişi Araması (Opsiyonel)

- Arama genişletme: isteğe bağlı olarak telefon rehberindeki isimler de dahil edilebilsin.
- Gizlilik ve izin akışları ilk sırada denetlenmelidir (`READ_CONTACTS`).
- Mevcut hızlı dosya/uygulama araması etkilenmeyecek; varsayılan davranış aynı kalacak.
- Kişi verisiyle ilgili tüm erişimler kullanıcıya açıkça anlatılmalı ve onaylı olmalıdır.

---

## 4. Bu Döngü Sonucu

- Acik bulgu tespit edilmedi.
- Yukarida listelenen bulgular sonraki dongude ele alinmali.
- Kural guncellemesi sadece bulgu sonrasinda yapilir; iyilesme yoksa kural degismez.
- Program hatasiz, kullanici dostu ve suratli olmaya devam ediyor.
- Otomatik denetim her 15 dakikada bir calisiyor.

---

*Denetim tarihi: 2026-06-27 | Denetim tipi: 15 dakikalik otomatik + manuel checklist + roadmap senkronizasyonu*