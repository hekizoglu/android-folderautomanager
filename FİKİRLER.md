# FİKİRLER.md — AppOrganizer Fikir & Görev Havuzu

> Yeni özellik fikirleri, döngüden gelen görevler, backlog adayları buraya eklenir.
> ROADMAP.md artık değiştirilmez — yeni her şey buraya gelir.
> Telegram onayı alındıktan sonra fikir hayata geçirilir.

---

## Nasıl Kullanılır

- Claude her döngü sonunda yeni fikri/görevi buraya ekler
- Her madde: tarih + kaynak + öncelik + kısa açıklama
- Onay gelince: `[ONAYLANDI 2026-xx-xx]` etiketi + uygulama başlar
- Tamamlanınca: `[TAMAMLANDI]` etiketi

**Öncelik:** 🔴 Kritik · 🟡 Orta · 🟢 Düşük · ⚪ Fikir (tartışılacak)

---

## 🔴 Kritik

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-20 | Denetim #13 | **FCM push mimari kararı LEARNINGS.md'ye eklenmeli** — `AppFirebaseMessagingService.kt` + `AppOrganizerApp.kt` FCM init belgelenmedi | [TAMAMLANDI] |
| 2026-06-16 | ROADMAP | **QUERY_ALL_PACKAGES Play Store beyan formu** — göndermeden önce zorunlu, aksi halde APK reddedilir | Bekliyor ⚠️ |

---

