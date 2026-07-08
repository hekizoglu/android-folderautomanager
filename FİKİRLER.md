# FİKİRLER.md — AppOrganizer Fikir & Görev Havuzu

> Yeni özellik fikirleri, döngüden gelen görevler, backlog adayları buraya eklenir.
> 15+ puan alanlar ROADMAP.md'ye de taşınır. Tamamlananlar → HISTORY.md arşiv.
> Telegram onayı alındıktan sonra fikir hayata geçirilir.

---

## Nasıl Kullanılır

- Claude her döngü sonunda yeni fikri/görevi buraya ekler
- Her madde: tarih + kaynak + öncelik + kısa açıklama
- Onay gelince: `[ONAYLANDI 2026-xx-xx]` etiketi + uygulama başlar
- Tamamlanınca: `[TAMAMLANDI]` etiketi → HISTORY.md'ye taşınır

**Öncelik:** 🔴 Kritik · ⭐ Yüksek Puanlı (15+) · 🟡 Orta (12-14) · 🟢 Düşük · ⚪ Fikir

---

## 🔴 Kritik

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-16 | ROADMAP | **QUERY_ALL_PACKAGES Play Store beyan formu** — göndermeden önce zorunlu, aksi halde APK reddedilir | Bekliyor ⚠️ |

---

## ⭐ Yüksek Puanlı (≥15p)

| Tarih | Puan | Madde | Durum |
|-------|------|-------|-------|
| 2026-07-07 | 18p | **Play Store Privacy/Data Safety uyum paketi** — `play-store-hazirlik-risk-raporu.md` + `izin-veri-haritasi.md`: privacy policy, Data Safety, Firebase/Crashlytics/Analytics/FCM, DeepSeek, Drive/SAF ve package inventory beyanlari ayni hikayeye cekilmeli. (KV:5 · U:5 · BR:5 · EA:3 = **18p**) | Kısmen tamamlandı (Döngü 214+215) — privacy_policy.html Firebase/Contacts/Bildirim/Accessibility Service celiskileri duzeltildi, Firebase Analytics'ten package_name kaldirildi; Play Console Data Safety formu doldurma dış aksiyon olarak kalıyor |
| 2026-07-07 | 16p | **Play Store release imza ve submission kapisi** — release keystore, content rating, QUERY_ALL_PACKAGES declaration ve final AAB temiz committen build edilmeden yayin tamamlanamaz. (KV:5 · U:4 · BR:4 · EA:3 = **16p**) | Bekliyor — dış aksiyon (keystore/Play Console) gerekli |
| 2026-07-07 | 15p | **Gercek cihaz Play-oncesi QA paketi** — Android 14 NotificationListener, Play screenshot smoke, backup/restore, worker schedule ve blur/API26 testleri tek kanitli test paketinde kosulmali. (KV:4 · U:4 · BR:4 · EA:3 = **15p**) | Bekliyor |
| 2026-07-08 | 15p | **Akıllı Bildirim Analiz Sistemi** — `AppNotificationListenerService` + `notification_events` + `NotificationAnalyzer` + `SmartInsightWorker` hattı mevcut ama privacy-first rapor/öneri/günlük akıllı bildirim sistemine tamamlanmamış (worker duplicate-schedule riski, saat değişince yeniden planlama, POST_NOTIFICATIONS yoksa sessiz devam, Privacy Policy/Data Safety metniyle birebir uyum). Tam kabul kriterleri ve modül listesi → ROADMAP.md "🧠 Akıllı Bildirim Analiz Sistemi — Detay". (KV:4 · U:3 · BR:4 · EA:4 = **15p**) | Bekliyor — ROADMAP R7 ile eşleşiyor |

---

