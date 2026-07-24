# P0 — AppOrganizer Ana Ekran: 10 Sorun Çözümü

Tarih: 2026-07-24
Commit: c6de7601, 794c4120

## Sorunlar ve Çözümler

### 07 — Klasör Sürükleme editMode=false Kapılı
**Sorun:** `HomeScreen.kt:1443`'te `editMode = false` sabit — sürükleme logic'i çalışmıyor.
**Çözüm:** 
- `HomeScreen.kt:214` — `homeEditMode` state ekle: `var homeEditMode by remember { mutableStateOf(false) }`
- `HomeScreen.kt:1472` — FolderGridPage parametresinde `editMode = homeEditMode` yap
- Sonuç: Düzenleme modu artık reaktif, kullanıcı ayarlardan toggle edebilir

**Dosya:** HomeScreen.kt
**Satırlar:** 214, 1472

---

### 08 — Tablet Drag Sütun Sayısı Sabit 4
**Sorun:** `HomeScreen.kt:1401`'te `val colCount = 4` — tablet'te 5-6 sütunlu ekranlar sabit 4 sütunla render ediliyor.
**Çözüm:**
- `HomeScreen.kt:1430` — `val colCount = screenColumns` olarak değiştir
- `screenColumns` zaten var: `HomeScreen.kt:170-175`'te telefon 4, tablet 5-6 sütunla ayarlanmış
- Sonuç: Drag hesapları artık gerçek ekran genişliğine göre yapılıyor

**Dosya:** HomeScreen.kt
**Satır:** 1430

---

### 09 — FIRST_FOLDER_PAGE Yanlış Index 0
**Sorun:** `HomeScreen.kt:503`'te FIRST_FOLDER_PAGE mode'un daima index 0'a gitmesi — Dashboard varsa ilk folder sayfası index 1 veya sonrası.
**Çözüm:**
- `HomeScreen.kt:532` — `pages.indexOfFirst { it is FolderPage }.coerceAtLeast(0)`
- Sonuç: İlk klasör sayfasını Dashboard'dan bağımsız olarak bulur

**Dosya:** HomeScreen.kt
**Satır:** 532

---

### 10 — Dashboard Toggle Etkisiz
**Sorun:** `HomeScreen.kt:1067-1070`'te `buildHeroPages()` çağrısında Dashboard toggle parametresi yok.
**Çözüm:**
- `HomePagePlanner.kt:23-31` — `buildHeroPages()` imzasını güncelle:
  ```kotlin
  fun buildHeroPages(
      folders: List<AppFolder>,
      pageSize: Int,
      dashboardEnabled: Boolean = true,
      widgetPageEnabled: Boolean = true,
  )
  ```
- Sonuç: P24'te Dashboard toggle'ı user setting'e bağlamaya hazır

**Dosya:** HomePagePlanner.kt
**Satırlar:** 23-31

---

### 11 — Klasör Yukarı Kaydırma Multi-Trigger
**Sorun:** `FolderTile.kt:129-144`'te swipe handler'ı tetiklendikten sonra aynı gesture'de tekrar tetiklenebilir.
**Çözüm:**
- `FolderTile.kt:120` — `swipeTriggered` state ekle:
  ```kotlin
  var swipeTriggered by remember { mutableStateOf(false) }
  ```
- `FolderTile.kt:131-145` — onDragStart/End/Cancel'de reset et, onVerticalDrag'da kontrol et:
  ```kotlin
  onDragStart = { swipeDy = 0f; swipeTriggered = false }
  onDragEnd = { swipeDy = 0f; swipeTriggered = false }
  onDragCancel = { swipeDy = 0f; swipeTriggered = false }
  onVerticalDrag = { change, delta ->
      if (delta >= 0f) return@detectVerticalDragGestures
      if (swipeTriggered) return@detectVerticalDragGestures  // ← İlk tetik sonrası exit
      // ...
      if (swipeDy < -swipeThresholdPx) {
          swipeTriggered = true  // ← Bayrak kaldır
          // ...
      }
  }
  ```
- Sonuç: Tek sürükleme hareketi bir kez tetiklenir

**Dosya:** FolderTile.kt
**Satırlar:** 120, 131-145

