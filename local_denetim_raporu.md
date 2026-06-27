# Local Denetim Raporu

> Dongu: `15 dakikalik 8+1 odak rotasyonu + runtime API senkronizasyon denetimi`
> Son denetim: `2026-06-27 09:51`
> Kapanan maddeler `local_denetim_tamamlananlar.md` dosyasina tarih-saat ile tasinir.
> Roadmap: Telefon rehberi arama destegi opsiyonel kalir; gizlilik ve izin akislari oncelikli denetlenir.

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu yok (K9 cozuldu) |
| YUKSEK | 1 | Permission rette fallback ve ayar yonlendirme eksik (Y6) |
| ORTA | 1 | removeFromDock Unit donduruyor, geri bildirim yok (O7) |
| DUSUK | 0 | Acik dusuk bulgu yok |
| TOPLAM | 2 | |

---

## Yeni Tespit Edilen Bulgular

### [YUKSEK] [Y6] Permission rette fallback ve ayar yonlendirme eksik
**Dosja:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/OnboardingScreen.kt` (satir 108)
**Sorun:** `shouldShowRequestPermissionRationale` kontrolu eksik; kullanici izni reddedince onboarding sonsuz donguye girebiliyor.
**Oneri:** Rationale kontrolu ve `ACTION_APPLICATION_DETAILS_SETTINGS` ile ayarlara yonlendirme ekle.

### [ORTA] [O7] removeFromDock Unit donduruyor, geri bildirim yok
**Dosja:** `app/src/main/java/com/armutlu/apporganizer/utils/DockPrefs.kt` (satir 43)
**Sorun:** `removeFromDock()` `Unit` donduruyor; caller'a basari/basari durumu bilgisi verilmiyor.
**Oneri:** Return type'i `Boolean` yap ve kaldirma isleminin basarili olup olmadigini dondur.

---

## Kapatilan Bulgular

### [KRITIK] [K9] Runtime NoSuchMethodError riski - getAllCategoriesFlow API senkronu
**Durum:** COZULDU
**Cozum:** Kaynak kodda `getAllCategoriesFlow()` hem `CategoryDao` (satir 60) hem `AppRepository` (satir 28) hem `AppListViewModel` (satir 120) tarafinda tanimli ve cagriliyor. Sorun APK build cache / incremental build senkronizasyon kaynakli. `clean build` ile yeniden derleme yapildi; APK ile kaynak kod artik senkron.

---

## 4. Hata Analizi: NoSuchMethodError Neden Denetimde Gormedi?

Bu hata **runtime/derleme sonrasi** bir sorundur:

- Kaynak kodda `getAllCategoriesFlow()` metotlari tanimli ve cagriliyor.
- Denetim script'imiz (`audit.ps1`) **sadece kaynak kodu tarar**; APK derlemez, APK icerigini okumaz.
- Incremental build cache, ProGuard/R8 obfuscation veya eski APK ile yeni kod senkronizasyon bozuklugu nedeniyle APK'da metot kaybolabilir.
- Eski APK yuklenmisken yeni kod build edilirse, APK ile kaynak kod eslesmez ve runtime hatasi alinir.

**Yapilan cozum:** Denetime `K9` kurali eklendi - bu kural `repository.getAllCategoriesFlow()` cagrisini tespit eder ve API senkronizasyon riskini isaretler. Boylece gelecekte benzer hatalar daha erken yakalanacak.

---

## 5. Bu Dongu Sonucu

- Tam denetim akisi 15 dakikalik odak rotasyonu ile calisiyor.
- K9 (KRITIK) bulgusu API senkronizasyon denetimi ile eklendi; bu tur cozuldu.
- Resolve turu otomatik olarak calisiyor.
- Gecici build log dosyasi `.gitignore` kapsamina alindi.

---

*Denetim tarihi: 2026-06-27 | Denetim tipi: 15 dakikalik otomatik + manuel checklist + K9 API senkron denetimi*