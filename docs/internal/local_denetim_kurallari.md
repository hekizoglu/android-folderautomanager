# Local Denetim Kurallari

> Bu dosya, Android uygulamasi icin profesyonel local kod denetimi kurallarini tanimlar.
> Kapsam son kullanicinin gordugu tum katmanlardir: UI, ViewModel, state, settings, flow, data flow ve geri bildirim akislari.
> Denetim iki parcadan olusur:
> 1. Otomatik statik denetim
> 2. Manuel veya yari otomatik semantik UX denetimi

---

## 1. Denetim Amaci

- Son kullaniciya yansiyan mantik, UI ve davranis hatalarini erken yakalamak
- Ayarlar ekranlari ve eylem butonlarinda anlam-davranis tutarliligini korumak
- Accessibility, net etiketleme, geri bildirim ve hata onleme ilkelerini standartlastirmak
- Her dongude tekrar edilebilir bir denetim standardi saglamak

---

## 2. Kapsam

- Kod kapsami: `app/src/main/java/**`
- Haric tutulanlar: `test/`, `androidTest/`, ucuncu parti kutuphane kaynaklari
- Birincil odak:
  - UI state ve recomposition davranisi
  - Ayarlar ve preference akisleri
  - Buton, menu, dialog ve bottom sheet aksiyonlari
  - Arama, siralama, filtreleme, cache ve locale davranisi
  - Accessibility ve metin-eylem tutarliligi

---

## 3. Arastirmaya Dayali Temel Ilkeler

Bu kurallar resmi Android ve Material kaynaklarindan turetilmistir:

- Settings ekranlari kullanicinin uygulama davranisini kontrol ettigi yerlerdir; acik, kesin ve gruplandirilmis olmali.
- Sik kullanilan aksiyonlar settings icine saklanmamali; ozellige yakin yerde olmali.
- Buton etiketi, tiklandiginda olacak eylemi dogrudan anlatmali.
- Labels kisa, acik, tek anlamli ve amaci tarif eder nitelikte olmali.
- Accessibility icin interaktif elemanlarin anlami, rolu ve gerekiyorsa durumu okunabilir olmali.
- Sistem ayarina yonlendiren akislar, Android'in ayri izin ekranlariyla uyumlu ve acik isimlendirilmis olmali.
- Test yalnizca statik analizle bitmez; TalkBack ve manuel akis kontrolu de denetimin parcasi olmalidir.

---

## 4. Denetim Kategorileri

### A. Mantik Hatalari

| Kural | Aciklama |
|-------|----------|
| A1 | State degiskenleri `remember`, `mutableStateOf`, `StateFlow` veya benzeri yapiyla dogru yonetiliyor mu? |
| A2 | `remember` key parametreleri eksik, fazla veya yanlis mi? |
| A3 | `LaunchedEffect`, `DisposableEffect`, coroutine ve timer akislarinda race condition riski var mi? |
| A4 | SharedPreferences veya benzeri ayar depolama okuma-yazma tutarliligi korunuyor mu? |
| A5 | Flow veya StateFlow collect edilip UI ile dogru senkronize oluyor mu? |
| A6 | Null safety eksigi, unsafe call veya bos deger riski var mi? |
| A7 | Kosullar, erken donusler ve durum gecisleri dogru mu? |
| A8 | Dizi, sayfa ve liste indexleri sinir guvenli mi? |

### B. Baglam ve Veri Kaynagi Hatalari

| Kural | Aciklama |
|-------|----------|
| B1 | Ayni veri kaynagi birden fazla yerde gereksiz okunuyor mu? |
| B2 | Composable icinde pahali sistem cagri, prefs cagri veya package manager sorgusu tekrarli calisiyor mu? |
| B3 | Cache key formatlari ekranlar arasinda tutarli mi? |
| B4 | Locale kullanimi tum arama, siralama ve karsilastirmalarda tutarli mi? |
| B5 | Singleton veya global mutable state yan etki uretiyor mu? |

### C. UI ve UX Hatalari

