# Local Denetim Raporu

> Son denetim: `2026-06-26 18:27`
> Bu rapor hem otomatik statik taramayi hem de manuel semantik UI, Settings ve aksiyon denetimini icerir.

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu yok |
| YUKSEK | 4 | Yaniltici veya yikici aksiyon akisi |
| ORTA | 3 | Etiket-davranis ve aciklama tutarsizliklari |
| DUSUK | 0 | Acik dusuk bulgu yok |
| TOPLAM | 7 | |

---

## Otomatik Denetim Sonucu

- `scripts/audit.ps1` ile yapilan statik tarama temiz gecti.
- State, prefs, locale, cache ve benzeri otomatik taranabilen kurallarda acik bulgu yok.

---

## Manuel Semantik UI ve Settings Denetimi

### [YUKSEK] [F4] Yikici ayar aksiyonu dogrudan calisiyor ve navigasyon gibi gorunuyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsAppsSection.kt` (satir 108-112)
**Sorun:** `Tum Kategorileri Sifirla` satiri `SettingsButtonRow` ile sunuluyor; satirda chevron var ve bu gorunum navigasyon beklentisi yaratirken tiklama aninda tum atamalari sifirlayip yeniden siniflandirmayi baslatiyor. Ustelik onay veya geri alma yok.
**Etki:** Kullanici ayar detayina gidecegini sanip yanlislikla geri alinmasi zor toplu islem baslatabilir.
**Oneri:** Bu aksiyonu onay dialogu ile koru; satir tipini komut aksiyonu olarak ayir veya etiketini daha acik hale getir.

### [YUKSEK] [F2] Dock sifirlama satiri anlik islem yapiyor ama navigasyon gibi sunuluyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsScreen.kt` (satir 262-267)
**Sorun:** `Varsayilanlara Sifirla` satiri `SettingsButtonRow` kullaniyor ve chevron gosterdigi icin detay ekranina gidecek izlenimi veriyor; gercekte dock listesini aninda bosaltiyor.
**Etki:** Kullanici yanlis beklentiyle dokunup dock duzenini istemeden sifirlayabilir.
**Oneri:** Onay dialogu ekle veya bu islemi ikincil buton ya da destructive action deseniyle ayir.

### [YUKSEK] [F1] `Yeniden Siniflandir` etiketi gercek veri etkisini eksik anlatiyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/AppListScreen.kt` (satir 81-83)
**Sorun:** Menu maddesi `Yeniden Siniflandir` diyor; fakat tiklandiginda `resetAndReclassifyAllApps()` cagriliyor ve once tum kategoriler sifirlaniyor, sonra yeniden siniflandirma basliyor.
**Etki:** Kullanici mevcut manuel kategori duzenlemelerinin korunacagini dusunebilir; oysa tum atamalar silinir.
**Oneri:** Etiketi `Kategorileri Sifirla ve Yeniden Siniflandir` gibi aciklastir veya islem oncesi onay al.

**Davranis dogrulamasi:** `app/src/main/java/com/armutlu/apporganizer/presentation/viewmodel/AppListViewModel.kt` (satir 462-475) once `resetAllCategories()` sonra yeniden siniflandirma calistiriyor.

### [YUKSEK] [F4] `Geri Yukle` akisi secilen yedegi dogrudan iceri aktarip mevcut veriyi uzerine yaziyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsBackupAboutSection.kt` (satir 90-99)
**Sorun:** Dosya secildikten sonra JSON icerigi dogrudan `importBackup()` ile uygulanıyor; onay, ozet veya once-ne-olacak aciklamasi yok.
**Etki:** Kullanici yanlis dosyayi secerse kategori, gizlilik ve kullanim verileri fark etmeden degisebilir.
**Oneri:** Iceri aktarmadan once kac uygulamanin etkilenecegini gosteren bir onay adimi ekle.

**Davranis dogrulamasi:** `app/src/main/java/com/armutlu/apporganizer/utils/BackupManager.kt` (satir 69-90) secilen JSON kayitlarini mevcut uygulamalar uzerine yaziyor.

