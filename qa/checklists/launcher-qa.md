# Android Launcher QA Checklist

> Bu checklist, AppOrganizer launcher uygulaması için mutlaka test edilecek senaryoları listeler.
> Kaynak: [Android Developers — Home/Automotive](https://developer.android.com/develop/ui/views/appwidgets/host), [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager), [Package Visibility](https://developer.android.com/training/package-visibility)

---

## Build Doğrulama

- [ ] Proje `./gradlew assembleDebug` ile başarıyla derleniyor
- [ ] Lint hatası yok (`./gradlew lintDebug`)
- [ ] Unit testler geçiyor
- [ ] `detekt` hatası yok
- [ ] `ktlint` hatası yok

---

## Launcher Akışları

- [ ] **Varsayılan launcher ayarı**: Ayarlar → Varsayılan uygulamalar → Ana ekran → AppOrganizer seçiliyor
- [ ] **Home tuşu**: Home tuşuna basınca AppOrganizer açılıyor
- [ ] **Back tuşu**: Back tuşu ile uygulamadan çıkılıyor, boş ekranda takılmıyor
- [ ] **App drawer**: Yan dalga/yukarı kaydırma ile tüm uygulamalar listeleniyor
- [ ] **Arama**: Uygulama arama doğru sonuç veriyor
- [ ] **Klasörler**: Klasör oluşturma, içine uygulama ekleme, açma, düzenleme çalışıyor
- [ ] **Favoriler**: Yıldız ile favori ekleme/kaldırma çalışıyor
- [ ] **Son kullanılanlar**: Recent apps listesi dolu görünüyor
- [ ] **Dock**: En fazla 4 uygulama dock'a eklenebiliyor, kaldırma çalışıyor

---

## Android Sürümü ve OEM Uyumluluğu

- [ ] **Android 11 (API 30)**: Package visibility kısıtlarına takılmıyor
- [ ] **Android 13 (API 33)**: Bildirim izni akışı çalışıyor
- [ ] **Android 14 (API 34)**: NotificationListenerService ve sağlayıcı davranışı
- [ ] **Samsung One UI**: Sistem uygulamaları doğru filtreleniyor
- [ ] **Xiaomi MIUI/HyperOS**: İzin ve pil optimizasyonu davranışı
- [ ] **Huawei/Honor**: HarmonyOS/EMUI uyumluluğu

---

## Görsel ve UI

- [ ] **Tema**: Açık tema ve koyu tema her ekranda düzgün görünüyor
- [ ] **Büyük font**: Sistem font büyüklüğü artırıldığında taşma olmuyor
- [ ] **Dikey/yatay dönüş**: Ekran döndüğünde state kaybolmuyor
- [ ] **Tablo/çoklu sütun**: Farklı ekran boyutlarında grid düzgün dağılıyor
- [ ] **İkonlar**: Uygulama ikonları eksik/bulanık değil

---

## Performans

- [ ] **Soğuk açılış**: ≤ 1 saniye
- [ ] **Drawer açılışı**: ≤ 200ms
- [ ] **Scroll jank**: Hareketsiz, akıcı
- [ ] **RAM**: 5 dakika sonra ≤ 180MB
- [ ] **Arka plan**: Beklenmeyen arka plan çalışması yok

---

## Güvenlik ve İzinler

- [ ] `QUERY_ALL_PACKAGES` kullanımı gerekçeli mi?
- [ ] `POST_NOTIFICATIONS` fallback ve açıklama var mı?
- [ ] `PACKAGE_USAGE_STATS` ayar yönlendirmesi var mı?
- [ ] Hassas veri loglarda/dışa aktarmada sızmıyor mu?
- [ ] Bağımlılıklar checksum doğrulamasından geçiyor mu?

---

## Kayıt Sonrası

Her test sonrası:
- Sonuç: PASS / FAIL / SKIP
- Notlar:
- Ekran görüntüsü:
