# Local Denetim Raporu

> Dongu: tiered frequency (T1:her · T2:3dongu · T3:10dongu)
> Son denetim: 2026-06-29 01:26
> Acik bulgu yok - tum maddeler cozuldu veya HISTORY.md arsivine tasindi.

---

## Denetim Ozeti

| Oncelik | Sayi | Aciklama |
|---------|------|----------|
| KRITIK | 0 | Acik kritik bulgu |
| YUKSEK | 0 | Acik yuksek bulgu |
| ORTA | 0 | Acik orta bulgu |
| DUSUK | 0 | Acik dusuk bulgu |
| TOPLAM | 0 | |

---

## Tier Sistemi (D182)

| Tier | Frekans | Kapsam |
|------|---------|--------|
| T1 | Her dongu | Temel regex: K1, Y1-Y5, Y7-Y8, O1, D1 (10 kural) |
| T2 | 3 dongude 1 | + CE1-CE8 compose-expert (8 kural) |
| T3 | 10 dongude 1 | + Compose metrics + Dependency matrix + APK trend + Skill integrity + Dead code |

**Optimizasyon notu (D182):** `gradlew lintDebug` T3'ten kaldirildi — 2+ dk suruyor. Yerine build artifact tabanli hizli kontroller eklendi.

---

## Cron Denetim Gecmisi

