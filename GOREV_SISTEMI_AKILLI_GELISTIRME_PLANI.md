# GÃ¶rev Sistemi â€” AkÄ±llÄ± GeliÅŸtirme PlanÄ±

> **OluÅŸturma:** 2026-07-19 Â· **Durum:** PLAN (kod yazÄ±lmadÄ±) Â· **Temel:** AkÄ±llÄ± NabÄ±z roadmap M00-M08 Ã§Ä±ktÄ±larÄ± Ã¼zerine inÅŸa edilir.

---

## 1. Mevcut Durum Analizi

### GÃ¼Ã§lÃ¼ altyapÄ± (M00-M08'de kuruldu â€” yeniden yazma GEREKMEZ)
| BileÅŸen | Durum |
|---|---|
| GÃ¶rev yaÅŸam dÃ¶ngÃ¼sÃ¼ | âœ… 8 durumlu MissionStatus + dÃ¶nem sonu settlement (WorkManager + catch-up) |
| Ä°lerleme modeli | âœ… Åu an / hedef / kalan / aÅŸÄ±m + progress bar + deadline |
| TÄ±klaâ†’aksiyon | ğŸŸ¡ VAR ama sÄ±nÄ±rlÄ±: MissionActionRouter 4 hedef (Kontrol Bekleyenler, Bildirim Raporu, KullanÄ±m Raporu, Ä°zin ayarÄ±) â€” satÄ±r sonunda kÃ¼Ã§Ã¼k buton |
| KalÄ±cÄ± gÃ¶rev Ã¶rnekleri | âœ… mission_instances tablosu â€” **baselineValue/targetValue alanlarÄ± VAR ama KULLANILMIYOR** (hazÄ±r fÄ±rsat!) |
| Adil puanlama | âœ… M08: toplu tavan, ceza yok, Â±10 pulse katkÄ±sÄ± |
| Ana ekran kartÄ± | âœ… HomeMissionCard: N/M + birincil gÃ¶rev + risk Ã¶nceliÄŸi |
| Åerit entegrasyonu | âœ… T03: AT_RISK / son adÄ±m / baÅŸarÄ± anlarÄ± ÅŸeride dÃ¼ÅŸÃ¼yor |

### ZayÄ±f noktalar (heyecan/dikkat eksikliÄŸinin kÃ¶kÃ¼)
1. **Havuz Ã§ok kÃ¼Ã§Ã¼k ve statik:** 5 gÃ¼nlÃ¼k + 2 haftalÄ±k sabit gÃ¶rev; aynÄ± 3'lÃ¼ set gÃ¼n seed'iyle dÃ¶nÃ¼yor â†’ birkaÃ§ gÃ¼nde ezberleniyor, merak Ã¶lÃ¼yor.
2. **Hedefler herkese aynÄ±:** 3 saat ekran / 30 kilit aÃ§ma â€” yoÄŸun kullanÄ±cÄ± iÃ§in imkÃ¢nsÄ±z, hafif kullanÄ±cÄ± iÃ§in anlamsÄ±z. KiÅŸiselleÅŸtirme sÄ±fÄ±r.
3. **YÄ±ldÄ±zÄ±n anlamÄ± yok:** Biriken â­ hiÃ§bir ÅŸey aÃ§mÄ±yor/gÃ¶stermiyor â€” Ã¶dÃ¼l dÃ¶ngÃ¼sÃ¼ kapanmÄ±yor.
4. **SÃ¼reklilik hissi yok:** Streak/seri yok; dÃ¼n 3/3 yapmakla hiÃ§ yapmamak arasÄ±nda gÃ¶rÃ¼nÃ¼r fark yok.
5. **Kutlama yok:** GÃ¶rev bitince sessizce tik atÄ±lÄ±yor â€” dopamin anÄ± kaÃ§Ä±yor.
6. **TÄ±klama davranÄ±ÅŸÄ± zayÄ±f:** Sadece satÄ±r sonundaki kÃ¼Ã§Ã¼k buton; satÄ±rÄ±n kendisi Ã¶lÃ¼ alan. BazÄ± gÃ¶revlerin hedef ekranÄ± hiÃ§ yok (gece gÃ¶revi, haftalÄ±k karÅŸÄ±laÅŸtÄ±rma â†’ hepsi genel kullanÄ±m raporuna gidiyor).

