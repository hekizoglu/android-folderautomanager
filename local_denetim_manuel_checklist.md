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

## 3. Tehlikeli Islem Kontrolu

- Silme, resetleme, kaldirma veya veri etkileyen eylemler yeterince acik mi?
- Onay, geri alma veya ikinci adim gerekiyor mu?
- Kullanici yanlislikla basarsa zarar buyuk mu?

## 4. Accessibility Kontrolu

- TalkBack ile elemanin amaci tek seferde anlasilir mi?
- `contentDescription` eksik veya celiskili mi?
- Bir grup tek bir kart gibi davranirken cocuk elemanlar gereksiz fokus aliyor mu?
- Okuma sirasi mantikli mi?

## 5. Sonuc Kontrolu

- Kullanici eylemden sonra bekledigi sonucu hemen goruyor mu?
- Toast, dialog veya state metni yapilan islemi dogru anlatiyor mu?
- Basarisiz durumda kullanici ne yapacagini anlayabiliyor mu?

---

## Onerilen Manuel Tur Alanlari

- `SettingsScreen`
- Tum dialog ve bottom sheet aksiyonlari
- Context menu ve overflow menu maddeleri
- Ilk acilis, izin, reset ve backup akislari
