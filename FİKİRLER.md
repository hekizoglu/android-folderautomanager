# FİKİRLER.md — AppOrganizer Fikir & Görev Havuzu

> Yeni özellik fikirleri, döngüden gelen görevler, backlog adayları buraya eklenir.
> ROADMAP.md artık değiştirilmez — yeni her şey buraya gelir.
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

## ⭐ Yüksek Puanlı (≥15p) — Rekabet Analizi 2026-06-29

> Kaynak: ChatGPT destekli açık kaynak launcher karşılaştırması (Lawnchair, KISS, Neo, mLauncher, Fossify, Kvaesitso, Pie, Olauncher)

| Tarih | Puan | Madde | Durum |
|-------|------|-------|-------|
| 2026-06-29 | **17 ⭐** | **Folder Swipe-Up → En Sık Kullanılan Uygulama** — Klasör kartına yukarı kaydırınca o klasördeki en çok açılan uygulama doğrudan başlar (Sosyal→WhatsApp, Finans→Garanti, Spor→Hevy). AppPrefs ile her klasör için override edilebilir. (KV:5 · U:4 · BR:4 · EA:4) | [TAMAMLANDI — FolderTile.kt:131, HomeScreen.kt:543] |
| 2026-06-29 | **17 ⭐** | **UsageScore Algoritması v2** — Mevcut usageCount+lastUsed yetmez. `score = baseCategory + usageCountWeight + lastUsedBoost + timeOfDayBoost + dayOfWeekBoost + dockFavoriteBoost + notificationBoost - focusModePenalty`. Sabah: Takvim/Gmail/Maps; öğlen: banka/yemek; akşam: spor/müzik; gece: alarm/okuma. (KV:5 · U:3 · BR:4 · EA:5) | [TAMAMLANDI — LauncherViewModel.kt:483, dock+0.15 notif+0.2 boost] |
| 2026-06-29 | **17 ⭐** | **Privacy Center UI** — Fossify tarzı net gizlilik ekranı: "Uygulama listesi cihazda kalır · İnternete veri gönderilmez · Online DB opsiyonel · Tüm veriyi sıfırla." Settings > Hakkında altına kart + README'ye bölüm. Play Store için zorunlu. (KV:4 · U:5 · BR:5 · EA:3) | [TAMAMLANDI — SettingsBackupAboutSection.kt:62, AppListViewModel.kt] |
| 2026-06-29 | **16 ⭐** | **Smart Search v1 (KISS tarzı)** — AllAppsDrawer aramasını genişlet: uygulama adı + paket + kategori adı aynı anda aranır. "banka" → Finans klasörü + tüm finans uygulamaları; "spor" → Hevy+YouTube Music+Sağlık; "wa ali" → WhatsApp Ali kişisi (ileride). İlk sürümde: app+kategori+shortcut. (KV:5 · U:3 · BR:3 · EA:5) | [TAMAMLANDI — AllAppsDrawer.kt:587, catMatch bucket eklendi] |
| 2026-06-29 | **16 ⭐** | **AppOrganizer Assistant Kartları** — Ana ekranda klasörlerin üstünde küçük kart: "Sabah genelde Haritalar açıyorsun", "Finans'ta 3 okunmamış bildirim", "Son 7 gündür açılmayan 12 uygulama var", "WhatsApp bu klasörde en çok açılıyor — swipe-up atanabilir." İlk sürümde AI gerekmez, saf kural motoru. (KV:5 · U:3 · BR:4 · EA:4) | [TAMAMLANDI — InsightEngine.kt, AssistantInsightRow.kt, LauncherViewModel+HomeScreen] |
| 2026-06-29 | **15 ⭐** | **Contextual Dock v1** — Dock sabit 4 uygulama değil: 2 kullanıcı sabit + 2 akıllı öneri. Saat+gün+son kullanım+bildirim yoğunluğuna göre değişir. Gündüz: Mail+Takvim; akşam: müzik+sosyal. DockContext veri sınıfı + AppPrefs toggle. (KV:5 · U:3 · BR:3 · EA:4) | [TAMAMLANDI — LauncherViewModel.kt:contextualDockPackages, HomeScreen+Settings toggle] |
| 2026-06-29 | **15 ⭐** | **Manual Category Override** — Long press uygulama → "Kategori Değiştir" → seçim + "Bu kararı hatırla" checkbox. ManualCategoryOverride tablosu (Room) — AppClassifier bu tabloyu exactMatch'ten önce kontrol eder. "Aynı tür uygulamalara uygula" opsiyonu. (KV:4 · U:4 · BR:4 · EA:3) | [TAMAMLANDI — AppPrefs.KEY_MANUAL_CAT_OVERRIDES, AppClassifier.classifyApp(), LauncherViewModel.updateAppCategory()] |
| 2026-06-29 | **14 🟡** | **Batch Kategori Değiştirme** — AppListScreen çoklu seçim modu: birden fazla uygulama checkbox ile seçilir, tek seferde kategori atanır. "Tümünü Seç" + "Seçimi Temizle" butonları. Kategori taşıma sonrası seçim korunur — zincirleme düzenleme kolaylığı. (KV:4 · U:4 · BR:2 · EA:4) | [MEVCUT — AppListScreen.kt:120-139, AppListViewModel.kt:287-353, AppListDialogs.kt:BulkCategoryPicker] |
| 2026-06-29 | **14 🟡** | **Widget Öneri Motoru** — Kullanıcının en çok kullandığı uygulamaların widget'larını öner. AppWidgetManager.getInstalledProviders() ile cihazdaki widget'ları tara, kullanım verisiyle eşleştir, "WhatsApp için 3 widget var" toast + Settings'te öneri listesi. (KV:4 · U:4 · BR:3 · EA:3) | [TAMAMLANDI — WidgetSuggestionEngine.kt, WidgetSuggestionSection.kt, AppListViewModel+LauncherViewModel] |
| 2026-06-29 | **13 🟡** | **Weekly Digest (Kullanılmayan Uygulama Raporu)** — Her hafta "7+ gündür açılmayan X uygulama var" notification. Settings'te detaylı rapor: hangi uygulamalar, kaç gündür kapalı, toplu gizleme/kaldırma önerisi. UsageStatsManager + WorkManager PeriodicWork. (KV:4 · U:4 · BR:2 · EA:3) | [TAMAMLANDI — WeeklyDigestWorker.kt PeriodicWork 7gün, lastUsedTimestamp tabanlı, AppPrefs.KEY_WEEKLY_DIGEST, SettingsBackupAboutSection switch] |

