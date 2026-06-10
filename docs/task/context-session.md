# AppOrganizer — Context Session (Source of Truth)

> Bu dosya ajanlar arası iletişimin tek kaynağıdır.
> Her subagent işe başlamadan önce okur, bitişte günceller.

---

## Proje Durumu

**Son build:** v0.9 (commit `46af11b`)
**Tarih:** 2026-06-10
**Branch:** main (GitHub'a push edildi)

---

## Tamamlanan Görevler (v0.9)

| Görev | Detay |
|---|---|
| Async icon loading | `produceState` + `withContext(Dispatchers.IO)` — AppListScreen |
| Haptic feedback | AllAppsDrawer, FolderSheet, FolderTile — `HapticFeedbackType.LongPress` |
| Dock kullanıcı seçimi | `DockPrefs.kt` + `LauncherViewModel` dockPackages StateFlow |
| Ayarlar launcher butonu | SettingsScreen'de "Ayarla" / "Değiştir" + Dock yönetimi UI |
| LauncherActivity onResume | Default launcher kontrolü — dialog durumu güncelleme |

---

## Aktif Backlog

### 1. Onboarding Akış Sırası (⏳ Sıradaki)
**İstek:** Uygulama kurulunca açılır → kullanıcı tüm ayarları yapar → EN SONUNDA default launcher seçimi gelir.

**Mevcut sıra:**
```
WELCOME → SET_LAUNCHER → QUERY_PACKAGES → NOTIFICATIONS → ACCESSIBILITY → DONE
```

**Hedef sıra:**
```
WELCOME → QUERY_PACKAGES → NOTIFICATIONS → ACCESSIBILITY → SET_LAUNCHER → DONE
```

**İlgili dosya:** `app/src/main/java/com/armutlu/apporganizer/presentation/ui/screens/OnboardingScreen.kt`

---

### 2. AppListScreen Refactor (⏳ Bekliyor)
**İstek:** Büyük dosyayı küçük composable'lara böl.
**İlgili dosya:** `presentation/ui/screens/AppListScreen.kt`

---

### 3. Play Store Hazırlık (⏳ Bekliyor)
- Privacy policy URL
- Store screenshots
- Content rating
- ProGuard kuralları
- Release keystore: `release.jks` (şifre güvenli saklanmalı)

---

## Mimari Notlar

- **Tema:** Teal 600 (`#00897B`) + Cyan (`#26C6DA`)
- **Launcher:** `CATEGORY_HOME` + `ROLE_HOME` (API 29+)
- **DI:** Hilt
- **State:** StateFlow + collectAsState
- **Icons:** async — `produceState` + `Dispatchers.IO`
- **Haptic:** `LocalHapticFeedback` / `HapticFeedbackType.LongPress`
- **Dock persist:** `DockPrefs.kt` (SharedPreferences)

---

## Subagent Çıktı Dosyaları

| Dosya | İçerik | Durum |
|---|---|---|
| `onboarding-plan.md` | Onboarding akış refactor planı | ⏳ Oluşturulmadı |
| `applist-refactor-plan.md` | AppListScreen composable bölünme planı | ⏳ Oluşturulmadı |

---

*Güncelleme: 2026-06-10 — Ana Agent*
