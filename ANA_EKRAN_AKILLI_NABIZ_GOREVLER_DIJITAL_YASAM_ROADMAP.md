# AppOrganizer — Ana Ekran Akıllı Nabız, Görevler ve Dijital Yaşam Roadmap'i

> **Oluşturma tarihi:** 2026-07-17
> **Kapsam:** Ana ekrandaki kayan yazı/haber şeridi, görev sistemi ve Dijital Yaşam kartının tek bir ürün sistemi olarak yeniden düzenlenmesi.
> **Ana hedef:** Kullanıcıya aynı bilgiyi üç yerde tekrar etmek yerine, doğru bilgiyi doğru bileşende; mevcut değer, hedef, kalan miktar, neden ve eylem ile göstermek.
> **Durum:** Tamamlanan 33 döngü HISTORY.md arşivine taşındı (2026-07-19, bkz. "ARSIV: ANA_EKRAN_AKILLI_NABIZ_..." bloğu). Bu dosyada yalnız açık iş kaldı.

Durum değerleri: `⏳ Bekliyor` · `🚧 Devam ediyor` · `🟡 Kısmen tamamlandı` (kod+temel test var, cihaz/regresyon kanıtı eksik) · `✅ Tamamlandı` · `⛔ Bloke`.

Tamamlanan bir döngünün durum satırı biçimi:

```text
**Durum:** ✅ Tamamlandı — Döngü HXX — commit: <SHA> — tarih: YYYY-MM-DD
```

---

# Açık döngü

## Döngü U04 — Tam test matrisi ve yayın kapısı

**Amaç:** Değişikliklerin gerçek cihazlarda güvenilir çalışması.

## Unit test matrisi

### Görevler

- Günlük üst sınır erken tamamlanmaz.
- Gün sonunda doğru settlement.
- Haftalık ISO sınırı.
- Eylem görevi anlık tamamlanır.
- Aynı görev iki kez ödüllendirilmez.
- Progress current/target/remaining.
- İzin yok ve stale veri.
- Saat dilimi değişimi.
- Toplu puan cap.

### Dijital Yaşam

- Tek motor.
- Aynı snapshot bütün tüketicilerde aynı skor.
- Confidence low davranışı.
- Weekly baseline.
- Task contribution cap.
- Top reason/action eşleme.

### Akıllı Nabız

- En fazla 3 öğe.
- TTL.
- Dedupe.
- Tür kotası.
- Görev ve skor tekrarı yok.
- Yararlı aday yoksa boş.
- Kullanıcı kapattığı tür gösterilmez.

## Compose UI test matrisi

- Görev 1/3, 3/3, riskli, veri yok.
- Dijital kart skor/delta/reason/veri birikiyor.
- Ticker 1/3, manuel geçiş, kritik item.
- Büyük font %200.
- TalkBack semantics.
- Animasyonlar kapalı.
- Açık/koyu ve farklı duvar kâğıtları.

## Gerçek cihaz matrisi

1. Ana günlük kullanılan Android telefon.
2. Temiz kurulum test telefonu.
3. İzinleri kapatıp bozma testi yapılan telefon.
4. Android tablet.

Cihaz senaryoları:

- Kullanım erişimi açık/kapalı.
- Bildirim erişimi açık/kapalı.
- Gece yarısı geçişi.
- Pazartesi hafta geçişi.
- Uygulama kapalıyken worker.
- Pil optimizasyonu.
- Yeniden başlatma/process death.
- Ekran döndürme/tablet.
- Ana ekran pager ile ticker gesture çakışması.

## Yayın kapıları

- `testDebugUnitTest` geçmeli.
- `lintDebug` kritik hata vermemeli.
- Debug APK üretilmeli.
- En az bir telefon ve tablette smoke test yapılmalı.
- Sağlık raporunda skor kaynağı tek görünmeli.
- Aynı skor ana ekranda bir kez görünmeli.
- Sabah görev ekranı açmak yıldız üretmemeli.
- Ticker yararlı içerik yokken gizlenmeli.

**Değişecek dosyalar:** Testler, gerekirse CI workflow.

**Kabul kriterleri:** Bütün yayın kapıları kanıtla geçer.

**Bağımlılıklar:** Tüm önceki döngüler (hepsi ✅, bkz. HISTORY.md arşivi).

**Durum:** 🟡 Kısmen tamamlandı — Döngü U04 — commit: 5d3befe — tarih: 2026-07-18 — Unit test/lint/çeviri eşliği ve APK kapısı kanıtlandı. FAZ A-1 (2026-07-19, cihaz: R92Y200CBKX Samsung tablet) ile 9 senaryodan 1'i (ekran döndürme/tablet) gerçek cihazda kısmen kanıtlandı: portrait/landscape/folder-grid render sorunsuz ama rotasyon+swipe kombinasyonunda LazyGrid "measure is called on a deactivated node" IllegalArgumentException crash'i tetiklendi (activity kendini otomatik toparladı, veri kaybı yok — bu crash EX03'te ayrı olarak düzeltildi, bkz. HISTORY.md). Kalan: tek telefon + temiz kurulum + izin kapatma senaryoları + kalan 8 senaryo + emülatör smoke + Compose UI test altyapısı + tam 4 cihaz matrisi — bu maddeler kanıtlanmadan roadmap tamamlandı sayılamaz.

---

# Definition of Done (tüm roadmap için)

1. Dijital skor yalnız `DigitalPulseEngine` tarafından hesaplanmalıdır. ✅
2. Ana ekranda skor yalnız Dijital Yaşam kartında birincil olarak görünmelidir. ✅
3. Pulse Clock skor tekrarından arındırılmalıdır. ✅
4. Ticker ham skoru tekrar etmemelidir. ✅
5. Görev ekranı mevcut değer, hedef ve kalan miktarı göstermelidir. ✅
6. Sabah görev ekranı açmak dönemsel göreve yıldız kazandırmamalıdır. ✅
7. Haftalık görev pazartesi–pazar sınırını kullanmalıdır. ✅
8. Görevler ekranı açılmasa bile dönem sonunda sonuçlanmalıdır. ✅
9. Ana ekran görev kartı gerçek `x/y` ve en yakın hedefi göstermelidir. ✅
10. Ticker en fazla üç yüksek değerli öğe göstermelidir. ✅
11. Yararlı ticker öğesi yoksa şerit gizlenmelidir. ✅
12. TalkBack ve büyük font desteği doğrulanmalıdır. 🟡 (U04 kalan cihaz matrisi)
13. Telefon ve tablette smoke test geçmelidir. 🟡 (U04 kalan cihaz matrisi)
14. Sağlık raporu üç sistemin durumunu göstermelidir. ✅
15. Telemetri kişisel metin veya ad göndermemelidir. ✅
16. Tüm döngülerde durum satırı commit ve tarihle `✅ Tamamlandı` olmalıdır. — U04 hariç tamam.

---

> **Son güncelleme tarihi:** 2026-07-19 — 33 tamamlanan döngü HISTORY.md arşivine taşındı; yalnız U04 (🟡) açık kaldı.
