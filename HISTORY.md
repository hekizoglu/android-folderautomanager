# HISTORY.md — AppOrganizer Döngü Arşivi

> CLAUDE.md'den taşınan döngü-spesifik değişiklik logları. **Her konuşmada okunmaz** — sadece "geçmişte X'i nasıl yapmıştık?" sorusunda referans.
> Append-only. Yeni döngü özetleri sona eklenir.
>
> Kalıcı kurallar → `CLAUDE.md` · Promote öğrenmeler → `LEARNINGS.md`

---

## Özellik Denetim Geçmişi

### Özellik Kontrol Listesi — Kontrol Komutları
| # | Özellik | Kontrol Komutu |
|---|---------|----------------|
| 1 | Turkuaz tema | `grep -n "00897B\|26C6DA" app/src/.../Theme.kt` |
| 2 | Launcher manifest | `grep -n "HOME\|DEFAULT" .../AndroidManifest.xml` |
| 3 | RoleManager | `grep -rn "RoleManager\|ROLE_HOME" app/src` |
| 4 | AllAppsDrawer blur | `grep -n "blur\|Transparent" .../AllAppsDrawer.kt` |
| 5 | İkon async | `grep -rn "produceState\|iconCacheInternal" app/src` |
| 6 | Long-press haptic | `grep -rn "HapticFeedback" app/src` |
| 7 | Tap haptic | `grep -n "launchApp\|startActivity" + haptic` |
| 8 | DockEditSheet | `grep -n "DockEditSheet\|DockEdit" app/src` |
| 9 | Varsayılan launcher butonu | `grep -n "Varsayılan Launcher\|ROLE_HOME" .../SettingsScreen.kt` |
| 10 | Bildirim badge | `grep -rn "notificationCount\|badgeText" app/src` |
| 11 | NotificationListenerService | `find app/src -name "*Notification*Service*"` |
| 12 | AppListScreen satır | `wc -l .../AppListScreen.kt` |

### Son Tam Denetim (2026-06-13)
Tüm 12 madde ✅. Detay:
- #7: Tap haptic — LongPress tipi ile eklendi (TextHapticFeedback API yok)
- #11: AppNotificationListenerService eklendi, DB'ye yazıyor
- #12: AppListScreen 244 satır — CategoryChip+AppListContent → AppListComponents'a taşındı

---

## Erken Bug Fix'ler (2026-06-10 → 06-12)

**Düzeltilen Buglar (2026-06-10):**
- FolderSheet geri/home tuşu: `sheetState.hide()` + `BackHandler` entegre
- Sil/Gizle: `contextMenuApp` artık `allApps` flow'undan güncel veri alıyor (stale state giderildi)
- Dock'a ekle: dolu/zaten var durumları Toast ile bildiriliyor
- Swipe-up AllApps: `detectVerticalDragGestures` eklendi
- Çift tap: ana ekrana çift dokunarak AllApps açılıyor

**Eklenen Özellikler (2026-06-12):**
- AppClassifier: +30 Türk uygulaması (Getir, Çiçeksepeti, D-Smart, Puhutv, Tabii, TRT, Marti, TCDD)
- KeywordDatabase: Türkçe keyword'ler
- AppIconView: tap'te spring bounce scale (ripple kaldırıldı, Pixel hissi)
- HomeLongPressSheet: Duvar Kağıdı / Dock Düzenle / Ayarlar menüsü
- AllAppsDrawer: açılınca 300ms sonra klavye otomatik (FocusRequester) + kapanışta arama geçmişe

---

## AppPrefs Anahtar Geçmişi

**2026-06-12 eklenenler:**
`KEY_AUTO_BACKUP_ENABLED`, `KEY_HIDE_NAV_BUTTONS`, `KEY_ALLAPPS_BG_ALPHA` (float), `KEY_NOTIFICATION_TEXT_ENABLED`, `KEY_LAST_RECONCILE` (5dk throttle)