---

## 2. GeliÅŸtirme Paketleri (Ã¶ncelik sÄ±rasÄ±yla)

> âœ… **G1+G7 ve G2 tamamlandÄ± (2026-07-19)** â€” HISTORY.md'ye taÅŸÄ±ndÄ± (commit'ler: 7355593, G2 kapanÄ±ÅŸ commit'i HISTORY'de). Uygulama-spesifik gÃ¶rev tÄ±klamasÄ± G3'te gelecek.

### G3 â€” GÃ¶rev Ã‡eÅŸitliliÄŸi: Yeni GÃ¶rev Tipleri
Havuzu 7'den ~18'e Ã§Ä±kar; gÃ¼nlÃ¼k 3'lÃ¼ seÃ§im **aÄŸÄ±rlÄ±klÄ±** olur (kullanÄ±cÄ±nÄ±n zayÄ±f alanÄ±na Ã¶ncelik â€” InsightEngine/PulseScoreReason'dan beslenir):
- **Uygulama-spesifik:** "BugÃ¼n [en Ã§ok kullandÄ±ÄŸÄ±n sosyal uygulama]'yÄ± 45 dk altÄ±nda tut" â€” hedef uygulama kullanÄ±cÄ±nÄ±n verisinden seÃ§ilir, adÄ± gÃ¶revde gÃ¶rÃ¼nÃ¼r (Firebase'e ASLA gitmez â€” U02 kuralÄ±).
- **DÃ¼zen gÃ¶revleri:** "2 kategorisiz uygulamayÄ± yerleÅŸtir", "Bir klasÃ¶re emoji/renk ver", "HiÃ§ aÃ§madÄ±ÄŸÄ±n 1 uygulamayÄ± kaldÄ±r veya gizle".
- **Odak gÃ¶revi:** "BugÃ¼n 1 kez Focus Mode'da 45 dk geÃ§ir" (P18 preset'iyle entegre â€” aÃ§Ä±lÄ±ÅŸ/kapanÄ±ÅŸ zamanÄ± Ã¶lÃ§Ã¼lÃ¼r).
- **KeÅŸif gÃ¶revleri (nadir, %10 aÄŸÄ±rlÄ±k):** "AkÄ±llÄ± NabÄ±z ayarlarÄ±na gÃ¶z at", "HaftalÄ±k raporunu incele" â€” Ã¶zellik keÅŸfi + retention.
- **Sabah pozitifi:** "Ä°lk 30 dk'da sosyal medya aÃ§madan baÅŸla" (gece gÃ¶revinin sabah simetriÄŸi).
- Kural: AynÄ± gÃ¼n en az 1 "kaÃ§Ä±nma" + 1 "eylem" gÃ¶revi karÄ±ÅŸÄ±r (hepsi pasif bekleyiÅŸ olmasÄ±n â€” eylem gÃ¶revleri anÄ±nda tamamlanabilir, tatmin verir).

> âœ… **G4 tamamlandÄ± (2026-07-19)** â€” HISTORY.md'de.

### G5 â€” Kutlama & Mikro-etkileÅŸim
- GÃ¶rev tamamlanma ANI: satÄ±rda tek seferlik yÄ±ldÄ±z patlamasÄ± animasyonu + kÄ±sa haptic (reduced-motion'da sadece haptic + renk).
- 3/3 gÃ¼nÃ¼: HomeMissionCard'da konfeti benzeri hafif parÄ±ltÄ± + "BugÃ¼nÃ¼ topladÄ±n â­â­â­".
- DÃ¶nem sonu (settlement worker zaten gece Ã§alÄ±ÅŸÄ±yor): sabah ilk aÃ§Ä±lÄ±ÅŸta tek satÄ±rlÄ±k Ã¶zet â€” "DÃ¼n 2/3 tamamladÄ±n, serin 5 gÃ¼nde" (ticker WEEKLY_REPORT tipi; ayrÄ± bildirim Ä°STEÄE BAÄLI, varsayÄ±lan kapalÄ±).
- TÃ¼mÃ¼ KEY_ ile kapatÄ±labilir (CLAUDE.md ayar kuralÄ±).

### G6 â€” YÄ±ldÄ±z Ekonomisi (Ã¶dÃ¼l dÃ¶ngÃ¼sÃ¼nÃ¼ kapat)
- **Seviye:** Toplam â­ â†’ seviye adlarÄ± ("Ã‡aylak â†’ DÃ¼zenli â†’ OdaklÄ± â†’ Denge UstasÄ±" â€” nÃ¶tr, yargÄ±sÄ±z dil). GÃ¶revler ekranÄ± baÅŸlÄ±ÄŸÄ±nda rozet.
- **Kozmetik aÃ§Ä±lÄ±mlar:** Belirli seviyelerde klasÃ¶r emoji paketi / Pulse Clock stili / tema varyantÄ± aÃ§Ä±lÄ±r (mevcut kozmetik varlÄ±klar kilitlenmez â€” sadece YENÄ° eklenenler seviyeyle gelir; mevcut kullanÄ±cÄ± hiÃ§bir ÅŸey kaybetmez).
- **Dijital YaÅŸam baÄŸÄ±:** Zaten Â±10 katkÄ± var â€” seviye atlama anÄ± PULSE_CHANGE olarak ÅŸeride dÃ¼ÅŸer.
- Para/satÄ±n alma YOK â€” tamamen iÃ§sel motivasyon.

### G8 â€” Cihaz DÃ¼zeni Ä°Ã§gÃ¶rÃ¼leri (HÃ¼seyin talebi, 2026-07-19)
**Karar gerekÃ§esi:** "Telefonunuz yavaÅŸ Ã§alÄ±ÅŸÄ±yor" tarzÄ± teÅŸhis YAPILMAZ â€” kanÄ±tlanamaz (Android 3. parti uygulamaya CPU/RAM baskÄ±sÄ± vermez), sahte "cleaner app" imajÄ± yaratÄ±r ve Play aldatÄ±cÄ± davranÄ±ÅŸ politikasÄ± inceleme reddi riski taÅŸÄ±r. Bunun yerine: **sayÄ± veren, kanÄ±tlanabilir, tek dokunuÅŸla eyleme baÄŸlanan dÃ¼zen fÄ±rsatlarÄ±.**
- **Depolama fÄ±rsatÄ±:** StorageStatsManager ile â€” "Depolama %91 dolu; 6 aydÄ±r aÃ§madÄ±ÄŸÄ±n 9 uygulama 3,2 GB tutuyor â†’ Ä°ncele". Ä°ddia her zaman doÄŸrulanabilir sayÄ±yla.
- **KullanÄ±lmayan uygulama temizliÄŸi:** Mevcut CLEANUP_UNUSED reason'Ä± kaldÄ±rma akÄ±ÅŸÄ±na baÄŸlanÄ±r (Ã§oklu seÃ§im + sistem uninstall dialogu).
- **Bildirim yÃ¼kÃ¼ Ã¶zeti:** Mevcut NotificationAnalyzer verisi â€” "Bu hafta 412 bildirim, %70'i 3 uygulamadan â†’ Rapor".
- **Ã–z-tanÄ± (dÃ¼rÃ¼st saÄŸlÄ±k):** DiagnosticsReportManager'dan yalnÄ±z KENDÄ° uygulamamÄ±zÄ±n dÃ¼zeltilebilir sorunlarÄ± â€” "KullanÄ±m izni kapanmÄ±ÅŸ, skorun gÃ¼ncellenemiyor â†’ DÃ¼zelt". BaÅŸka uygulamalar/sistem hakkÄ±nda yargÄ± YOK.
- **Dil kurallarÄ± (mutlak):** Korku dili yasak ("yavaÅŸ", "tehlike", "acil"); her iÃ§gÃ¶rÃ¼ SAYI iÃ§erir; "temizle!" emri deÄŸil "istersen incele" daveti; genel cihaz yargÄ±sÄ± asla.
- **Entegrasyon:** Ä°Ã§gÃ¶rÃ¼ler ticker CONTEXTUAL_SUGGESTION tipi + G3 dÃ¼zen gÃ¶revleri olarak yaÅŸar ("BugÃ¼n 2 kullanÄ±lmayan uygulamayÄ± kaldÄ±r â†’ â­"); Dashboard'da ayrÄ± bÃ¶lÃ¼m GEREKMEZ (tekrar Ã¶nleme 1.4 kuralÄ±).
- **Ayar:** KEY_DEVICE_TIDINESS_INSIGHTS (varsayÄ±lan aÃ§Ä±k) â€” tek toggle ile tamamen kapanÄ±r.
- **Play policy notu:** Uninstall Ã¶nerileri sistem dialoguyla yapÄ±lÄ±r (sessiz kaldÄ±rma yok); depolama sayÄ±larÄ± StorageStats izni gerektirirse izin CTA'sÄ± dÃ¼rÃ¼st metinle sunulur.

---

## 3. Ã–nerilen DÃ¶ngÃ¼ SÄ±rasÄ± ve Puanlama (FÄ°KÄ°RLER kuralÄ±: DeÄŸer+Uygulanabilirlik+Risk+Etki, her biri 1-5)

| DÃ¶ngÃ¼ | Ä°Ã§erik | Puan | Not |
|---|---|---|---|
| ~~G1~~ | âœ… TamamlandÄ± 2026-07-19 | 19 | HISTORY'de |
| ~~G2~~ | âœ… TamamlandÄ± 2026-07-19 | 18 | HISTORY'de |
| G3 | GÃ¶rev Ã§eÅŸitliliÄŸi (18 gÃ¶rev + aÄŸÄ±rlÄ±klÄ± seÃ§im) | **17** (5+4+4+4) | En bÃ¼yÃ¼k iÃ§erik iÅŸi; 2 dÃ¶ngÃ¼ye bÃ¶lÃ¼nebilir (G3a Ã§ekirdek tipler, G3b uygulama-spesifik) |
| G4 | Streak | **17** (5+5+4+3) | KÃ¼Ã§Ã¼k iÅŸ, bÃ¼yÃ¼k retention etkisi |
| G5 | Kutlama | **15** (4+4+4+3) | G4 ile birlikte anlamlÄ± |
| G6 | YÄ±ldÄ±z ekonomisi | **14** (4+3+4+3) | Kozmetik varlÄ±k Ã¼retimi gerektirir â€” sona |
| ~~G7~~ | âœ… TamamlandÄ± 2026-07-19 (G1'e iliÅŸtirildi) | 13 | HISTORY'de |
| G8 | Cihaz dÃ¼zeni iÃ§gÃ¶rÃ¼leri | **16** (5+4+3+4) | HÃ¼seyin talebi; korku dili yasak, sayÄ±lÄ±/eylemli fÄ±rsat Ã§erÃ§evesi |

**Kalan uygulama sÄ±rasÄ±:** G4 â†’ G8 â†’ G3a â†’ G5 â†’ G3b â†’ G6
Toplam tahmin: 7-8 dÃ¶ngÃ¼. Her dÃ¶ngÃ¼ tek commit, kademeli doÄŸrulama, ara APK yok (kullanÄ±cÄ± kuralÄ±).

## 4. KÄ±rmÄ±zÄ± Ã‡izgiler (mevcut ilkeler korunur)
- Erken Ã¶dÃ¼l YOK (M00/M04 settlement dÃ¼zeni bozulmaz).
- Reddetme/baÅŸarÄ±sÄ±zlÄ±k CEZASIZ (M08).
- Uygulama/kiÅŸi adlarÄ± telemetriye ASLA gitmez (U02 â€” uygulama-spesifik gÃ¶revlerde Ã¶zellikle kritik).
- Ham skor/rutin ilerleme ÅŸeride sÄ±zmaz (1.4 tekrar tablosu).
- Her yeni gÃ¶rÃ¼nÃ¼r Ã¶zellik Settings toggle'Ä± ile kapatÄ±labilir.
- Onboarding sÄ±rasÄ± deÄŸiÅŸmez.
