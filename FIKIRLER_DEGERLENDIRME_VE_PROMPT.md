# Fikirler Değerlendirmesi ve Uygulama Promptu

Tarih: 2026-07-10

## Sonuç

- Aktif fikirler içinde sayısal puanı olmayan tek madde `QUERY_ALL_PACKAGES Play Store beyan formu` idi.
- Puanı: **19/20** (`KV:5 · U:5 · BR:5 · EA:4`). Yayın engelleyici olduğu için ilk sıradadır.
- `ÇIKARILDI (kayıt)` satırındaki `-`, puansız fikir değil; bilinçli olarak reddedilmiş fikirlerin denetim kaydıdır ve puanlanmamalıdır.
- “Beklet (≤11p)” bölümünde 12–15 puanlı maddeler bulunuyor. Bunlar içerik değişmeden doğru öncelik bölümlerine taşınmalıdır.

## Yapılması Gerekenler

1. **Play Console yayın kapısını kapat:** `QUERY_ALL_PACKAGES` beyanını tamamla; Data Safety, Privacy Policy ve uygulamanın gerçek paket görünürlüğü davranışının birbiriyle tutarlı olduğunu doğrula.
2. **Release hazırlığını tamamla:** release keystore, content rating, imzalı AAB ve temiz committen üretim kanıtını hazırla.
3. **Gerçek cihaz Play-öncesi QA çalıştır:** Android 14 NotificationListener, backup/restore, worker schedule, blur/API 26 ve temel launcher akışlarını kanıtla.
4. **Store görsellerini üret:** kişisel veri içermeyen Pixel 6 light/dark ekran görüntülerini hazırla.
5. **15p ürün işlerini sırala:** Akıllı Bildirim Analiz Sistemi ve UsageEvents oturum altyapısını, yayın kapıları tamamlandıktan sonra ele al.
6. **13–14p kalite işlerini planla:** hardcoded Türkçe metin envanteri, backup kapsamı, restore sonrası UI refresh, startup sadeleştirme ve ayar etki matrisi.
7. **Backlog sınıflandırmasını düzelt:** 12–14p maddeleri “Orta”, 15p maddeleri “Yüksek” bölümüne taşı; “Beklet” bölümünde yalnızca ≤11p kalsın.
8. Her madde için durum, kanıt dosyası, test sonucu ve gerekiyorsa dış aksiyon sahibini kaydet; tamamlananları `HISTORY.md`ye taşı.

## Nokta Atışı Prompt

```text
AppOrganizer reposunda yayın öncesi önceliklendirme ve uygulama turu yap.

Önce AGENTS.md ve CLAUDE.md kurallarını eksiksiz oku. Ardından FİKİRLER.md, ROADMAP.md, COZULEMEYEN_SORUNLAR.md ve ilgili kaynak kodu incele. Mevcut kullanıcı değişikliklerini koru; ilgisiz dosyalara dokunma.

Hedef:
1) FİKİRLER.md içindeki puan/bölüm tutarsızlıklarını düzelt: 15+ Yüksek, 12–14 Orta, ≤11 Beklet. “ÇIKARILDI” kayıtlarını fikir gibi puanlama.
2) Önce yayın engelleyicilerini ele al: QUERY_ALL_PACKAGES beyan kanıtı, Privacy Policy/Data Safety tutarlılığı, release imza/AAB kapısı.
3) Kodla yapılabilen en yüksek öncelikli işi uygula; yalnız Play Console veya keystore sahibi tarafından yapılabilecek işleri değiştirmeden, tam adım ve gerekli metin/kanıt listesi olarak raporla.
4) Uygulanan değişiklik için birim test, lint ve ilgili Gradle testlerini çalıştır. Gerçek cihaz gerektiren testi başarılı varsayma; “dış doğrulama gerekli” diye işaretle.
5) FİKİRLER.md ve ROADMAP.md durumlarını yalnız kanıtlanan sonuçlara göre güncelle; tamamlanan işi HISTORY.md'ye taşı.

Puan modeli: KV (kullanıcı değeri) + U (uygulanabilirlik) + BR (benzersizlik/stratejik değer) + EA (efor avantajı), her biri 1–5; toplam 20. Kanıt zayıfsa bunu açıkça belirt.

Çıktı sırası:
- Kısa denetim bulguları
- Değiştirilen dosyalar ve gerekçe
- Test komutları ve gerçek sonuçları
- Dış aksiyonlar (sahip + tam adım + gerekli kanıt)
- Kalan işler: P0 / P1 / P2

Başarı ölçütü: yayın engelleyicileri açıkça ayrılmış, backlog puanları doğru bölümlerde, yapılan kod değişikliği test edilmiş ve hiçbir dış işlem yapılmış gibi gösterilmemiş olmalı.
```

## Puanlama Notu

Mevcut repo ölçeği korunmuştur. RICE, erişim ve efor verisi oluştuğunda yüksek puanlı ürün fikirlerini ikinci turda sıralamak için kullanılabilir; mevcut ham backlog için aynı dört kriterle tutarlı puanlama daha uygundur.
