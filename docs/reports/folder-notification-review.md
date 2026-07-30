# Klasor Bildirimleri Inceleme Raporu

## Kapsam

Klasor acildiginda gosterilen bildirim bandi, klasor kutucugundaki bildirim ozeti ve bildirim dinleyicisinin aktif bildirim akisi incelendi.

## Bulgular

- Klasor icindeki bildirim bandi yalnizca `NotificationListenerService` tarafindan aktif kabul edilen bildirimleri gosteriyor.
- Uygulamalar klasor icindeki konumlarina gore siralaniyordu; kullanici icin en onemli bildirim once gelmiyordu.
- Bildirim bandi icin ayri bir kullanici tercihi yoktu.
- Bildirim adedi gosteriliyordu ancak 99 uzeri degerler icin alan kontrolu yoktu.
- Bildirim metni ile bildirim adedi farkli akislar tarafindan beslense de tek bir UI satirinda birlikte sunulabiliyordu.

## Uygulanan degisiklikler

- `Klasor icinde bildirimleri goster` tercihi Launcher ayarlarina eklendi.
- Tercih `SharedPreferences` ile kalici tutuluyor ve acik klasor ayar degistiginde aninda tepki veriyor.
- Klasor bildirim bandi kapatildiginda band ve icindeki uygulama bildirimleri gizleniyor; bildirim dinleyicisinin veri toplama akisi etkilenmiyor.
- Uygulamalar once bildirim adedine, esitlikte son bildirim zamanina gore siralaniyor.
- Adetler `1..99` veya `99+` olarak gosteriliyor.
- Ekran okuyucu etiketi uygulama adini ve bildirim adedini tasiyor.

## Veri ve urun sinirlari

- Bu alan aktif sistem bildirimlerini temsil eder; 7 gunluk veya 30 gunluk gecmis raporu degildir.
- Bildirim erisimi kapaliysa bant veri gosteremez; bu Android `NotificationListenerService` yasam dongusunun dogal sonucudur.
- Klasor kutucugundaki kisa bildirim metni mevcut genel `Bildirim metnini goster` tercihiyle kontrol edilmeye devam eder.

## Sonraki oneriler

1. Klasor icinde bildirim bandina `Tumunu ac` ve uygulama bazli Android bildirim ayarlarina gitme aksiyonu eklenebilir.
2. Bildirimleri `Son gelen`, `En cok`, `Yuksek oncelik` olarak secilebilir bir siralama filtresiyle sunmak yararli olur.
3. Aktif bildirim adedi ile secilen rapor araligi toplam adedi ayri etiketlerle gosterilmelidir; ikisi ayni sayi degildir.
4. Gercek cihazda bildirim erisimi kapatma, servis yeniden baglanmasi ve 99 uzeri bildirim senaryolari test edilmelidir.

## Dogrulama

- XML kaynak dosyasi parse edildi.
- `git diff --check` temiz.
- Gradle derlemesi kaynak derleme asamasina gelmeden Windows'ta salt-okunur `app/build/generated/source/buildConfig/debug/com/armutlu/apporganizer` ciktisinda `AccessDeniedException` ile durdu; bu nedenle derleme sonucu basarili kabul edilmedi.