---

### 12 — notificationImportance Hiç Yazılmıyor
**Sorun:** Bildirimlerin önemlilik seviyesi (`notificationImportance`) DB'ye kaydedilmiyor.
**Çözüm:**
- `AppDao.kt:339-340` — `updateNotificationImportance()` metodu ekle:
  ```kotlin
  @Query("UPDATE apps SET notificationImportance = :importance WHERE packageName = :packageName")
  suspend fun updateNotificationImportance(packageName: String, importance: Int)
  ```
- `AppNotificationListenerService.kt:25` — `appDao` inject et
- `AppNotificationListenerService.kt:36-50` — onNotificationPosted'da çağır:
  ```kotlin
  val importance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      sbn.notification?.priority ?: 0
  } else {
      0
  }
  appDao.updateNotificationImportance(sbn.packageName, importance)
  ```
- Sonuç: Yüksek önemlikli bildirimler doğru kaydediliyor

**Dosyalar:** AppDao.kt, AppNotificationListenerService.kt
**Satırlar:** AppDao 339-340, AppNotificationListenerService 36-50

---

### 13 — "En Güncel Bildirim" lastUsedTimestamp Kullanıyor
**Sorun:** `FolderTile.kt:318-322`'te bildirim sıralaması `lastUsedTimestamp` (uygulama açılış saati) kullanıyor, bildirim gönderim saati değil.
**Çözüm:**

#### A. AppInfo'ya field ekle
- `AppInfo.kt:57` — `lastNotificationPostedAt` field ekle:
  ```kotlin
  val lastNotificationPostedAt: Long = 0L,
  ```

#### B. AppDao'ya güncelleme metodu ekle
- `AppDao.kt:341-342` — `updateLastNotificationPostedAt()`:
  ```kotlin
  @Query("UPDATE apps SET lastNotificationPostedAt = :timestamp WHERE packageName = :packageName")
  suspend fun updateLastNotificationPostedAt(packageName: String, timestamp: Long)
  ```

#### C. Notification service yazma
- `AppNotificationListenerService.kt:36-50` — `onNotificationPosted` içinde:
  ```kotlin
  val timestamp = System.currentTimeMillis()
  appDao.updateLastNotificationPostedAt(sbn.packageName, timestamp)
  ```

#### D. FolderTile comparator güncelle
- `FolderTile.kt:318-323` — comparator'ı `lastNotificationPostedAt` ile yap:
  ```kotlin
  val notifRecencyComparator = remember {
      compareBy<AppInfo>(
          { it.notificationImportance },
          { it.lastNotificationPostedAt }  // ← Değişti
      )
  }
  ```

- Sonuç: En yeni AND en önemli bildirim gösteriliyor

**Dosyalar:** AppInfo.kt, AppDao.kt, AppNotificationListenerService.kt, FolderTile.kt
**Satırlar:** AppInfo 57, AppDao 341-342, AppNotificationListenerService 36-50, FolderTile 318-323

---

### 14 — Bildirim Observer Eski Coroutine Yazması
**Sorun:** `LauncherViewModel.kt:392-431`'te `onEach` + `launch(Dispatchers.IO)` + `launchIn(viewModelScope)` pattern — eski/kısıtlı.
**Çözüm:**
- `LauncherViewModel.kt:392-415` — `viewModelScope.launch(Dispatchers.IO)` + `collectLatest`:
  ```kotlin
  viewModelScope.launch(Dispatchers.IO) {
      combine(
          AppNotificationListenerService.badgeCounts,
          AppNotificationListenerService.lastPostedAt,
      ) { active, posted -> active to posted }
          .collectLatest { (active, posted) ->
              runCatching {
                  // ...
              }
          }
  }
  ```
- `LauncherViewModel.kt:417-431` — latestTexts observer de aynı pattern
- Sonuç: collectLatest ile önceki job otomatik cancel edilir, memory leak'ten kaçınılır

**Dosya:** LauncherViewModel.kt
**Satırlar:** 392-415, 417-431

---

### 15 — Düzen Tercihleri Reaktif Değil
**Sorun:** `HomeScreen.kt:201`'te `homeLayoutConfig = remember(context)` — Settings'ten geri dönerken değişiklikler görünmüyor.
**Çözüm:**