**Sonraki döngülerde eklenenler:**
`KEY_FOLDER_SORT_MODE`, `KEY_ICON_PACK`, `KEY_WIDGET_AREA_ENABLED`, `KEY_FOLDER_SIZE`, `KEY_MANUFACTURER_CLASSIFY`, `KEY_LABEL_COLOR`, `KEY_BG_TYPE/COLOR/TEXT_ALPHA`, `KEY_SUGGESTIONS_ENABLED`, `KEY_FOLDER_CUSTOM_NAMES/EMOJIS/COLORS`, `KEY_DEEPSEEK_API_KEY`, `KEY_NEW_BADGE_ENABLED`, `KEY_SWIPE_HINT_ENABLED`, `KEY_FOLDER_COUNT_VISIBLE`, `KEY_FOLDER_SWIPE_HINT_ENABLED`

---

## Mimari Notlar (Döngü kaynaklı)

**Tema Sistemi:** `AppOrganizerTheme` artık `ThemePreferences`'i dinler — `context.themeDataStore` Flow'undan `AppTheme`+`AppFont` okuyup `buildColorScheme`/`buildTypography` üretir. Tema seçimi anında uygulanır.

**HorizontalPager:** HomeScreen'de 8 klasör/sayfa — `pageSize = 8`, `HorizontalPager` + sayfa noktacıkları. LazyVerticalGrid `userScrollEnabled = false`.

**Bildirim Metni Mimarisi (Döngü 8):** `AppNotificationListenerService.latestTexts: StateFlow<Map<String,String>>` — her `onNotificationPosted`'da EXTRA_TITLE+EXTRA_TEXT birleşir. `AppInfo.notificationText` field (DB v6). Gösterim: `isNotificationTextEnabled()`=true → FolderTile/AllAppsDrawer.

**LauncherActivity Onboarding:** `onCreate` başında onboarding bitmemişse MainActivity'ye yönlendirir (pm clear/fresh install sonrası onboarding atlanması giderildi).

**Onboarding Adım Listesi (14 adım):** WELCOME → RESTORE_BACKUP → QUERY_PACKAGES → NOTIFICATIONS → UNUSED_GREY → AUTO_BACKUP → NOTIF_TEXT → NOTIF_ACCESS → SWIPE_HINT → NEW_BADGE → FOLDER_COUNT → NAV_HIDE → THEME_SELECT → SET_LAUNCHER → DONE. (SET_LAUNCHER en sona alındı — tüm ayarlar bitince varsayılan launcher sorulur.)

---

## Döngü Logları (8-31)

**Döngü 8 — Bildirim metni mimarisi:** (yukarıda)

**Döngü 9 — Onboarding bug fix:** `OnboardingScreen.kt:544` yanlış key/prefs (`"app_prefs"/"onboarding_complete"`) → her açılışta onboarding tekrarlıyordu. FIX: `AppPrefs.PREFS_NAME`+`KEY_ONBOARDING_DONE`. Yeni adımlar: AUTO_BACKUP + NOTIF_TEXT (isSkippable=true).

**Döngü 10 — MainActivity refactor:** `PREFS_NAME`/`KEY_ONBOARDING_DONE` const kaldırıldı → AppPrefs'e taşındı (DRY).