---

## 🟡 Orta Öncelik (12-14p)

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-29 | Rekabet | **GestureActionEngine** — Swipe up/down/left/right + çift tık + long press home + saat tıklama + klasör swipe-up için özelleştirilebilir aksiyon tablosu. Aksiyonlar: drawer aç, bildirimler, hızlı ayarlar, ekran kilitle, screenshot, belirli uygulama aç, focus mode. (KV:4 · U:3 · BR:3 · EA:4 = **14p**) | [TAMAMLANDI v1 — AppPrefs.GestureAction, SettingsGestureSection.kt, HomeScreen dispatch] |
| 2026-06-29 | Rekabet | **Backup / Restore JSON** — dock apps + folder order + manual category overrides + tema + gesture ayarları + gizlenen uygulamalar. Google Drive SAF entegrasyonu var, format standardize edilmeli. Neo'da mevcut. (KV:4 · U:3 · BR:4 · EA:3 = **14p**) | [TAMAMLANDI — BackupManager.kt v3: dock+klasör+gesture+manualOverride+theme restore] |
| 2026-06-29 | Rekabet | **ShortcutManager Entegrasyonu** — Long press app → Android dynamic/static shortcuts göster: WhatsApp'ta "Son kişi / Yeni mesaj / Kamera", Haritalar'da "Eve git / İşe git". ShortcutManagerCompat kullan. (KV:4 · U:3 · BR:3 · EA:4 = **14p**) | [MEVCUT — AppContextMenu.kt:85-186, ShortcutHelper.kt — LauncherApps API, ShortcutItem composable, haptic launch] |
| 2026-06-29 | Rekabet | **Notification Badge Intelligence** — Mevcut kırmızı badge yeterli değil. Renk sistemi: Kırmızı=çağrı/alarm, Yeşil=mesajlaşma, Sarı=sistem/güncelleme, Gri=düşük önem. Kanal importance + package prefix bazlı, içerik okunmaz. (KV:3 · U:4 · BR:3 · EA:3 = **13p**) | [TAMAMLANDI — BadgeColorEngine.kt, AppIconView.kt+FolderTile.kt renk entegrasyonu, AppPrefs.KEY_BADGE_INTELLIGENCE toggle, SettingsScreen Bildirim bölümü] |
| 2026-06-29 | Rekabet | **Biometric Settings Lock** — Ayarlar ekranını parmak izi / yüz tanıma arkasına kilitle. Kurumsal/kiosk/çocuk modu için. BiometricPrompt API, minSDK 28. (KV:3 · U:4 · BR:4 · EA:2 = **13p**) | [TAMAMLANDI — BiometricHelper.kt, SettingsScreen açılışta LaunchedEffect doğrulama, "Güvenlik" bölümü toggle, AppPrefs.KEY_BIOMETRIC_SETTINGS_LOCK] |
| 2026-06-29 | Rekabet | **Quick Wheel / Pie Mode (Opsiyonel)** — Boş alana uzun bas → 6-8 uygulamalı radyal çark. Pie Launcher'dan ilham. Kas hafızasıyla bakılmadan açma. AppPrefs toggle, varsayılan kapalı. (KV:3 · U:3 · BR:4 · EA:3 = **13p**) | [TAMAMLANDI — QuickWheelOverlay.kt, HomeScreen.kt onLongPress entegrasyon, Settings "Hızlı Erişim" toggle] |
| 2026-06-29 | Rekabet | **Widget Host (Gerçek)** — Sistem widget'larını (hava durumu, takvim, müzik) destekle. AppWidgetHost + AppWidgetManager. Mevcut saat widgeti bunun üzerine inşa edilebilir. Zor ama Play Store'da ciddi launcher için şart. (KV:4 · U:2 · BR:3 · EA:4 = **13p**) | Bekliyor |
| 2026-06-29 | Rekabet | **Icon Pack Desteği** — Üçüncü taraf icon pack uygulamalarından ikon yükleme. IconPackManager mevcut ama UI eksik. Settings > Görünüm'e seçici ekle. (KV:3 · U:3 · BR:3 · EA:3 = **12p**) | [TAMAMLANDI — SettingsAppearanceSection.kt: DropdownMenu icon pack seçici, IconPackManager.clearCache() + AppPrefs.setIconPack() entegrasyonu, sadece yüklü pack varsa gösterilir] |
| 2026-06-29 | Hüseyin | **claude-code-templates mobile-design skill** — Tablet/foldable desteği planlandığında ekle (şimdi marginal fayda) | Bekliyor |
| 2026-06-29 | Yeni | **Klasör Rengi Otomatik (Dominant İkon)** — Klasör oluşturulunca içindeki uygulamaların ikonundan dominant renk hesaplanıp otomatik atanır. Manuel değiştirilebilir. (KV:3 · U:4 · BR:2 · EA:4 = **13p**) | [TAMAMLANDI — DominantColorExtractor.kt (androidx.palette Vibrant/Dark), LauncherViewModel folders.onEach auto-assign, AppPrefs.KEY_AUTO_FOLDER_COLOR toggle, SettingsAppearanceSection switch] |
| 2026-06-29 | Yeni | **Onboarding Yeniden Başlatma** — Settings > Hakkında'ya "Kurulum Sihirbazını Yeniden Başlat" butonu ekle. (KV:3 · U:5 · BR:1 · EA:3 = **12p**) | [TAMAMLANDI — SettingsBackupAboutSection.kt "Hakkında" bölümüne SettingsButtonRow + AlertDialog: KEY_ONBOARDING_DONE=false → MainActivity clear task restart] |

