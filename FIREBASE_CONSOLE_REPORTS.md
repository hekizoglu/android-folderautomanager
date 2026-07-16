# Firebase konsol raporları — B12 işletim sözleşmesi

Bu dosya, Firebase/GA4 konsolunda tutulacak en küçük rapor setini ve her raporun hangi ürün kararını tetiklediğini tanımlar. Ham kullanıcı içeriği, paket adı veya serbest metin hiçbir raporda boyut değildir. Varsayılan karşılaştırma `son 7 gün / önceki 7 gün`; örneklem küçükse karar verilmez ve 28 günlük görünüm kullanılır.

## Yayımlanacak rapor koleksiyonu

| Panel | Konsol yüzeyi ve ölçüm | Ürün kararı |
|---|---|---|
| Kullanıcı ve onboarding | GA4 özet: günlük/haftalık aktif kullanıcı. Kapalı Funnel Exploration: onboarding başladı → sabit adımlar → tamamlandı; adım ve izin sonucu sabit enum boyutlarıdır. | WAU iki ardışık 28 günlük dönemde düşerse elde tutma işi önceliklenir. En yüksek terk oranlı adım bir sonraki onboarding denemesinin hedefidir; izin reddi yüksekse açıklama/istem zamanı değiştirilir. |
| Arama | GA4 özet/exploration: arama yapan aktif kullanıcı / aktif kullanıcı; sıfır sonuç / arama; sonuç açma / arama; ilk sonuç açma / sonuç açma; sabit gecikme kovası dağılımı. | Kullanım düşükse keşfedilebilirlik, sıfır sonuç yüksekse indeks/kapsam, açma oranı düşükse sıralama iyileştirilir. İlk sonuç tercihi düşerse ranking deneyi; yavaş kovalar büyürse Performance incelemesi açılır. |
| Sınıflandırma | GA4 exploration: incelemeye alınan / sınıflandırılan; onay ve düzeltme / incelenen; yalnız sabit kaynak ve güven kovalarına göre düzeltme; günlük özet tutarlılık alanı. | Düşük güvenli veya belirli kaynaklı hata yoğunlaşırsa o kaynak/model düzeltilir. Toplam uyuşmazlık artarsa sürüm yayılımı durdurulur; iyi oranlarda kullanıcıya daha az inceleme gösterilir. |
| Sağlık | Crashlytics: crash-free kullanıcı ve ANR; GA4 günlük sağlık özeti: sabit worker uyarı kodu, indeks yaş kovası; Android sürümü ve üreticiye göre kırılım. | Crash-free kullanıcı geriler veya ANR artarsa yayın durdurulur. Tek bir OS/üretici kümesi baskınsa uyumluluk düzeltmesi; worker kodu veya eski indeks artarsa ilgili worker/index işi önceliklenir. |
| Özellik benimseme | GA4 özet: günlük kullanım özetindeki sabit `top_feature`, sayım kovaları ve boolean alanlardan arama, klasörler, görevler, raporlar, bildirim analizi, widget ve yedekleme kullanan aktif kullanıcı oranı. | Benimsenmeyen özellik iki ardışık 28 günlük dönemde karar üretmiyorsa navigasyonu/özelliği sadeleştir; büyüyen özellik için erişim ve güvenilirlik yatırımı yap. |

Formüllerde payda sıfırsa oran gösterilmez. Kullanıcı sayısı düşük kırılımlar yorumlanmaz. Analytics oranları yalnız telemetriye onay veren kurulumları temsil eder; tüm kullanıcı nüfusu olarak sunulmaz. Crash-free metriği yalnız fatal olaylara dayanır ve bir kullanıcı bir uygulama kurulumudur.

## Konsol kurulum ve kanıt listesi

1. GA4 Editor/Admin hesabıyla `AppOrganizer — Ürün Sağlığı` koleksiyonunu oluştur; yukarıdaki GA4 raporlarını ekle ve koleksiyonu yayımla.
2. Onboarding kapalı funnel adımlarını sırayla tanımla. Serbest metin veya yüksek-cardinality boyut ekleme.
3. Crashlytics görünümünü aynı tarih aralığında crash-free kullanıcı ve ANR için kaydet; uygulama sürümü, Android sürümü ve cihaz üreticisi filtrelerini doğrula.
4. Arama gecikmesini Analytics kova dağılımıyla, `global_search` süresini Performance > Custom traces görünümüyle çapraz kontrol et.
5. Opt-in açık test cihazında DebugView üzerinden her rapor ailesinin örnek event/parametresini gör. Crashlytics ve Performance verisinin ilgili konsol yüzeyine ulaştığını doğrula.
6. Kanıt kaydına tarih, Firebase proje/GA4 property adı, rapor adı, görünür örnek veri ve doğrulayan kişi ekle. Kullanıcı kimliği veya event içeriği içeren ekran görüntüsü saklama.

## Bakım ve kaldırma kuralı

Raporlar haftalık sürüm kontrolünde incelenir. Her kararın sahibi geliştiricidir. Bir metrik iki ardışık 28 günlük incelemede hiçbir karar veya araştırma üretmediyse koleksiyondan kaldırılır; yeni metrik ancak bu dosyaya karar cümlesi ve gizlilik kontrolü eklenince yayımlanır. Konsol kurulumu veya gerçek veri kanıtı yoksa B12 `Tamamlandı` sayılamaz.

## Resmi kaynaklar

- [Firebase Analytics DebugView](https://firebase.google.com/docs/analytics/debugview)
- [GA4 funnel exploration](https://support.google.com/analytics/answer/9327974)
- [GA4 özel funnel raporu](https://support.google.com/analytics/answer/13012015)
- [Crashlytics crash-free metrikleri](https://firebase.google.com/docs/crashlytics/crash-free-metrics)
- [Crashlytics ANR inceleme](https://firebase.google.com/docs/crashlytics/debug-anr-errors)
- [Firebase Performance konsol kullanımı](https://firebase.google.com/docs/perf-mon/console)