## 🟡 Orta Öncelik (12-14p)

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-07-06 | Fable D199 | **Onboarding sonrası "ilk izlenim" emülatör testi** — onboarding sonrası ilk açılış deneyimi (RoleManager dialog + klasör oluşturma süresi + ilk render) test edilmedi. Her 18 döngüde bir tam teste eklensin. (KV:4 · U:5 · BR:2 · EA:3 = **14p**) | Bekliyor |
| 2026-07-07 | Rekabet (Smart Launcher/Niagara) | **Home ekranı/dock'ta kullanım sıklığına göre otomatik sıralama** — Niagara pattern. `lastUsedTimestamp` alanı zaten Room'da mevcut (MIGRATION_3_4), sadece dock/klasör içi sıralama mantığı eksik. (KV:4 · U:3 · BR:3 · EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-etki-matrisi.md` çıkar** — tüm `AppPrefs` key'leri + ayar ViewModel state'leri için UI konumu / yazıldığı yer / okunduğu yer / gerçek davranış / sorun / aksiyon tablosu. "Ayar UI'da var" ile "ayar gerçekte çalışıyor" ayrımını netleştirir, diğer 5 inceleme bunun üzerine kurulu. (KV:4·U:4·BR:2·EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | play-store-hazirlik-risk-raporu.md | **Play Store screenshot paketi** — Pixel 6 light/dark seti, privacy hassas ekranlarda kisisel veri olmadan alinmali; store listing ve QA pack ayni gorsel hikayeyi anlatmali. (KV:4 · U:4 · BR:2 · EA:4 = **14p**) | Bekliyor — dış aksiyon (emülatör screenshot çekimi) |
| 2026-07-07 | yedekleme-geri-yukleme-guvenilirlik-raporu.md | **Backup kapsam matrisi ve eksik ayarlar** — yedek "tum ayarlar" iddiasi tasiyor ama Smart/Search/Widget/Theme gibi ayarlar eksik olabilir; her AppPrefs key'i backup'a giriyor mu cikarilmali. (KV:4 · U:4 · BR:3 · EA:3 = **14p**) | Bekliyor |
| 2026-07-07 | yedekleme-geri-yukleme-guvenilirlik-raporu.md | **Restore sonrasi global UI refresh stratejisi** — import AppPrefs/DockPrefs/Room guncelliyor; listener olmayan ekranlar eski kalabilir, restart veya global invalidation karari gerekli. (KV:3 · U:4 · BR:3 · EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | performans-bellek-build-raporu.md | **Performans benchmark sonuclari dosyasi** — build suresi, cold start, Home idle bellek, AllApps search jank ve APK boyutu olculup `performans-benchmark-sonuclari.md` icine islenmeli. (KV:4 · U:4 · BR:2 · EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | performans-bellek-build-raporu.md | **App startup init listesini sadelestir** — `AppOrganizerApp.onCreate` Firebase, worker schedule, FCM token, channel ve analytics baslatiyor; zorunlu/ertelenebilir isler ayrilmali. (KV:4 · U:3 · BR:3 · EA:3 = **13p**) | Bekliyor |
| 2026-07-07 | ana-ekran-rekabet-revizyon-raporu.md | **Home UX karar listesi** — otomatik klasorleme, search bar, dock/favorites/recent/widget yuzeyleri icin kalacak-gidecek-yeniden gruplancak karar listesi cikarilmali. (KV:3 · U:4 · BR:2 · EA:3 = **12p**) | Bekliyor |
| 2026-07-07 | ana-ekran-rekabet-revizyon-raporu.md | **Minimal Search Mode / Focus Mode revizyonu** — klasor istemeyen kullanici icin search bar + favorites + recent odakli mod, mevcut Focus Mode uzerinden yeniden tasarlanmali. (KV:3 · U:4 · BR:2 · EA:3 = **12p**) | Bekliyor |

---

## ⏸ Beklet (≤11p)

| Tarih | Kaynak | Madde | Puan |
|-------|--------|-------|------|
| 2026-06-29 | Rekabet | **Online App Category DB** — Opsiyonel, gizlilik riski yüksek. Lokal → keyword → kullanıcı override → en son online. | 10p |
| 2026-06-29 | Hüseyin | **claude-code-templates mobile-design skill** — Tablet/foldable desteği planlandığında ekle. | 9p |
| 2026-07-07 | Rekabet (Smart Launcher/Niagara) | **Ayarlar'da grid yoğunluğu (satır/sütun) manuel slider'ı** — Smart Launcher'ın Fluid Grid pattern'i; mevcut grid sabit mi kontrol edilmeli, sabitse ayarlanabilir hale getirilsin. (KV:3 · U:3 · BR:2 · EA:3) | 11p |
| 2026-07-07 | ayarşar-raporlar.md | **Haftalık Uygulama Raporu ile Akıllı Bildirimler/Kullanılmayan Uygulamalar kavramsal çakışıyor** — ikisi de "uzun süredir açılmayan uygulama" bildirimi; Haftalık Uygulama Raporu Bildirimler ekranına taşınmalı. (KV:2·U:3·BR:3·EA:2) | 10p |
| 2026-07-07 | ayarşar-raporlar.md | **"Hakkında & Yedekleme" ekranı çok fazla iş yapıyor** — hakkında/gizlilik/yedek/Drive/restore/tanıtım/dashboard/debug aynı alt ekranda; ayrı alt sayfalara bölünmeli. (KV:3·U:3·BR:3·EA:2) | 11p |
| 2026-07-07 | ayarşar-raporlar.md | **DeepSeek API key düz metin SharedPreferences'ta saklanıyor** — `SettingsAppsSection.kt:164`; EncryptedSharedPreferences'a taşınmalı veya en azından risk uyarısı eklenmeli. Güvenlik ilgili, öncelik yükseltilebilir. (KV:4·U:2·BR:2·EA:3) | 11p |
| 2026-07-07 | ayarşar-raporlar.md | **Ayarlar hub'ına "ayar ara" ekle** — bu kadar çok ayar varken arama kolaylaştırır. (KV:3·U:3·BR:2·EA:3) | 11p |
| 2026-07-07 | ayarşar-raporlar.md | **Kart radius 16dp → 8-12dp'ye indirilebilir** — repo UI disiplinine göre ayarlar ekranı daha dense/operasyonel olmalı (`SettingsComponents.kt:35`). (KV:1·U:2·BR:1·EA:5) | 9p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-gizlilik-izin-uyum-raporu.md` çıkar** — NotificationListener/UsageStats/Contacts/File indexing/QUERY_ALL_PACKAGES/Crashlytics/FCM/DeepSeek/Drive verilerinin ne sakladığını Play Data Safety beyanıyla karşılaştır. Play Store gönderimi öncesi risk taşır. (KV:4·U:3·BR:2·EA:2) | 11p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-worker-zamanlama-raporu.md` çıkar** — BackupWorker/SmartInsightWorker/WeeklyDigestWorker'ın toggle ile schedule/cancel bağını, seçilen gün/saat/dakikanın hesaba katılıp katılmadığını ve duplicate notification riskini doğrula. (KV:3·U:3·BR:3·EA:2) | 11p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-state-reaktivite-raporu.md` çıkar** — hangi ayarların SharedPreferences listener ile reaktif, hangilerinin sadece ilk açılışta `remember` ile okunduğunu tara; restore/import/onboarding sonrası UI'ın eski kalıp kalmadığını doğrula. (KV:3·U:3·BR:2·EA:3) | 11p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-test-senaryolari.md` çıkar** — bildirim erişimi reaktifliği, çift tık arama/gesture çakışması, arama geçmişi limiti, dosya arama varsayılanı, yedekleme/SmartInsight worker schedule, ikon pack canlı güncelleme gibi 9 senaryoyu emülatörde uçtan uca doğrula. (KV:3·U:3·BR:2·EA:3) | 11p |
| 2026-07-07 | ayarlar-inceleme-talepleri.md | **`ayar-bilgi-mimarisi-raporu.md` çıkar** — hub rozetleri, Hakkında/Yedekleme bölünmesi, Launcher>Ana ekran alt gruplama, tekrar eden ayarların hangi ekranda kalacağı ve tehlikeli aksiyon onay standardı kullanıcı akışı olarak incelensin. (KV:2·U:3·BR:1·EA:3) | 9p |
| 2026-07-07 | yedekleme-geri-yukleme-guvenilirlik-raporu.md | **Backup JSON'a eski appName ekle** — restore eksik paket dialogunda sadece packageName yerine eski uygulama adi + packageName gosterilsin. (KV:2 · U:3 · BR:1 · EA:4) | 10p |
| 2026-07-07 | yedekleme-geri-yukleme-guvenilirlik-raporu.md | **Yedekte usage verisini dahil etme secenegi** — backup JSON usageCount/lastUsedTimestamp/hidden state tasiyor; kullaniciya hassas veri dahil etme secenegi verilebilir. (KV:3 · U:2 · BR:3 · EA:3) | 11p |
| 2026-07-07 | ana-ekran-rekabet-revizyon-raporu.md | **Permission Center kartini sade anlatimla yenile** — Contacts/files/bildirim/usage izinleri icin "kapaliysa ne kaybedersin, aciksa ne kazanirsin" dilini tek kartta toparla. (KV:3 · U:3 · BR:2 · EA:3) | 11p |
| 2026-07-07 | performans-bellek-build-raporu.md | **Worker schedule listesini app acilisinda logla** — Backup/SmartInsight/WeeklyDigest/FilesIndex worker durumlari debug logda gorulsun, duplicate schedule takibi kolaylassin. (KV:2 · U:3 · BR:2 · EA:4) | 11p |
| 2026-07-07 | ana-ekran-rekabet-revizyon-raporu.md | **Home ilk ekran deger onerisi testi** — ilk ekranda "otomatik klasorlendi + arama hazir + dock akilli" mesaji 3 saniyede anlasiliyor mu screenshot/QA ile kontrol edilsin. (KV:3 · U:3 · BR:1 · EA:3) | 10p |

---

## 📊 Rekabet Pozisyonlama Özeti

| Rakip | Bizim aldığımız | Kalan fark |
|-------|----------------|------------|
| KISS | UsageScore + Smart Search fikri | KISS'in arama hızı (native C) |
| Lawnchair | Pixel hissi korunuyor | Icon pack, global search |
| mLauncher | Gesture fikri | Biometric lock |
| Neo | Backup/kategori fikri | Icon shape, drawer özelleştirme |
| Fossify | Privacy Center fikri | Tam offline mimari |
| Kvaesitso | Search provider mimarisi | Plugin sistemi |
| Pie Launcher | Quick Wheel fikri | Tam radyal nav |
| Smart Launcher | Otomatik kategori atama vaadi (en yakın rakip) | Fluid Grid yoğunluk ayarı, gesture bar |
| Niagara Launcher | Arama-öncelikli minimal ana ekran fikri | Kullanım sıklığına göre otomatik dock sıralama |

**Hedef cümle:** "AppOrganizer, uygulamalarını otomatik klasörleyen; kullanım alışkanlığına göre dock, arama ve önerileri akıllandıran privacy-first Android launcher'dır."

---

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda | Son güncelleme: 2026-07-07*
*Not: 2026-06-29 rekabet analizi [TAMAMLANDI] maddeleri HISTORY.md Tamamlananlar Arşivi'ne taşındı (v1.2.0 döngüsü). S1/S2/K1 (Döngü 207-208) tamamlandı, HISTORY.md'ye taşındı. Ayarlar audit'inden 10 madde Döngü 213'te, Play Store privacy uyumu (Accessibility Service beyanı dahil) Döngü 214-215'te tamamlandı, HISTORY.md'ye taşındı. R1-R7 (DOCS_SCORE_HIGH) artık `scripts/score_docs_backlog.ps1` içinde gerçek kaynaklarla (FİKİRLER.md/HISTORY.md/ROADMAP.md) tanımlı — önceki phantom rapor dosyası referansları (Döngü 215'te) düzeltildi.*