---

## ⏸ Beklet (≤11p)

| Tarih | Kaynak | Madde | Puan |
|-------|--------|-------|------|
| 2026-06-29 | Yeni | **Duvar Kağıdı Renk Uyumu** — Dominant renge göre klasör başlık rengi | 11p |
| 2026-06-29 | Rekabet | **Online App Category DB** — Opsiyonel, gizlilik riski yüksek. Lokal → keyword → kullanıcı override → en son online. (KV:3 · U:2 · BR:2 · EA:3 = 10p) | 10p |
| 2026-06-29 | Rekabet | **Focus Mode / Minimal Mod** — Olauncher tarzı dijital wellbeing modu, yalnızca izin verilen uygulamalar görünür | 9p |

---

## ✅ Tamamlananlar (Bu Dosyada Kalan)

| Tarih | Madde | Döngü |
|-------|-------|-------|
| 2026-06-29 | Uygulama Kullanım Raporu Ekranı (15p) | D190 |
| 2026-06-29 | Çift Tıkla Arama (14p) | D135 |

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

**Hedef cümle:** "AppOrganizer, uygulamalarını otomatik klasörleyen; kullanım alışkanlığına göre dock, arama ve önerileri akıllandıran privacy-first Android launcher'dır."

---

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda | Son güncelleme: 2026-06-29*
*🏆 Piyasa Puanı: 7.8/10 → Hedef 8.5/10 (Smart Search + Contextual Dock + Privacy Center sonrası)*