| Kural | Aciklama |
|-------|----------|
| C1 | Kullanici aksiyonu dogru hedefe bagli mi? |
| C2 | Loading, bos, hata ve izin durumlari dogru ve zamaninda gosteriliyor mu? |
| C3 | Toggle ve ayar degisiklikleri UI'a aninda yansiyor mu? |
| C4 | Ikon ve gorsel guncellemeler dogru invalidation ile yenileniyor mu? |
| C5 | Accessibility semantics, contentDescription ve okuma sirasi eksigi var mi? |
| C6 | Buton, menu veya satir etiketi, gercek davranisi acik ve dogru tarif ediyor mu? |
| C7 | Tehlikeli veya geri alinmasi zor aksiyonlar yeterince acik isimlendirilmis mi? |
| C8 | Subtitle, helper text veya toast metni gercek davranisla tutarli mi? |
| C9 | Disabled, loading veya secili durumlar kullaniciya gorunur ve anlasilir mi? |
| C10 | Swipe, drag, nested scroll ve dismiss gesture'lari birbirini bozuyor mu? |

### D. Performans Hatalari

| Kural | Aciklama |
|-------|----------|
| D1 | `LazyColumn` ve `LazyRow` icin `key` eksigi var mi? |
| D2 | `remember`, `derivedStateOf`, `produceState` veya memoization ihtiyaci kacirilmis mi? |
| D3 | Gereksiz recomposition tetikleyen state yapilari var mi? |
| D4 | IO islemleri ana thread uzerinde mi? |

### E. Kod Sagligi

| Kural | Aciklama |
|-------|----------|
| E1 | Kullanilmayan parametre, degisken veya olu kod var mi? |
| E2 | Kirik import, bozuk referans veya stale API kullanimi var mi? |
| E3 | Hardcoded string, renk veya boyut merkezi yonetimden kacmis mi? |
| E4 | Log, toast ve hata mesaji anlamli, tutarli ve kullanici baglamina uygun mu? |
| E5 | Kullanilmayan class, function, composable, resource, route veya preference anahtari var mi? |

### F. Islem-Anlam Tutarliligi

| Kural | Aciklama |
|-------|----------|
| F1 | `Ayarla`, `Degistir`, `Ac`, `Kapat`, `Sil`, `Sifirla`, `Yonet` gibi fiiller gercek davranisla birebir uyusuyor mu? |
| F2 | Bir satir ayar ekranini aciyorsa, bunu degisiklik yapan bir toggle gibi gostermiyor mu? |
| F3 | Bir buton dogrudan islem yapiyorsa, yalnizca bilgi ekranina gidiyormus gibi adlandirilmamis mi? |
| F4 | Yikici aksiyonlar onay, geri alma veya acik uyari gerektiriyor mu? |
| F5 | Kullanici butona bastiginda bekledigi sonucu goruyor mu, yoksa gizli yan etki mi oluyor? |

### H. Derleme ve API Senkronizasyonu

| Kural | Aciklama |
|-------|----------|
| H1 | Repository veya DAO'ya yeni bir metot eklendiyse, tum cagiran ViewModel'lar guncellendi mi? |
| H2 | APK derleme sonrasi en son kod ile senkron mu? Eski APK hala calisiyor mu? |
| H3 | ProGuard/R8 obfuscation metot adlarini degistirmis olabilir mi? (NoSuchMethodError riski) |
| H4 | Incremental build cache sorunu - eski derleme dosyalari APK'da kalakalmis olabilir mi? |
| F6 | Label, icon ve contentDescription ayni anlami tasiyor mu? |
| F7 | Settings subtitle veya aciklama metni gercekte ne olacagini soyluyor mu? |

### G. Settings Profesyonel Denetimi

| Kural | Aciklama |
|-------|----------|
| G1 | Settings icinde sik kullanilan islevler yanlis yere gomulmus mu? |
| G2 | Sistem ayarlarini tekrar eden veya onlarla catisan uygulama ayarlari var mi? |
| G3 | Ayarlar mantikli gruplara ayrilmis mi? |
| G4 | Default degerler dusuk riskli, tarafsiz ve pil/veri dostu mu? |
| G5 | Ayarlar ekraninda bilgi, aksiyon ve tercih kavramlari birbirine karismis mi? |