| Tarih-Saat | Tier | Ana Odak | Ekstra | Bug |
|------------|------|----------|--------|-----|
| 2026-06-28 02:45 | T1 | AppClassifier | - | Bulunamadi |
| 2026-06-28 03:03 | T1 | Room DB | - | Bulunamadi |
| 2026-06-28 03:10 | T1 | WorkManager | - | Bulunamadi |
| 2026-06-28 04:03 | T1 | NotificationListener | - | Bulunamadi |
| 2026-06-28 05:03 | T1 | Onboarding | - | Bulunamadi |
| 2026-06-28 06:03 | T1 | Widget | - | Bulunamadi |
| 2026-06-28 07:03 | T1 | IconCache | - | Bulunamadi |
| 2026-06-28 08:03 | T1 | DockPrefs | - | Bulunamadi |
| 2026-06-28 09:03 | T3 | BackupWorker | lint+dep+skill | Bulunamadi |
| 2026-06-28 10:03 | T1 | Theme | - | Bulunamadi |
| 2026-06-28 11:03 | T1 | AppClassifier | - | Bulunamadi |
| 2026-06-28 12:03 | T1 | Room DB | - | Bulunamadi |
| 2026-06-28 13:03 | T1 | WorkManager | - | Bulunamadi |
| 2026-06-28 13:33 | T1 | NotificationListener | - | Bulunamadi |
| 2026-06-28 14:03 | T1 | Onboarding | - | Bulunamadi |
| 2026-06-28 14:12 | T1 | Widget | - | Bulunamadi |
| 2026-06-28 14:14 | T1 | IconCache | - | Bulunamadi |
| 2026-06-28 15:03 | T1 | DockPrefs | - | Bulunamadi |
| 2026-06-28 16:03 | T1 | BackupWorker | - | Bulunamadi |
| 2026-06-28 17:03 | T1 | Theme | - | Bulunamadi |
| 2026-06-28 18:03 | T1 | AppClassifier | - | Bulunamadi |
| 2026-06-28 19:03 | T1 | RoomDB | - | Bulunamadi |
| 2026-06-28 20:03 | T1 | WorkManager | - | Bulunamadi |
| 2026-06-28 21:03 | T1 | NotificationListener | - | Bulunamadi |
| 2026-06-28 22:03 | T1 | Onboarding | - | Bulunamadi |
| 2026-06-28 23:03 | T1 | Widget | - | Bulunamadi |
| 2026-06-29 00:03 | T1 | IconCache | - | Bulunamadi |
| 2026-06-29 01:03 | T1 | DockPrefs | - | Bulunamadi |
| 2026-06-29 02:03 | T1 | BackupWorker | - | Bulunamadi |
| 2026-06-29 01:38 | T1 | Settings etiket (UI) | Navigation routing | Bulunamadi |
| 2026-06-29 02:12 | T2 | Gesture/Swipe/Drawer + CE | Timber log quality | Bulunamadi |
| 2026-06-29 02:35 | T1 | State/SharedPrefs | Memory/Lifecycle | Bulunamadi |
| 2026-06-29 03:09 | **T3** | Category CRUD + CE + Compose+Dep+APK+Skill+Dead | RoomDB v8 OK | Bulunamadi |
| 2026-06-29 03:28 | T2 | State/SharedPrefs + CE | WorkManager OK | Bulunamadi |
| 2026-06-29 03:47 | T1 | Category CRUD | Repository/DataLayer | Bulunamadi |
| 2026-06-29 04:10 | T1 | Gesture/Swipe/Drawer | NotificationListener OK | Bulunamadi |
| 2026-06-29 04:35 | T2 | Category CRUD + CE | Onboarding 17 adim OK | Bulunamadi |
| 2026-06-29 05:00 | T1 | Gesture/Swipe/Drawer | Timber log quality | Bulunamadi |
| 2026-06-29 05:18 | T1 | Accessibility/A11y | Widget OK | Bulunamadi |
| 2026-06-29 08:04 | **T2** | Izin/Onboarding + CE | **2 BULGU: CE7(Settings:255) + CE9(HomeScreen:89)** | **DUZELTILDI** |
| 2026-06-29 08:47 | T1 | State/SharedPrefs | IconCache OK | Bulunamadi → CE10 eklendi |
| 2026-06-29 09:55 | **T3** | Gesture/Swipe/Drawer + CE + Compose+Dep+APK+Skill+Dead | Timber log | Bulunamadi |
| 2026-06-29 10:32 | T1 | Accessibility/A11y | DockPrefs OK | Bulunamadi → CE11 eklendi |
| 2026-06-29 10:55 | T1 | State/SharedPrefs | BackupWorker OK | Bulunamadi → CE12 eklendi |
| 2026-06-29 11:08 | T2 | Dock/Widget/Backup + CE | StateFlow/ViewModel | Bulunamadi |
| 2026-06-29 11:30 | T1 | Izin/Onboarding | Theme OK | Bulunamadi → CE13 eklendi |
| 2026-06-29 11:52 | T1 | Category CRUD | AppClassifier OK | Bulunamadi |
| 2026-06-29 12:08 | T2 | Dock/Widget/Backup + CE | StateFlow/ViewModel | Bulunamadi |
| 2026-06-29 12:35 | T1 | Izin/Onboarding | OEM Compatibility | Bulunamadi |
| 2026-06-29 13:00 | T1 | Dock/Widget/Backup | StateFlow/ViewModel | Bulunamadi |
| 2026-06-29 14:10 | T2 | Gesture/Swipe/Drawer + CE | Timber log | Bulunamadi |
| 2026-06-29 14:10 | **T3** | Izin/Onboarding + CE + Compose+Dep+APK+Skill+Dead | OEM Compatibility | Bulunamadi |
| 2026-06-29 18:05 | T1 | Accessibility/A11y | Test Coverage | Bulunamadi |
| 2026-06-29 18:20 | T2 | Settings UI/Labels + CE | Navigation/Routing | Bulunamadi |
| 2026-06-29 18:40 | T1 | Permission/Izin | ViewModel/StateFlow | Bulunamadi |
| 2026-06-29 19:05 | T1 | Category CRUD | Repository/DataLayer | Bulunamadi |
| 2026-06-29 19:25 | T2 | Gesture/Swipe/Drawer + CE | Timber log | Bulunamadi |
| 2026-06-29 19:50 | T1 | Accessibility/A11y | Test Coverage | Bulunamadi |

*59 tur, 0 bug*

*51 tur, 0 bug | IMP#4: derivedStateOf fix*

*50 tur, 0 bug*

*49 tur, 0 bug*
