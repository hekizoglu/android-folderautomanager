# Logic Sentinel Kurallari

Bu belge, AppOrganizer icin urun mantigi hatalarini erken yakalamak uzere tanimlanan ilk `Logic Sentinel` kural setini aciklar.

## Hedef

- Rapor metni ile hesap mantigi arasindaki kopuklugu bulmak
- Bildirim -> ekran yonlendirme zincirindeki kopukluklari yakalamak
- Cache, sync ve stale state kaynakli tutarsizliklari erken gormek
- Launcher, onboarding, permission ve ayarlar akislarinda sessiz bozulmalari taramak

## Hizli Kurallar

| Id | Oncelik | Kapsam | Neyi yakalar |
|---|---|---|---|
| LS001 | P1 | ViewModel state | `combine` disinda kalan secili/gecici state nedeniyle stale UI |
| LS002 | P1 | UI action | Snapshot state ile toplu islem |
| LS004 | P1 | Repository sync | Insert-delete var, update yok |
| LS005 | P1 | Notification/report | "bugun / en cok actigin" metni ama yanlis metrik |
| LS006 | P1 | Navigation | `putExtra` var, route tuketimi kopuk |
| LS007 | P2 | WorkManager | `cancel + KEEP enqueue` yarisi |
| LS008 | P2 | Suggestion/new app | Siralama olmadan `first()` secimi |
| LS009 | P2 | Android intent | `NEW_TASK` eksikligi |
| LS010 | P2 | Security UX | Biyometrikte fail-open davranis |

## Semantik Kurallar

Ikinci fazda custom detekt veya custom lint ile AST tabanli hale getirilecek hedef kurallar:

1. `StaleStateCombineRule`
2. `ActionUsesSnapshotInsteadOfSourceRule`
3. `LaunchWithoutUsageTrackingRule`
4. `SyncOnlyInsertDeleteRule`
5. `NotificationTextMetricMismatchRule`
6. `DeadIntentExtraRule`
7. `UnsafelyRescheduledWorkerRule`
8. `OnboardingRememberedPrefsRule`
9. `CategoryCountVsFilteredListRule`
10. `BiometricFailOpenRule`

## Calistirma

```powershell
.\gradlew.bat logicAuditFast
.\gradlew.bat :app:detekt
.\gradlew.bat logicAuditSemantic
```

## Raporlama

- `P1`: crash, veri/rakam yanlisligi, kullaniciyi yanlis yonlendiren kritik bulgu
- `P2`: akisi bozan veya OEM/cihaz bazli sorun cikarabilen bulgu
- `P3`: bakim, okunabilirlik veya ileride bug'a donusme riski