---

## 5. Oncelik Sistemi

| Oncelik | Kriter |
|---------|--------|
| KRITIK | Veri kaybi, crash, yanlis uygulama acma, geri alinmasi zor yanlis islem, yaniltici tehlikeli aksiyon |
| YUKSEK | UI calismaz, toggle etkisiz, ayar farkli is yapar, buton adi ile islev celisir, arama/siralama bozuk |
| ORTA | Gecici state tutarsizligi, performans kaybi, kafa karistiran etiket, eksik geri bildirim |
| DUSUK | Kod kalitesi, minor UX, log veya ufak metin netligi sorunu |

---

## 6. Denetim Yontemi

### 6.1 Otomatik Denetim

`scripts/audit.ps1` ile yapilir.

Amac:
- Regex veya statik analizle yakalanabilen sorunlari bulmak
- Onceki duzeltilerin regress etmeyip etmedigini kontrol etmek
- Her dongude hizli durum ozeti vermek

Kontrol ornekleri:
- Hardcoded prefs adi
- Locale belirtilmeyen arama
- State yerine normal degisken kullanimi
- Getter bazli pahali hesap
- Kullanilmayan parametre
- Global mutable singleton flag
- Kullanilmayan `private` fonksiyon, property veya helper
- Cagirilmayan screen, dialog, route veya utility
- Kullanilmayan drawable, string, layout, menu veya diger resource artiklari

Dead code odakli minimum kontroller:
- Android Lint ile `UnusedResources`
- Kotlin/IDE inspection ile `Unused symbol`
- Varsa detekt benzeri statik analizle `UnusedPrivateMember` ailesi

### 6.2 Manuel veya Yari Otomatik Denetim

Bu kurallar otomatik tarama ile tam yakalanamaz:
- C6, C7, C8, C9
- C10
- F1, F2, F3, F4, F5, F6, F7
- G1, G2, G3, G4, G5
- E5'in davranissal etkisi olan kismi: artik kullanilmayan ama hala akisi varmis gibi duran screen, ayar, buton veya route kalintilari

Bu denetimde su checklist uygulanir:

1. Gorunen buton veya satir metnini bul.
2. `onClick`, `onCheckedChange`, `Intent`, `launcher` veya `viewModel` cagrisi ne yapiyor kontrol et.
3. Label ile davranis uyusmuyorsa bulgu yaz.
4. Kullaniciya gosterilen subtitle, toast veya dialog metninin sonucu dogru anlattigini kontrol et.
5. Yikici aksiyonlarda aciklik, uyari ve geri alma ihtiyacini kontrol et.
6. TalkBack mantigiyla elemanin amacinin anlasilip anlasilmadigini dusun.
7. Kodda tanimli ama akista hic ulasilmayan screen, helper, ayar anahtari veya composable kalip kalmadigini kontrol et.
8. Gesture kullanan ekranlarda child-parent event tuketiminin scroll veya dismiss davranisini bozup bozmadigini kontrol et.

Detayli kontrol listesi icin:
- `local_denetim_manuel_checklist.md`

---

## 7. Rapor Formati

Her bulgu su formatta yazilir:

```md
### [ONCELIK] [KOD] - Kisa baslik
**Dosya:** `dosya.kt` (satir x-y)
**Sorun:** Tek cumle ile aciklama
**Etki:** Kullanici ne yasar?
**Oneri:** Tek cumle ile duzeltme onerisi
```

---

## 8. Calistirma Sikligi