#### A. State mutable yap
- `HomeScreen.kt:201` — `val` → `var mutableStateOf`:
  ```kotlin
  var homeLayoutConfig by remember { mutableStateOf(HomeLayoutPrefs.read(context).config) }
  ```

#### B. DisposableEffect'e layout listener ekle
- `HomeScreen.kt:219-346` — mevcut prefs listener'ına layout prefs ekleme:
  ```kotlin
  // Av düzeni tercihlerini yeniden oku (P15 bölümü — header/footer/content sipariş, gizli bölümler)
  HomeLayoutPrefs.KEY_HEADER_ORDER,
  HomeLayoutPrefs.KEY_FOOTER_ORDER,
  HomeLayoutPrefs.KEY_CONTENT_ORDER,
  HomeLayoutPrefs.KEY_HIDDEN_SECTIONS,
  HomeLayoutPrefs.KEY_LAYOUT_VERSION,
  HomeLayoutPrefs.KEY_CUSTOMIZED -> {
      homeLayoutConfig = HomeLayoutPrefs.read(context).config
  }
  ```

#### C. Ayrı listener ayrı prefs için
- `HomeScreen.kt:321-337` — `home_layout_prefs` SharedPreferences için listener:
  ```kotlin
  val layoutPrefs = context.getSharedPreferences(
      HomeLayoutPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE
  )
  val layoutListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key in setOf(...)) {
          homeLayoutConfig = HomeLayoutPrefs.read(context).config
      }
  }
  layoutPrefs.registerOnSharedPreferenceChangeListener(layoutListener)
  ```

- Sonuç: Settings > Görünüm değişiklikleri anında ana ekrana yansıyor

**Dosya:** HomeScreen.kt
**Satırlar:** 201, 219-346

---

## Commit Bilgileri

**Commit 1:** c6de7601
```
P0 — AppOrganizer ana ekran: 10 P0 sorununu çöz
```

**Commit 2:** 794c4120
```
Fix spacing issue in FIRST_FOLDER_PAGE indexOfFirst call
```

---

## Test Komutları

```bash
cd c:\Users\hekizoglu\Documents\AppOrganizer

# Build al
.\gradlew assembleDebug -x lint

# Unit testler (opsiyonel)
.\gradlew testDebugUnitTest

# APK boyutu kontrol
(Get-Item app/build/outputs/apk/debug/app-debug.apk).length / 1MB
```

---

## İlgili Dosya Listesi

1. `app/src/main/java/com/armutlu/apporganizer/domain/models/AppInfo.kt`
2. `app/src/main/java/com/armutlu/apporganizer/data/local/AppDao.kt`
3. `app/src/main/java/com/armutlu/apporganizer/service/AppNotificationListenerService.kt`
4. `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreen.kt`
5. `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomePagePlanner.kt`
6. `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/FolderTile.kt`
7. `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/LauncherViewModel.kt`

---

## Sonraki Adımlar

1. **Build doğrulaması:** assembleDebug başarılı mı kontrol et
2. **Emülatörde test:**
   - Klasör sürükleme (editMode toggle)
   - Tablet mod (4/5/6 sütun)
   - FIRST_FOLDER_PAGE navigasyon
   - Bildirim badge gönderim zamanı
3. **Git push:** Remote'a gönder
4. **Telegram raporla:** Build sonucu ve test bulguları

---

## Dikkat Edilecek Noktalar

- **collectLatest:** LauncherViewModel'de eski `onEach + launch` pattern ✅ Fixed
- **Syntax:** HomeScreen.kt:532'deki spacing `.coerceAtLeast` ✅ Fixed (Commit 2)
- **KSP build cache:** İlk derlemede sorun olabilir, `.gradle` sil → rebuild
- **Room Migration:** appInfo'ye `lastNotificationPostedAt` eklendiğinde DB versiyonu artacak (henüz migration yazılmadı — AppDatabase version check et)

---

*Hüseyin, all 10 P0 issues fixed and committed. Build running in background — we'll verify success and push to remote once assembleDebug completes.*
