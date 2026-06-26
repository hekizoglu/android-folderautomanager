# Local Denetim Manuel Checklist

> Bu checklist, otomatik taramanin tek basina yakalayamadigi semantik UI ve ayar davranisi sorunlari icin kullanilir.

---

## 1. Buton ve Label Kontrolu

- Buton etiketi bastiginda olacak eylemi acikca anlatiyor mu?
- `Ayarla` etiketi gercekte bir sistem secim ekrani aciyorsa uygun mu?
- `Degistir` etiketi gercekte anlik degisiklik yapmiyorsa uygun mu?
- `Sil` veya `Sifirla` gibi aksiyonlar fazla yumusak veya belirsiz adla gizlenmis mi?
- Icon varsa metinle ayni anlami destekliyor mu?

## 2. Settings Satiri Kontrolu

- Satir bir preference mi, bir navigasyon satiri mi, yoksa anlik aksiyon mu?
- Subtitle kullanicinin ne olacagini dogru anlatiyor mu?
- Ayar degisince ekranda aninda geri bildirim var mi?
- Ayar sadece sistem ekranina yonlendiriyorsa bu metinden anlasiliyor mu?
- Sistem ayarlarini kopyalayan veya catisan ayar var mi?
- Sistem ayarina giden satir, sanki uygulama ici toggle degismis gibi yaniltici bir dil kullaniyor mu?

## 3. Tehlikeli Islem Kontrolu

- Silme, resetleme, kaldirma veya veri etkileyen eylemler yeterince acik mi?
- Onay, geri alma veya ikinci adim gerekiyor mu?
- Kullanici yanlislikla basarsa zarar buyuk mu?

## 4. Accessibility Kontrolu

- TalkBack ile elemanin amaci tek seferde anlasilir mi?
- `contentDescription` eksik veya celiskili mi?
- Bir grup tek bir kart gibi davranirken cocuk elemanlar gereksiz fokus aliyor mu?
- Okuma sirasi mantikli mi?
- Bildirim sayisi, secili durum, acik/kapali durum gibi kritik bilgiler sesli olarak aktariliyor mu?

## 5. Sonuc Kontrolu

- Kullanici eylemden sonra bekledigi sonucu hemen goruyor mu?
- Toast, dialog veya state metni yapilan islemi dogru anlatiyor mu?
- Basarisiz durumda kullanici ne yapacagini anlayabiliyor mu?

## 6. Dead Code Kontrolu

- Tanimli olup hic acilmayan ekran, dialog, bottom sheet veya route var mi?
- Kodda kalan ama UI'da artik tetiklenmeyen buton handler, callback veya helper var mi?
- Preference key tanimli ama hic okunmayan ya da yazilmayan ayar var mi?
- Kullanilmayan drawable, string, menu veya diger resource artiklari birikmis mi?
- Eski refactor sonrasi yalnizca bir kez tanimli kalmis composable, extension veya util fonksiyon var mi?

## 7. Gesture ve Kapanis Kontrolu

- Scroll edilen liste ile swipe-to-close veya swipe-to-open gesture'lari birbirini eziyor mu?
- Cocuk composable drag event'ini gereksiz erken consume ederek parent gesture'ini bozuyor mu?
- Threshold degerleri dp/px acisindan ekran yogunlugundan bagimsiz ve tutarli mi?

---

## Onerilen Manuel Tur Alanlari

- `SettingsScreen`
- Tum dialog ve bottom sheet aksiyonlari
- Context menu ve overflow menu maddeleri
- Ilk acilis, izin, reset ve backup akislari
- checklist için geliştirme önerin var ise en alta sistemin önerileri diye başlık aç ve her döngü de yeni bir fikrini ekle
