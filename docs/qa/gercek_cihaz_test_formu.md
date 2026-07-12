# Gercek Cihaz Test Formu

Bu form AppOrganizer'in release oncesi fiziksel cihaz dogrulamalari icin kullanilir.
Test sirasinda ilgili alanlari doldurmak yeterlidir.

## Test Bilgisi

- Tarih:
- Cihaz:
- Marka/Model:
- Android surumu:
- Build tipi:
- Build numarasi/commit:
- Test eden:

## Genel Sonuc

- Durum: `Gecti / Kismi / Kaldi`
- Release blocker var mi: `Evet / Hayir`
- Kisa ozet:

## 1. Temiz Kurulum ve Onboarding

- [ ] Uygulama temiz kuruldu
- [ ] Ilk acilis sorunsuz
- [ ] Onboarding sirasi dogru
- [ ] Tema secimi calisiyor
- [ ] Hizli ayarlar adimi dogru
- [ ] Launcher ayarlama adimi dogru
- [ ] Izin vermeden de temel kullanim mumkun

Beklenen:
- akis bozulmadan tamamlanmali
- cokme olmamali

Sonuc:

Kanit:

Not:

## 2. POST_NOTIFICATIONS Izni

- [ ] Ilk kurulumda bildirim izni kapali senaryo denendi
- [ ] Izin yokken uygulama cokmedi
- [ ] Izin yokken sessiz degrade etti
- [ ] Sonradan izin verilince davranis duzeldi
- [ ] Izin geri alinca tekrar kontrollu degrade etti

Beklenen:
- Android 13+ izin mantigi dogru
- gereksiz spam veya crash yok

Sonuc:

Kanit:

Not:

## 3. Notification Access / Bildirim Analizi

- [ ] Notification access kapaliyken yonlendirme dogru
- [ ] Access acildi
- [ ] Gercek bildirim uretildi
- [ ] Bildirim verisi raporlara dustu
- [ ] Cok konusan / rahatsiz eden / dikkat dagitan alanlari mantikli
- [ ] Rapor satiri tiklamasi dogru yere gidiyor

Beklenen:
- veri gercekten toplanmali
- yanlis paket/bozuk sayi olmamali

Sonuc:

Kanit:

Not:

## 4. Reboot Sonrasi Davranis

- [ ] Telefon yeniden baslatildi
- [ ] Uygulama geri acildi
- [ ] Yeni bildirimler tekrar toplandi
- [ ] Servis akisi kopmadi
- [ ] Gereksiz tam analiz baslamadi

Beklenen:
- reboot sonrasi sistem toparlamali

Sonuc:

Kanit:

Not:

## 5. Accessibility / Ozel Erisim

- [ ] Izin kapaliyken akis test edildi
- [ ] Kullanici dogru ayara yonlendirildi
- [ ] Izin acildiktan sonra ilgili ozellik calisti
- [ ] Erisim sadece beklenen is icin kullanildi
- [ ] Beklenmedik otomasyon gorulmedi
- [ ] Izin kapaliyken uygulama cokmedi

Beklenen:
- disclosure ile gercek davranis uyumlu olmali

Sonuc:

Kanit:

Not:

## 6. Ana Ekran / Cache / Performans

- [ ] Ana ekran normal surede acildi
- [ ] Tum uygulamalari sonlandir benzeri agir akis denendi
- [ ] Uygulama tekrar acildiginda tam yeniden analiz yapmadi
- [ ] Cache davranisi mantikli
- [ ] Belirgin donma/cokme olmadi

Beklenen:
- ana ekran stabil ve tekrar analiz acisindan kontrollu olmali

Sonuc:

Kanit:

Not:

## 7. Oneriler / Ticker / Tiklama Hedefleri

- [ ] Oneri kartlarinda logo ve yazi uyumlu
- [ ] Ticker icinde uygulama bazli oge gosterildi
- [ ] Tiklayinca dogru uygulama acildi
- [ ] Bilgilendirme karti bosa gitmedi
- [ ] Ayni icerik rahatsiz edici siklikta tekrar etmedi
- [ ] Dijital yasam skoru gorunurlugu yeterli

Beklenen:
- yanlis app acilmamali
- icerik tutarli olmali

Sonuc:

Kanit:

Not:

## 8. Arama Cubugu / Yetki Anlatimi

- [ ] Arama cubugu secili durumu belirgin
- [ ] Renk/dekorasyon secili oldugunu hissettiriyor
- [ ] Hangi yetki varsa/yoksa aciklaniyor
- [ ] Ek yetki gerekiyorsa belirtiliyor
- [ ] Birkac ret sonrasi kullaniciyi yormayan davranis var
- [ ] Ayarlarda gerekli izinler alani mantikli

Beklenen:
- kullanici ne eksik biliyor olmali

Sonuc:

Kanit:

Not:

## 9. Kategori ve Yeni Uygulamalar

- [ ] Yeni yuklenen uygulama tespit edildi
- [ ] Uygulamanin dustugu kategori anlasilabiliyor
- [ ] Kategori sonucu mantikli
- [ ] OEM'e gore asiri sapma yok

Beklenen:
- kullanici "hangi kategoriye gitti" hissini almali

Sonuc:

Kanit:

Not:

## 10. All Apps / Double-Tap / Arama Jestleri

- [ ] All Apps acildi
- [ ] Double-tap denendi
- [ ] Arama ile jest cakismasi denendi
- [ ] Tekrarlanabilir hata var mi kontrol edildi

Beklenen:
- yanlis tetikleme olmamali

Sonuc:

Kanit:

Not:

## 11. Backup / Restore

- [ ] Export alindi
- [ ] SAF / klasor secimi calisti
- [ ] Import yapildi
- [ ] Eksik paket uyarisi calisti
- [ ] Ayarlar / klasorler / duzen geri geldi
- [ ] Restore sonrasi cokme olmadi

Beklenen:
- telefon degisimi senaryosu guvenli olmali

Sonuc:

Kanit:

Not:

## 12. Blur / API Davranisi

- [ ] Blur destekli ekranlar test edildi
- [ ] Fallback dogru calisti
- [ ] API farkinda goruntu bozulmadi
- [ ] Performans kabul edilebilir

Beklenen:
- dusuk API veya farkli cihazda kirilma olmamali

Sonuc:

Kanit:

Not:

## 13. Uzun Oturum / Dayaniklilik

- [ ] 10-15 dk aktif kullanim yapildi
- [ ] Arka plana alip geri donuldu
- [ ] Uygulama durumunu korudu
- [ ] Belirgin memory/performance sorunu gorulmedi

Beklenen:
- gunluk kullanimda stabil kalmali

Sonuc:

Kanit:

Not:

## 14. OEM Ozel Kontrol

- [ ] Samsung test edildi
- [ ] Xiaomi test edildi
- [ ] Pixel/stock Android test edildi
- [ ] Izin ekrani farklari not edildi
- [ ] Worker / arka plan davranis farklari not edildi

Beklenen:
- kritik akislar OEM bazinda kirilmamali

Sonuc:

Kanit:

Not:

## Bug Kaydi

- Baslik:
- Adimlar:
- Beklenen:
- Gerceklesen:
- Siklik:
- Ciddiyet: `Blocker / High / Medium / Low`
- Kanit:
- Not:

## Test Sonu Ozeti

- Gecen alanlar:
- Kismi gecen alanlar:
- Kalan bug'lar:
- Release blocker'lar:
- Onerilen sonraki aksiyon:
