# Local Denetim Raporu

> Son dongu: `2026-06-27 03:20`
> Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasina tasinir.

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK | 1 | Gesture handling tutarsizligi |
| ORTA | 2 | Pointer tuketimi ve drawer kapanis davranisi |
| DUSUK | 2 | Threshold standardizasyonu ve OEM kenar durumu |
| INFO | 1 | Gelecege donuk model notu |
| TOPLAM | 6 | |

---

## 1. Launcher / Drawer Gesture Denetimi

### [YUKSEK] [S1] FolderTile ve HomeScreen'de cakisan swipe gesture handling
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/FolderTile.kt`
**Sorun:** `FolderTile` icindeki swipe-up algilama ile `HomeScreen` kok gesture akisi halen iki ayri noktada yonetiliyor.
**Etki:** Bazi cihazlarda klasor uzerinde yukari kaydirma davranisi hala tutarsiz hissedilebilir.
**Oneri:** Swipe-up davranisini tek gesture kaynagina indir veya parent-child gesture kontratini netlestir.

### [ORTA] [A6] AllAppsDrawer drag gesture scroll ile cakisiyor
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/AllAppsDrawer.kt`
**Sorun:** Drawer kapanis drag'i liste scroll'u ile ayni alanda calisiyor; liste ustteyken davranis daha acik kurala baglanmali.
**Etki:** Kullanicinin drawer'i mi listeyi mi hareket ettirdigi her zaman net olmayabilir.
**Oneri:** `LazyListState` ile ust konum kontrolu yapip kapanis drag'ini yalniz uygun durumda aktiflestir.

### [ORTA] [S3] Birden fazla vertical drag detector pointer consumption net degil
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreen.kt`
**Sorun:** `detectTapGestures`, `detectVerticalDragGestures` ve `nestedScrollConnection` birlikte calisiyor.
**Etki:** Drawer acma/kapama threshold'lari cihazdan cihaza farkli hissedilebilir.
**Oneri:** Tek merkezli gesture stratejisi veya daha net pointer consumption kurali uygula.

### [DUSUK] [S6] Swipe threshold degerleri ekranlar arasinda standardize degil
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/FolderTile.kt`
**Sorun:** Klasor, ana ekran ve drawer alanlarinda farkli threshold degerleri kullaniliyor.
**Etki:** Benzer gesture'lar farkli hassasiyetle calisiyor.
**Oneri:** Ortak dp bazli threshold sabitleri tanimla.

---

## 2. Izin ve Paket Gorunurlugu Denetimi

### [DUSUK] [P11] OEM-specific package visibility restrictions icin ek runtime koruma yok
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/utils/PackageManagerHelper.kt`
**Sorun:** Samsung/Xiaomi gibi OEM varyasyonlari icin ek fallback denetimi bulunmuyor.
**Etki:** Bazi sistem uygulamalari OEM kisitlarinda eksik gorunebilir.
**Oneri:** OEM tespitiyle kontrollu fallback veya tanisal log ekle.

---

## 3. Gelecek Notu

### [INFO] [S8] Kilitli/kilitsiz kategori davranisi mevcut degil
**Dosya:** `app/src/main/java/com/armutlu/apporganizer/domain/models/Category.kt`
**Sorun:** `isSystemCategory` var ama ayrica `locked` davranisi yok.
**Etki:** Bu ozellik denetim kapsaminda henuz uygulanmamis.
**Oneri:** Ihtiyac netlesirse model ve UI etkileşim kurallariyla birlikte ele al.

---

*Denetim tarihi: 2026-06-27 | Denetim tipi: Tam local denetim + manuel checklist senkronizasyonu*