**Döngü 11 — onResume perf + gesture nav:** `PACKAGE_FILTER` companion sabiti (her onResume'da IntentFilter oluşmaz), `receiverRegistered` bayrağı (çift kayıt önlendi), `isGestureNavEnabled()` (`config_navBarInteractionMode==2`), gesture nav aktifse `BEHAVIOR_DEFAULT` (Xiaomi/Samsung home gesture çakışması). HomeScreen: çift `loadDockPackages`+`syncAppSizes` kaldırıldı, alt 80dp gesture zone swipe AllApps tetiklemez.

**Döngü 12 — DockIcon async:** `remember(packageName)` senkron `toBitmap()` → `produceState<ImageBitmap?>` IO async + LRU-200 paylaşımı. `bitmap?.let { }` smart cast pattern. Encoding fix: PixelClockWidget bozuk UTF-8 (`C3 A2 E2 82 AC 22`) → em-dash.

**Döngü 15 — onResume yük + dock kırık ikon:** `dockLoaded` bayrağı (klasör sırası sadece ilk okuma), `loadDockPackages` değer karşılaştırması (gereksiz rekomposisyon önlendi), `onPackageRemoved` dock'taysa anında güncelleme, `isGestureNavEnabled()` → `by lazy` property.

**Döngü 16 — queryIntentActivities:** `getInstalledPackages(GET_META_DATA)`+per-package `getLaunchIntentForPackage` → tek `queryIntentActivities(MAIN+LAUNCHER)` (~5x hız). `reconcileIfNeeded` aynı optimizasyon. AllApps anim: fadeIn/Out → LinearOutSlowInEasing(300ms)/FastOutLinearInEasing(220ms). Dock `systemGestureExclusionRects` karşılaştırma.

**Döngü 17 — AllAppsDrawer rekomposisyon:** `rememberAppIcon` initialValue=cache (cache hit anında), `quickFilterCounts` `remember(apps)` memoize (4x→1x), `notifTextEnabled` drawer seviyesine çıktı (100+ SharedPrefs okuma önlendi). LauncherActivity: `markUsageStatsSynced` (çift senkron önlendi).

**Döngü 18 — bildirim badge temizleme:** Tüm bildirimler silinince badge/metin DB'de kalıyordu. `onNotificationRemoved` → map entry kaldırma (`activeNotifications?.any{...&&!isOngoing}`). `badgeCounts`/`latestTexts` observer `isNotEmpty()` guard kaldırıldı + DB temizleme. Boş listede yazma yok.

**Döngü 19 — ikon cache temizleme + DRY:** `onPackageRemoved`/`onPackageAdded` paket-spesifik cache temizleme, `onPackageAdded` tam tarama → `helper.getAppInfo(pkg)` (~5x). `dockLoaded` @Volatile. FolderSheet: `FolderSortMode` enum kaldırıldı → `AllAppsSortMode` (DRY).

**Döngü 20 — Flow Eagerly:** `folders`/`allApps`/`filteredAllApps` `WhileSubscribed(5000)` → `SharingStarted.Eagerly` (dönüşte "Yükleniyor" flaşı giderildi). `isLoadingApps` @Volatile guard. `loadDockPackages` sadece ilk yükleme. `reconcileIfNeeded` tek geçişli set dedup.

**Döngü 21 — dock in-memory + refactor:** `addToDock`/`removeFromDock` SharedPrefs okuma yerine `_dockPackages.value` (sıfır disk IO). Hardcoded `4` → `DOCK_MAX_SIZE`. `isDefaultLauncher` ölü kod kaldırıldı. HomeScreen→HomeScreenComponents refactor (866→634 satır, yeni 288 satır): PixelClockWidget/GoogleSearchBar/PixelDock/DockIcon/SwipeHint taşındı, `internal` görünürlük.

**Döngü 22 — ikon paketi desteği (Yol Haritası #6):** `IconPackManager.kt` — 5 intent filter (Nova/ADW/GO/Lawnchair/Tesla), `parseAppFilter()` appfilter.xml parse, `filterCache: ConcurrentHashMap`. `KEY_ICON_PACK` + otomatik cache temizleme. Cache key formatı güncellendi. SettingsScreen "İkon Paketi" bölümü.

**Döngü 23 — HomeLongPressSheet grid:** `items(emptySlots)` FolderTile'lardan önceydi → koordinat kayması. FIX: boş slotlar `items(pageFolders.size)`'den sonra. Üst boş alan (y≈180) uzun bas doğrulandı.

**Döngü 24 — widget desteği (Yol Haritası #7):** `WidgetPrefs.kt` (ID listesi SharedPrefs), `WidgetHostManager.kt` (AppWidgetHost singleton, start/stopListening), `WidgetArea.kt` (AppWidgetHostView AndroidView, uzun bas → X silme). LauncherActivity: widgetPicker/Configure launcher, onResume/Pause lifecycle. ViewModel: `widgetIds: StateFlow`. `KEY_WIDGET_AREA_ENABLED`. Manifest: `BIND_APPWIDGET`.

**Döngü 24 (yerel paralel) — AllApps greyscale + RESTORE_BACKUP:** `unusedGreyDays` drawer seviyesinde okunuyor, kapalıysa renkli. RESTORE_BACKUP onboarding adımı (JSON seçici). `_openFolder`→`_openFolderId` refactor (combine ile türetilir). KEY_BG_TYPE/COLOR/TEXT_ALPHA + DisposableEffect.

**Döngü 25 — greyscale + sort + onboarding:** `NiagaraAppRow` `unusedGreyDays` param (`<=0` → saturation=1f). `KEY_FOLDER_SORT_MODE` kalıcılık. RESTORE_BACKUP adımı (`hiltViewModel` ile AppListViewModel inject). `Locale("tr")` arama+sıralama (Ş/İ/Ğ/Ü/Ö/Ç).

**Döngü 26 — Settings reaktiflik:** `remember{}` AppPrefs değerleri Settings dönüşünde güncellenmiyordu. FIX: `mutableStateOf` + `DisposableEffect` + `OnSharedPreferenceChangeListener`. HomeScreen (bgType/bgColor/textAlpha/widgetAreaEnabled), AllAppsDrawer (notifTextEnabled/unusedGreyDays). FolderTile `textAlpha` param. `KEY_FOLDER_SIZE` slider (56-96dp).

**Döngü 27 — robustlik:** `isLoadingApps` @Volatile → `AtomicBoolean` (`compareAndSet`). `onListenerDisconnected()` → badge/text temizleme (stale önlendi). `iconPackPkg` reaktif (DisposableEffect+KEY_ICON_PACK listener). `MANUFACTURER_PREFIX_MAP` (Samsung/Huawei/Xiaomi/Sony/LG → kategori), `KEY_MANUFACTURER_CLASSIFY` toggle.

**Döngü 28 — app shortcuts:** `ShortcutHelper.kt` — `LauncherApps.getShortcuts()` (DYNAMIC+MANIFEST, runCatching), `getShortcutIconDrawable()`, `startShortcut()`. AppContextMenu: `shortcuts by produceState` (max 4), yatay Row, `ShortcutItem` (48dp ikon+2 satır). `KEY_LABEL_COLOR`, FolderTile labelColor.

**Döngü 29 — app önerileri + onboarding:** `KEY_SUGGESTIONS_ENABLED`. `suggestedApps: StateFlow` (lastUsedTimestamp öncelikli, take(4), Eagerly). `AppSuggestionsRow`+`SuggestionAppItem`. SettingsScreen "Uygulama Önerileri" toggle. Onboarding steps açık sıralama, SET_LAUNCHER en sona.

**Döngü 30 — klasör özelleştirme:** `KEY_FOLDER_CUSTOM_NAMES/EMOJIS` (JSON map). FolderTile `customName`/`customEmoji` param. FolderSheet header Edit ikonu → `FolderRenameDialog` (OutlinedTextField + 40 emoji LazyRow). AppClassifier build fix: CAT_TOOLS→CAT_UTILITIES, CAT_PHOTO→CAT_PHOTOGRAPHY.

**Döngü 31 — klasör renk:** `KEY_FOLDER_CUSTOM_COLORS` (JSON map #RRGGBB). FolderSheet `customColor` + 10 preset swatch. FolderTile `customColor` öncelikli. HomeScreen DisposableEffect listener.

---

## Döngü Logları (34-84) — AppClassifier Büyütme Dönemi

> Bu dönem ağırlıklı olarak `AppClassifier.exactMatchMap` büyütmesi: **479 → 3116+ benzersiz paket**. Tekrar eden tuzak: duplicate yönetimi (LEARNINGS.md'ye promote edildi).

**Döngü 32 (remote) — LLM fallback (Aşama 2):** `CategoryLLMFallback.kt` — `categorize(apps, apiKey, onProgress)` batch 15/istek, HttpURLConnection DeepSeek, `deepseek-chat` temp 0.1, 14 VALID_CATEGORIES. `KEY_DEEPSEEK_API_KEY`. ViewModel `categorizeDigerWithLLM`. SettingsScreen key input + buton + ilerleme.

**Döngü 34 — duplicate temizlik:** 107+25 duplicate temizlendi → 1448 benzersiz, 0 duplicate.

**Döngü 35 — Diğer Klasörü UI + favoriler:** `otherApps: StateFlow` (CAT_OTHER). SettingsScreen "Diğer Klasörü (N)" + ilk 20 liste. `favoriteApps: StateFlow` (`_favoritePkgs` combine), `initFavorites`/`toggleFavorite`, PackageChangeReceiver otomatik temizleme. **Dikkat:** `getFavoriteApps()` KALDIRILDI → `viewModel.favoriteApps` kullan.

**Döngü 36 — RecentApps + icon pack reaktif + favoriler uzun bas:** `recentApps: StateFlow` (lastUsedTimestamp, 8 uygulama — `suggestedApps` 4'tü, `take(8)` işlevsizdi). `suggestionIconPack` reaktif. `FavoritesRow` `onAppLongClick` → `combinedClickable`. BUILD #9: SettingsScreen lambda Unit hatası fix.

**Döngü 42 — BUILD #10:** Temiz build. AAB 6.3MB Play Store hazır (`Desktop/AppOrganizer_PlayStore/app-release-v1.0.0.aab`), mapping kaydedildi.

**Döngü 46 (remote) — büyük temizlik + App Not:** 186 duplicate temizlendi (1407→1182), +93 yeni (PHOTOGRAPHY/NEWS/FOOD/EDUCATION + sistem uygulamaları). KeywordDatabase güçlendirildi. AppContextMenu: `usageCount` ms → "2.5 sa"/"45 dk" format fix. **App Not özelliği:** `AppNoteDialog`, `AppDao.updateCustomNotes()`, `saveAppNote()`. `AppInfo.customNotes` (DB v6'dan beri vardı, aktif edildi). `formatUsageTime(ms)` private fun.

**Döngü 47 (remote) — SPORTS + COMMUNICATION:** SPORTS 13→49, COMMUNICATION 16→35. NA ligleri, soccer global, F1/Tenis/Golf, yayın platformları, TR spor. E-posta/video/VoIP/takım araçları. Toplam 1784.

**Döngü 48-49:** CAT_SPORTS+CAT_COMMUNICATION unresolved ref fix (Category modelde sabitler yoktu) — BUILD #11 fix.

**Döngü 50-53:** +105 TR Lifestyle/Global Streaming/Finance/Health/Productivity.

**Döngü 54-55:** +95 Google/Amazon/Meta/Microsoft tam paket isimleri, top oyunlar.

**Döngü 56:** +79 Müzik/Podcast/Maps/TR-Market/VPN/Auth.

**Döngü 57:** +79 SmartHome/Kids/DevTools/TR-Bankalar/Hava.

**Döngü 58 (remote+local) — büyük genişleme:** +183 net → 2074. Automotive, Sigorta (+TR), Gayrimenkul, Evcil Hayvan, Ev Hizmetleri, OEM Apps (OPPO/Realme/Vivo/Nothing/Asus/Motorola/OnePlus), Seyahat, Streaming, TR Transit & Yerel, Grocery, Foto/Video, E-Commerce, Kitap. 15 duplicate temizlendi, merge conflict çözüldü.

**Döngü 59:** +81 Gaming/Spor/TR-Kültür/Emlak/Telekom.

**Döngü 54-60 — BUILD #13:** Debug APK 28.1MB Telegram'a. AppClassifier 2074+.

**Döngü 61:** Otomotiv/AI(ChatGPT/Claude/Gemini/DeepSeek)/Sigorta +82.

**Döngü 62 (remote+local):** +128 net → 2185. TR Haber, Avrupa Haberleri, Asya/Pasifik Haber, Restoran Zincirleri, Yemek Teslimat, Foto/Kamera, Sosyal Medya. Kategori dağılımı: NEWS 95→147, FOOD 102→131, PHOTOGRAPHY 104→134, SOCIAL 136→153.

**Döngü 63:** LatAm/Orta Doğu/TR Gov(e-Devlet/SGK)/Cloud +74.

**Döngü 64:** Çevre/Erişilebilirlik/Browser/Ev Güvenlik +92.

**Döngü 65:** TR Kulüpler(GS/FB/BJK)/Müzik/TR Radyo/Sözlük +69.

**Döngü 61-66 — BUILD #14:** Debug APK 28.2MB. AppClassifier 2351.

**Döngü 67:** AppClassifier +70, FolderTile bildirim "AppAdı: mesaj" format + `onNotificationTap`, BackupWorker/WorkManager (haftalık), `work-runtime-ktx:2.9.0`.

**Döngü 68:** HomeScreen bildirim tap handler (bildirime tap → uygulama aç), +80 (mobile gaming/fintech/mental health/cloud/TR gov).

**Döngü 69 (remote):** 2562→2623 (+61 net). Encoding fix (com.tonguc.app, com.kigili.android). +93 (Productivity/Dev/TR Eğitim/Games/Crypto/TR Bankalar/Health/Smart Home/Navigation/Business). Merge conflict çözüldü.

**Döngü 67-72 — BUILD #15:** Debug APK 28.5MB. Loop 67-71 detayları (WorkManager, oyunlar, fintech, e-öğrenme, harita). AppClassifier 2624.

**Döngü 73 (remote+local):** 2486→2753 (+267 net). SOCIAL/PHOTOGRAPHY/NEWS/HEALTH/UTILITIES/TRAVEL(Hotels+Automotive)/FOOD Chains/FINANCE BNPL/SHOPPING TR/EDUCATION/GAMES + remote FOOD/TELECOM/GOV TR. 0 duplicate.

**Döngü 74-78 — BUILD #16:** +62 + Loop 77 Anime/Manga/SmartHome/Meditasyon/TR Medya +70. 20 duplicate temizlendi → 2861. Debug APK 28.5MB (msg ID 629).

**Döngü 79 (remote+local) — KeywordDatabase BUG FIX:** `mapOf()` duplicate kategori (CAT_TRAVEL/SHOPPING/FINANCE/HEALTH/UTILITIES iki kez) → ilk listeler Loop 40'tan beri kayboluyordu. Tüm kategoriler tek listede. +77 yeni (Hindistan E-Ticaret/Fintech/Seyahat/Sosyal, Afrika Fintech/Telekom, Pakistan/Bangladeş, GD Asya, MENA, LATAM). 2938 benzersiz.

**Döngü 79-84 — BUILD #17:** Loop 79-83 detayları (Fintech/Cloud/Streaming/Gaming/VPN/Dating/DevTools/Kids/Music/Fitness/Crypto/Food/LangLearn/TR). Debug APK 28.5MB (msg ID 657). AppClassifier 3047 (3000 aşıldı).

**Döngü 84 (remote) — son durum:** 3047→3116 (+69 net). Oyunlar (Gacha/ARPG 2024-25: Wuthering Waves/NIKKE/Reverse 1999), Güç Kullanıcı Araçları (Aurora/Shizuku/Tasker/Termux), Yeni Nesil Sosyal (Lemon8/Bluesky), PKM (Anytype/AppFlowy/Workflowy), Mental Wellness (Youper/Daylio), Web3 Cüzdanlar (Rainbow/Phantom), TR Dijital (Onedio/Webtekno/Kariyer.net). KeywordDatabase +37 satır (tüm kategorilere). **FolderSheet Türkçe arama fix:** `contains(ignoreCase=true)` → `lowercase(Locale("tr")).contains(q)`. 2 duplicate temizlendi.

---

## Build Geçmişi Özeti

| Build | Döngü | Boyut | Not |
|-------|-------|-------|-----|
| #6 | 41 | 28MB | İlk LLM fallback dahil |
| #9 | 36 | — | SettingsScreen lambda fix |
| #10 | 42 | — | AAB 6.3MB Play Store hazır |
| #11 | 48 | — | CAT_SPORTS/COMMUNICATION ref fix |
| #13 | 60 | 28.1MB | — |
| #14 | 66 | 28.2MB | — |
| #15 | 72 | 28.5MB | — |
| #16 | 78 | 28.5MB | msg ID 629 |
| #17 | 84 | 28.5MB | msg ID 657 |

> Remote ortamda APK build edilemez (dl.google.com erişim yok) — tüm build'ler yerel makinede.

---

*Bu arşiv 2026-06-15'te CLAUDE.md v3 geçişinde oluşturuldu. 76 döngülük log buraya taşındı.*
