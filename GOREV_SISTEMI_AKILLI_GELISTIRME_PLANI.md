# Görev Sistemi - Akıllı Geliştirme Planı

> **Oluşturma:** 2026-07-19 · **Durum:** PLAN (kod yazılmadı) · **Temel:** Akıllı Nabız roadmap M00-M08 çıktıları üzerine inşa edilir.

---

## 1. Mevcut Durum Analizi

### Güçlü altyapı (M00-M08'de kuruldu - yeniden yazma GEREKMEZ)
| Bileşen | Durum |
|---|---|
| Görev yaşam döngüsü | ✅ 8 durumlu MissionStatus + dönem sonu settlement (WorkManager + catch-up) |
| İlerleme modeli | ✅ Åu an / hedef / kalan / aşım + progress bar + deadline |
| Tıkla→aksiyon | 🟡 VAR ama sınırlı: MissionActionRouter 4 hedef (Kontrol Bekleyenler, Bildirim Raporu, Kullanım Raporu, İzin ayarı) - satır sonunda küçük buton |
| Kalıcı görev örnekleri | ✅ mission_instances tablosu - **baselineValue/targetValue alanları VAR ama KULLANILMIYOR** (hazır fırsat!) |
| Adil puanlama | ✅ M08: toplu tavan, ceza yok, ±10 pulse katkısı |
| Ana ekran kartı | ✅ HomeMissionCard: N/M + birincil görev + risk önceliği |
| Åerit entegrasyonu | ✅ T03: AT_RISK / son adım / başarı anları şeride düşüyor |

### Zayıf noktalar (heyecan/dikkat eksikliğinin kökü)
1. **Havuz çok küçük ve statik:** 5 günlük + 2 haftalık sabit görev; aynı 3'lü set gün seed'iyle dönüyor → birkaç günde ezberleniyor, merak ölüyor.
2. **Hedefler herkese aynı:** 3 saat ekran / 30 kilit açma - yoğun kullanıcı için imkânsız, hafif kullanıcı için anlamsız. Kişiselleştirme sıfır.
3. **Yıldızın anlamı yok:** Biriken ⭐ hiçbir şey açmıyor/göstermiyor - ödül döngüsü kapanmıyor.
4. **Süreklilik hissi yok:** Streak/seri yok; dün 3/3 yapmakla hiç yapmamak arasında görünür fark yok.
5. **Kutlama yok:** Görev bitince sessizce tik atılıyor - dopamin anı kaçıyor.
6. **Tıklama davranışı zayıf:** Sadece satır sonundaki küçük buton; satırın kendisi ölü alan. Bazı görevlerin hedef ekranı hiç yok (gece görevi, haftalık karşılaştırma → hepsi genel kullanım raporuna gidiyor).

---

## 2. Geliştirme Paketleri (öncelik sırasıyla)

> ✅ **G1+G7 ve G2 tamamlandı (2026-07-19)** - HISTORY.md'ye taşındı (commit'ler: 7355593, G2 kapanış commit'i HISTORY'de). Uygulama-spesifik görev tıklaması G3'te gelecek.

> ✅ **G3a+G3b tamamlandı (2026-07-19)** — HISTORY.md'de. Havuz 7→13, ağırlıklı seçim + uygulama-spesifik limit görevi.

> ✅ **G4 tamamlandı (2026-07-19)** - HISTORY.md'de.

> ✅ **G5 tamamlandı (2026-07-19)** — HISTORY.md'de.

### G6 - Yıldız Ekonomisi (ödül döngüsünü kapat)
- **Seviye:** Toplam ⭐ → seviye adları ("Çaylak → Düzenli → Odaklı → Denge Ustası" - nötr, yargısız dil). Görevler ekranı başlığında rozet.
- **Kozmetik açılımlar:** Belirli seviyelerde klasör emoji paketi / Pulse Clock stili / tema varyantı açılır (mevcut kozmetik varlıklar kilitlenmez - sadece YENİ eklenenler seviyeyle gelir; mevcut kullanıcı hiçbir şey kaybetmez).
- **Dijital Yaşam bağı:** Zaten ±10 katkı var - seviye atlama anı PULSE_CHANGE olarak şeride düşer.
- Para/satın alma YOK - tamamen içsel motivasyon.

> ✅ **G8 tamamlandı (2026-07-19)** — HISTORY.md'de. Uninstall çoklu seçim akışı G3b ile derinleşebilir.

---

## 3. Önerilen Döngü Sırası ve Puanlama (FİKİRLER kuralı: Değer+Uygulanabilirlik+Risk+Etki, her biri 1-5)

| Döngü | İçerik | Puan | Not |
|---|---|---|---|
| ~~G1~~ | ✅ Tamamlandı 2026-07-19 | 19 | HISTORY'de |
| ~~G2~~ | ✅ Tamamlandı 2026-07-19 | 18 | HISTORY'de |
| ~~G3~~ | ✅ Tamamlandı 2026-07-19 (a+b) | 17 | HISTORY'de |
| ~~G4~~ | ✅ Tamamlandı 2026-07-19 | 17 | HISTORY'de |
| ~~G5~~ | ✅ Tamamlandı 2026-07-19 | 15 | HISTORY'de |
| G6 | Yıldız ekonomisi | **14** (4+3+4+3) | Kozmetik varlık üretimi gerektirir - sona |
| ~~G7~~ | ✅ Tamamlandı 2026-07-19 (G1'e iliştirildi) | 13 | HISTORY'de |
| ~~G8~~ | ✅ Tamamlandı 2026-07-19 | 16 | HISTORY'de |

**Kalan uygulama sırası:** G6
Toplam tahmin: 7-8 döngü. Her döngü tek commit, kademeli doğrulama, ara APK yok (kullanıcı kuralı).

## 4. Kırmızı Çizgiler (mevcut ilkeler korunur)
- Erken ödül YOK (M00/M04 settlement düzeni bozulmaz).
- Reddetme/başarısızlık CEZASIZ (M08).
- Uygulama/kişi adları telemetriye ASLA gitmez (U02 - uygulama-spesifik görevlerde özellikle kritik).
- Ham skor/rutin ilerleme şeride sızmaz (1.4 tekrar tablosu).
- Her yeni görünür özellik Settings toggle'ı ile kapatılabilir.
- Onboarding sırası değişmez.