- Her dongu sonunda otomatik denetim
- Yeni kod eklendiginde veya merge oldugunda anlik denetim
- Haftalik en az bir kez manuel semantik UI ve Settings denetimi
- Buyuk UI degisikliklerinden sonra TalkBack odakli kisa tur
- Zamanlanmis tam denetim ilk kez her gun `04:00` (TR / Europe-Istanbul) saatinde baslar, sonraki turlar **her 1 saat** tekrar eder.
- Her turda **farkli bir odak alani** denetlenir; tum alanlar dairesel olarak rotasyona girer.
- Odak alanlari:
  1. `UI_Settings_Labels` - etiket-davranis ve ayar tutarsizliklari
  2. `Gesture_Swipe_Drawer` - swipe, drawer, gesture akislari
  3. `Permission_Izin` - izin akislari, onboarding, fallback
  4. `Data_State_Persistence` - state yonetimi, SharedPrefs, kalicilik
  5. `Accessibility_A11y` - TalkBack, contentDescription, semantics
  6. `Performance_Memory` - recomposition, cache, IO, performans
  7. `Category_CRUD` - kategori ekleme/duzenleme/silme
  8. `Dock_Widget_Backup` - dock, widget, yedekleme akislari

---

## 9. Kaynak Temelli Referans Basliklari

Bu kurallar su resmi kaynaklarla hizalidir:

- Android Settings tasarim rehberi
- Android accessibility principles
- Compose accessibility rehberi
- Android accessibility testing rehberi
- Material 3 button ve labeling guidance
- Android Lint `UnusedResources`
- JetBrains/Kotlin `Unused symbol` inspection guidance

### Resmi Kaynak Linkleri

- Android Settings: `https://developer.android.com/design/ui/mobile/guides/patterns/settings`
- Android Accessibility Principles: `https://developer.android.com/guide/topics/ui/accessibility/principles`
- Compose Accessibility: `https://developer.android.com/develop/ui/compose/accessibility`
- Compose Semantics: `https://developer.android.com/develop/ui/compose/accessibility/semantics`
- Compose Accessibility API Defaults: `https://developer.android.com/develop/ui/compose/accessibility/api-defaults`
- Material 3 Buttons: `https://m3.material.io/components/buttons/guidelines`
- Material 3 Dialogs: `https://m3.material.io/components/dialogs/guidelines`
- Material 3 Lists: `https://m3.material.io/components/lists/guidelines`
- Material 3 Snackbar: `https://m3.material.io/components/snackbar/guidelines`
- Material 3 Progress Indicators: `https://m3.material.io/components/progress-indicators/guidelines`
- Material 3 Text Fields: `https://m3.material.io/components/text-fields/guidelines`
- Android Lint Checks: `https://developer.android.com/studio/write/lint`
- JetBrains Unused Symbol Inspection: `https://www.jetbrains.com/help/inspectopedia/UnusedSymbol.html`
- detekt Potential Bugs Rule Set: `https://detekt.dev/docs/rules/potential-bugs`

---

## 10. Otomatik Denetim Dongusu ve Kurallar

- Denetim otomasyonu iki asamalidir: saat basinda tam denetim, **5 dakika sonra** rapor bulgularini cozmeye odakli resolve turu.
- Kural guncellemesi yalnizca bulgu sonrasinda yapilir; iyilesme yoksa kural degismez.
- Her tam denetim turu su adimlari izler:
  1. Otomatik statik tarama (`scripts/audit.ps1`)
  2. Manuel semantik tur (`local_denetim_manuel_checklist.md`)
  3. Yeni bulgular `local_denetim_raporu.md` icine tarih-saat ile eklenir
  4. Odak alanlari rotasyon indeksleri guncellenir
- Her resolve turu su adimlari izler:
  1. Tam denetimden 5 dakika sonra acik bulgular siralanir
  2. Cozulebilen maddeler kodda kapatilir
  3. Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasina tarih-saat ve aciklama ile tasinir
  4. Cozulemeyen maddeler `local_denetim_raporu.md` icinde acik kalir veya not dusulur
- Geri bildirim akislari kullanici dostu, anlasilir ve hizli olmali.

### Arama Gelistirme Yonergesi (Roadmap)

- Arama; istege gore telefon rehberindeki isimler de dahil edilebilecek sekilde tasarlanmalidir.
- Bu ozellik opsiyonel kalmali; varsayilan davranis mevcut hizli dosya/uygulama aramasidir.
- Kisi verisiyle ilgili gizlilik ve izin akislari ilk sirada denetlenmelidir.

---

*Guncelleme: 2026-06-27 | Denetci standardi: profesyonel local auditor*