### [ORTA] [F3] `Izin Ver` etiketi dogrudan izin vermiyor, sistem ayar ekranini aciyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsScreen.kt` (satir 194-203)
**Sorun:** `Izin Ver` butonu izni uygulama icinde vermiyor; yalnizca `ACTION_NOTIFICATION_LISTENER_SETTINGS` ekranina yonlendiriyor.
**Etki:** Kullanici butona basinca iznin aninda verilecegini dusunebilir; sistem ayarinda ikinci adim gerektigini ancak sonra fark eder.
**Oneri:** Etiketi `Ayarlari Ac` veya `Bildirim Erisimini Ac` gibi daha dogru bir metinle degistir.

### [ORTA] [G4] Otomatik yedekleme aciklamasi gercek calisma modeliyle celisiyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsBackupAboutSection.kt` (satir 62-68)
**Sorun:** Subtitle `Uygulama acildiginda otomatik JSON yedegi al` diyor; ancak kod haftalik periyodik yedekleme planliyor.
**Etki:** Kullanici acilis bazli yedekleme beklerken gercekte yedekleme haftalik arka plan gorevi olarak calisiyor.
**Oneri:** Metni haftalik periyodik yedeklemeyi dogru tarif edecek sekilde guncelle.

**Davranis dogrulamasi:** `app/src/main/java/com/armutlu/apporganizer/workers/BackupWorker.kt` (satir 64) uzerinde `PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)` kullaniliyor.

### [ORTA] [F1] `Klasor Onizleme` etiketi gercekte onizlemeyi degil swipe hint metnini kontrol ediyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsHomeScreenSection.kt` (satir 130-137)
**Sorun:** Ayar `Klasorde en cok kullanilan uygulamayi goster` diye etiketlenmis; fakat bagli preference `folderSwipeHintEnabled` ve UI davranisi klasor altinda `^ UygulamaAdi` ipucu metnini acip kapatiyor.
**Etki:** Kullanici ikon onizlemesi veya klasor icinde gorsel preview beklerken sadece metinsel swipe ipucunun degistigini gorur.
**Oneri:** Etiketi `Yukari Kaydirma Ipucu` benzeri dogrudan davranisi anlatan bir metinle degistir veya davranisi gercek onizleme haline getir.

**Davranis dogrulamasi:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/FolderTile.kt` (satir 222-231) uzerinde `^ ${topApp.appName}` ipucu gosteriliyor.

### [ORTA] [F2] `SettingsButtonRow` bileseni tum tiklanabilir satirlari navigasyon gibi gostermeye zorluyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/SettingsComponents.kt` (satir 79-100)
**Sorun:** Bilesen her durumda sagda `ChevronRight` gosterdigi icin satirin detay sayfasina gidecegi anlami uretiliyor; oysa ayni bilesen hem navigasyon hem de anlik komutlar icin kullaniliyor.
**Etki:** Settings genelinde kullanici zihinsel modeli bozuluyor; hangi satirin ekran acacagi, hangisinin anlik islem yapacagi ayirt edilmesi zorlasiyor.
**Oneri:** `SettingsButtonRow` ile `SettingsActionRow` ayrimi yap veya chevron'u yalnizca gercek navigasyon satirlarinda goster.

---

## Sonuc

- Otomatik kod denetimi temiz.
- Manuel semantik denetimde ozellikle `SettingsScreen`, `SettingsAppsSection`, `SettingsBackupAboutSection`, `SettingsHomeScreenSection` ve `AppListScreen` icinde 4 yuksek, 3 orta seviye bulgu var.
- Bulgular agirlikli olarak buton veya satir adinin yaptigi islemi tam anlatmamasi ve yikici aksiyonlarin yeterince guvenli sunulmamasi etrafinda toplaniyor.

---

*Denetim tarihi: 2026-06-26 | Denetim tipi: otomatik + kapsamli manuel semantik UI turu*