## 🟡 Orta Öncelik

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-21 | D100 sonrası | **Klasör sıra numarasıyla yer değiştirme** — kullanıcı "3. klasörü 7. sıraya taşı" gibi numara girerek taşıyabilsin | [TAMAMLANDI — D114 denetiminde zaten implementali: FolderSheet AlertDialog + onMove callback] |
| 2026-06-20 | Denetim #11 | **Onboarding adım sırası güncellenmeli** — CLASSIFY_MODE adımı eksik, CLAUDE.md "14+2" diyor ama LEARNINGS.md 14 adım | [TAMAMLANDI D105 — 16 adım doğrulandı, CLAUDE.md güncellendi] |
| 2026-06-20 | Denetim #14 | **Merge conflict AppClassifier kural CLAUDE.md §5'e promote** — 4+ tekrar, eşik geçildi | [TAMAMLANDI] |
| 2026-06-16 | Döngü 86 | **AllAppsDrawer/FolderSheet dark mode regresyon kontrolü** — D91 fix sonrası FCM commit tekrar değiştirdi | [TAMAMLANDI D96] |
| 2026-06-15 | ROADMAP | **Android 14 NotificationListenerService gerçek cihaz testi** | Bekliyor |
| 2026-06-15 | Döngü 43 | **AppNotificationListenerService ilk açılışta restart** — gerçek cihaz test gerekli | Bekliyor |
| 2026-06-15 | ROADMAP | **Firebase Crashlytics API kurulumu** — `google-services.json` + service account | Bekliyor |
| 2026-06-15 | ROADMAP | **Privacy Policy sayfası** — GitHub Pages tek HTML, içerik onayı gerekli ⚠️ | [TAMAMLANDI D121 — docs/index.html + PP web linki uygulamaya eklendi. Kalan: repo Settings'ten Pages aktifleştir] |
| 2026-06-15 | ROADMAP | **Screenshots** — Pixel 6 emülatörü, light + dark mode | Bekliyor |
| 2026-06-15 | ROADMAP | **Content rating anketi** — Play Store ⚠️ | Bekliyor |
| 2026-06-16 | ROADMAP | **BLUR-4: Gerçek cihaz testi** — blur performansı + API 26 uyumu | Bekliyor |
| 2026-06-16 | ROADMAP | **`cycle.ps1` uçtan uca test** — build → push → Telegram | Bekliyor |

---

## 🟢 Düşük Öncelik

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-15 | Döngü 43 | **AllApps double-tap emülatörde doğrulanamadı** — gerçek cihaz testi | Bekliyor |
| 2026-06-15 | Döngü 41 | **Üretici kategorileri gerçek cihaz testi** — 9 yeni kategori, onboarding'den "üreticiye göre" | Bekliyor |
| 2026-06-16 | ROADMAP | **Hilt DI kurulumu** — manuel `new()` çağrılarını temizle | [TAMAMLANDI D117 — utils/CategoryLLMFallback silindi, domain versiyonu inject edildi] |
| 2026-06-16 | ROADMAP | **Unit test coverage** — ViewModel'ler MockK testleri | [TAMAMLANDI D118 — 156 test, 9 sınıf, C:\AppOrg junction fix] |
| 2026-06-16 | ROADMAP | **Dark mode tam uyum audit** | [TAMAMLANDI D114] |
| 2026-06-16 | ROADMAP | **Multi-language support (TR/EN)** | [TAMAMLANDI D113] |

---

## ⚪ Fikirler (Tartışılacak)

| Tarih | Kaynak | Fikir | Not |
|-------|--------|-------|-----|
| 2026-06-21 | D103 | **Ayarlar — Kullanıcı Talep/Öneri Formu** — "Talep Gönder" butonu: e-posta Intent ile `huseyinekizoglu@gmail.com`'a yönlendirir | [TAMAMLANDI] |
| 2026-06-15 | ROADMAP | **AppClassifier'ı JSON asset'e böl** — 3717 paketlik mapOf şişiyor; `assets/app_categories.json` + runtime parse | [TAMAMLANDI D115] |
| 2026-06-15 | ROADMAP | **Kendi sunucu API'si** — `packageName → category` endpoint, APK güncellemeden DB güncellenebilir | Tartışılacak |
| 2026-06-16 | ROADMAP | **Akıllı Uygulama Önerileri (30dk)** — kullanım alışkanlığına göre değişen öneri satırı | [TAMAMLANDI D116 — saat dilimine göre dinamik başlık + UsageStatsHelper ağırlıklı skor] |
| 2026-06-16 | ROADMAP | **Wear OS companion app** | ⏸ İşleme alınmadı (puan: 8) — HISTORY'de |
| 2026-06-16 | ROADMAP | **Tablet layout (large screen)** | Uzun vade (puan: 13) |
| 2026-06-16 | ROADMAP | **Widget ekranı genişletme** — resize, çoklu sayfa | Uzun vade (puan: 13) |
| 2026-06-16 | ROADMAP | **Aider repo-map CBM entegrasyon testi** | ⏸ İşleme alınmadı (puan: 8) — HISTORY'de |
| 2026-06-16 | ROADMAP | **Greptile API PR review otomasyonu** | ⏸ İşleme alınmadı (puan: 7) — HISTORY'de |

---

## 📊 Fikir Puanlama (2026-06-21)

> Agent tarafından yapıldı. Kriterler: Kullanıcı Değeri · Uygulanabilirlik · Bağımlılık Riski · Etki Alanı (her biri 1-5, toplam 20)

| # | Fikir | Değer | Uygulanabilirlik | Bağımlılık | Etki | TOPLAM | Öneri |
|---|-------|:---:|:---:|:---:|:---:|:---:|:---:|
| 1 | QUERY_ALL_PACKAGES Play Store beyanı | 5 | 5 | 4 | 5 | **19** | ✅ Yap |
| 2 | Privacy Policy sayfası | 4 | 5 | 5 | 5 | **19** | ✅ Yap |
| 3 | Content rating anketi | 4 | 5 | 4 | 5 | **18** | ✅ Yap |
| 4 | Onboarding adım sırası güncellenmeli | 3 | 5 | 5 | 4 | **17** | ✅ Yap |
| 5 | Multi-language support (TR/EN) | 4 | 3 | 5 | 5 | **17** | ✅ [TAMAMLANDI D113] |
| 6 | Klasör sıra numarasıyla yer değiştirme | 4 | 4 | 5 | 3 | **16** | ✅ Yap |
| 7 | Dark mode tam uyum audit | 3 | 4 | 5 | 4 | **16** | ✅ [TAMAMLANDI D114] |
| 8 | Akıllı Uygulama Önerileri (30dk) | 5 | 2 | 4 | 5 | **16** | ✅ [TAMAMLANDI D116] |
| 9 | Screenshots | 3 | 4 | 3 | 4 | **14** | 🟡 Değerlendir |
| 10 | AppClassifier → JSON asset | 2 | 3 | 5 | 4 | **14** | ✅ [TAMAMLANDI D115] |
| 11 | Android 14 NotificationListenerService testi | 4 | 3 | 2 | 4 | **13** | 🟡 Değerlendir |
| 12 | Hilt DI kurulumu | 2 | 2 | 5 | 4 | **13** | ✅ [TAMAMLANDI D117] |
| 13 | Unit test coverage | 2 | 2 | 5 | 4 | **13** | ✅ [TAMAMLANDI D118] |
| 14 | cycle.ps1 uçtan uca test | 2 | 4 | 4 | 3 | **13** | 🟡 Değerlendir |
| 15 | Tablet layout (large screen) | 3 | 2 | 5 | 3 | **13** | 🟡 Değerlendir |
| 16 | Widget ekranı genişletme | 3 | 2 | 5 | 3 | **13** | 🟡 Değerlendir |
| 17 | AppNotificationListenerService restart | 4 | 3 | 2 | 3 | **12** | 🟡 Değerlendir |
| 18 | Firebase Crashlytics kurulumu | 3 | 3 | 2 | 4 | **12** | 🟡 Değerlendir |
| 19 | BLUR-4 gerçek cihaz testi | 3 | 4 | 2 | 3 | **12** | 🟡 Değerlendir |
| 20 | Kendi sunucu API'si | 5 | 1 | 1 | 5 | **12** | 🟡 Değerlendir |
| 21 | AllApps double-tap gerçek cihaz | 3 | 4 | 2 | 2 | **11** | 🟡 Değerlendir |
| 22 | Üretici kategorileri gerçek cihaz | 3 | 4 | 2 | 3 | **12** | 🟡 Değerlendir |
| 23 | Wear OS companion app | 2 | 1 | 3 | 2 | **8** | ⏸ Beklet |
| 24 | Aider CBM entegrasyon testi | 1 | 3 | 3 | 1 | **8** | ⏸ Beklet |
| 25 | Greptile API PR review | 1 | 3 | 2 | 1 | **7** | ⏸ Beklet |

**TOP 5:** Play Store beyanı (19) · Privacy Policy (19) · Content rating (18) · Onboarding fix (17) · Multi-language (17)
> Play Store engellerini (1+2+3) tek oturumda kapatmak en verimli yol. Onboarding tutarsızlığı ve dark mode audit bağımsız, her döngüye sıkıştırılabilir.

---

## 🔴 Kritik — 2026-06-22 Hüseyin Talepleri

> Haiku (openclaw gh/claude-haiku-4.5) analizi ile puanlandı. Öneri sırası: 9 → 7 → 1 → 8 → 6 → 4

| # | Tarih | Puan | Madde | Durum |
|---|-------|:----:|-------|-------|
| H1 | 2026-06-22 | **18** | **[BUG] Kategorisiz uygulama atama → mail compose açılıyor** — "Otomatik ata" butonuna basınca e-posta client açılıyor, intent yanlış | [TAMAMLANDI D124 — LazyList reflow fix, classifyLoading guard, Toast] |
| H2 | 2026-06-22 | **18** | **Telefonlar arası online senkronizasyon** — 2 telefon aynı masaüstü/klasör yapısı; uygulama birinde yoksa uyarı ver ("Facebook yükle"); senkron gerçek zamanlı | Bekliyor |
| H3 | 2026-06-22 | **17** | **Ana ekranda app search** — Google arama çubuğunun altına, klasörleri filtreler; X ile temizle; 30sn işlem yapılmazsa otomatik reset; sağ köşede arama ikonu, yazınca 30sn geri sayım göster | [TAMAMLANDI D125 — FolderSearchBar, 30s auto-reset, Ayarlar toggle] |
| H4 | 2026-06-22 | **17** | **Google Drive yedekleme + zamanlama** — Yedekleme saati/günü kullanıcı seçsin; Google Drive'a push seçeneği; mevcut BackupWorker genişletilecek | Bekliyor |
| H5 | 2026-06-22 | **16** | **Sayfa kayması ve ekran boyutu uyumu** — Tüm özellikler (favoriler, son kullanılanlar, öneriler) açıkken içerik alt kısma kayıyor; farklı telefon ekran boyutları için dinamik sayfa düzeni (4/6/8 klasör adaptif) | [TAMAMLANDI D127 — adaptif pageSize (screenHeightDp+activeFeatureCount), compactMode <640dp] |
| H6 | 2026-06-22 | **15** | **Renk teması tüm elemanlara yansısın** — Favoriler, son kullanılanlar, badge, klasör border, arama çubuğu — hepsi primary rengi takip etmeli; mevcut bazı hardcode #00897B kaldı | [TAMAMLANDI D128 — AppContextMenu/DockEdit/CategoryPicker/HomeLongPress/AppIconView/FolderPager'da TealColor→MaterialTheme.colorScheme.primary] |
| H7 | 2026-06-22 | **14** | **Bildirim izni kapalıysa uyarı + veri güvencesi** — Bildirimler kapalıysa HomeScreen'de uyarı banner; "Tüm veriler cihazda kalır, dışarı çıkmaz" açıklaması onboarding/ayarlarda | [TAMAMLANDI D126 — PermissionsBanner güvence notu, Onboarding privacyNoteRes, Ayarlar Bildirim bölümü] |
| H8 | 2026-06-22 | **13** | **Üreticiye göre klasörleme fuzzy matching** — Büyük/küçük harf, nokta, tire toleransı (Samsung/SAMSUNG/samsung); tek üretici 1 cihazsa "Diğer" klasörüne at; çok cihazsa üretici klasörü aç | ✅ TAMAMLANDI (D130) |
| H9 | 2026-06-22 | **12** | **Ayarlar istatistik ekranı** — Toplam klasör sayısı, uygulama sayısı, en çok kullanılan kategori, sınıflandırılmamış uygulama sayısı, son yedekleme tarihi | ✅ TAMAMLANDI (D129) |
| H10 | 2026-06-22 | **8** | **Kod dosyası bölme (single responsibility)** — Büyük composable'ları ayrı dosyalara böl; max 300 satır kuralı tüm dosyalara uygula | [TAMAMLANDI D134-D139 — AllAppsDrawer 982→695, FolderSheet 749→275, HomeScreen 876→748, SettingsScreen 765→352; 5 yeni dosya] |

---

## 📋 Son Eklenenler (Bu Konuşmadan)

| Tarih | Madde |
|-------|-------|
| 2026-06-22 | **openclaw API testi** — gh/claude-haiku-4.5 çalışıyor; bash üzerinden /v1/chat/completions |
| 2026-06-23 | **HomeAppSearchBar** — Google Search altında uygulama arama çubuğu eklendi (all99 round-robin proxy). [TAMAMLANDI D140] |
| 2026-06-23 | **Lokal AI Gateway (all99)** — http://localhost:20128/v1, 80 model, scripts/local_ai.py helper, .env kayıt tamamlandı |
| 2026-06-22 | Hüseyin 10 feature talebi FİKİRLER.md'ye eklendi (H1-H10, haiku puanlaması) |
| 2026-06-21 | **AllAppsDrawer klavye fix** [TAMAMLANDI D119] |
| 2026-06-21 | **Klasör sıralama görsel UI** [TAMAMLANDI D119] |
| 2026-06-21 | **Widget hızlı menü çalışmıyor** — araştırılacak |
| 2026-06-21 | **İki yeni tema: iOS + AMOLED** [TAMAMLANDI D122] |
| 2026-06-21 | **Onboarding yeniden yazım** [TAMAMLANDI D120] |
| 2026-06-21 | **Görsel kalite artırımı** [TAMAMLANDI D123] |
| 2026-06-21 | **Test altyapısı araştırması** — Maestro/UIAutomator/Firebase Test Lab |
| 2026-06-20 | FİKİRLER.md sistemi oluşturuldu |

---

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda | Son güncelleme: 2026-06-22*
