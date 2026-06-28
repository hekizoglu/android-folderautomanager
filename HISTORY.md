# HISTORY.md - AppOrganizer Döngü Arşivi



> CLAUDE.md'den taşınan döngü-spesifik değişiklik logları. **Her konuşmada okunmaz** - sadece "geçmişte X'i nasıl yapmıştık?" sorusunda referans.

---

## Tamamlananlar Arşivi

### FİKİRLER.md'den Taşınanlar
| Tarih | Madde | Döngü |
|-------|-------|-------|
| 2026-06-20 | FCM push mimari kararı LEARNINGS.md'ye eklendi — AppFirebaseMessagingService.kt + AppOrganizerApp.kt FCM init belgelendi | D13x |
| 2026-06-21 | Widget hızlı menü düzeltildi — WidgetArea.kt isDraggable long press mantığı, X butonu gösterilmeye başlandı | D141 |
| 2026-06-21 | İki yeni tema: iOS + AMOLED | D122 |
| 2026-06-21 | Onboarding yeniden yazım (16 adım, CLASSIFY_MODE→SET_LAUNCHER→DONE sırası) | D120 |
| 2026-06-21 | Görsel kalite artırımı | D123 |

### Local Denetim Tamamlananlar Arşivi

#### 2026-06-26 11:26
- `K1` AllApps sıralama tercihi `AppPrefs` üzerinden tek prefs kaynağına taşındı.
- `Y1` `fuzzySearch()` Türkçe locale ile normalize edilerek AppList ve drawer araması hizalandı.
- `Y2` Klasör arama sayacı `snapshotFlow` ve `collectLatest` ile eski sayaçları iptal edecek hale getirildi.
- `Y3` `FolderTile` içindeki `swipeDy` recomposition güvenli Compose state oldu.
- `Y4` Launcher varsayılan durumu tekrar hesaplama yerine hatırlanan state ile yönetildi.
- `O1` Kategori sekmeleri ViewModel tarafında önceden hesaplanan `visibleCategories` listesine taşındı.
- `O2` All Apps içindeki recent ve favorite ikon cache anahtarlarına `lastUpdatedTime` eklendi.
- `O3` `AppClassifier` üzerindeki global mutable flag kaldırıldı; sınıflandırma tercihi çağrı bazlı parametre oldu.
- `O4` Klasör arama temizleme akışı tek aktif sayaçla sınırlandı.
- `O5` `filteredApps` ve kategori istatistikleri her erişimde değil state üretiminde hesaplanır hale geldi.
- `D1` Kullanılmayan `itemHeightDp` parametreleri temizlendi.
- `D2` Ayarlar ekranındaki en dolu kategori hesabı önbelleğe alınmış state üzerinden okunur hale geldi.
- `D3` Tekrar doğrulandı; `isLoading` değişkeni loading fallback ekranında kullanıldığı için yanlış alarm olarak kapatıldı.

#### 2026-06-27 01:46
- Manuel semantik denetimdeki `Tüm Kategorileri Sıfırla` satırı onay dialogu ile korundu.
- Dock `Varsayılanlara Sıfırla` satırı chevron olmadan ve onay dialogu ile çalışacak şekilde düzeltildi.
- `İzin Ver` etiketi `Bildirim Erişimini Aç` olarak güncellendi.
- `Otomatik Yedekleme` açıklaması haftalık periyodik worker davranışını doğru anlatır hale getirildi.
- `Geri Yükle` akışına içe aktarma öncesi onay dialogu eklendi.
- `Klasör Önizleme` ayarı `Yukarı Kaydırma İpucu` olarak yeniden adlandırıldı.
- App listesi menüsündeki `Yeniden Sınıflandır` aksiyonu netleştirildi ve onay dialogu ile korundu.

#### 2026-06-27 02:28
- `A1-A2` `LauncherActivity` home-press zamanı `savedInstanceState` ile korundu; receiver kaydı `onStart/onStop`'a taşındı.
- `A3` `HomeScreen` swipe state'i `rememberSaveable` ile config-change güvenli hale getirildi.
- `A4` `AppContextMenu` favori durumu ViewModel state'iyle hizalandı.
- `A5` `FolderRenameDialog` boş isimde kaydı engelleyen hata ve disabled confirm davranışı kazandı.
- `A7` `WidgetArea` drag sıralama hesabı gerçek ölçülen kart yüksekliğine bağlandı.
- `A13` Arama geçmişi chip'lerine tıklanabilirlik semantics'i eklendi.
- `A15` Alfabetik drawer başlıkları `heading()` semantics'i ile erişilebilir hale getirildi.
- `P1-P9` İzin sorunları: `PermissionHelper` kaldırıldı, bildirim izninde fallback akışı eklendi, `GET_INSTALLED_PACKAGES` manifest izninden silindi, `QUERY_PACKAGES` onboarding adımı skippable yapıldı.
- `C1-C10` Kategori CRUD akışı gerçek Room verisine bağlandı; boş/duplicate ad engellendi; sistem kategorisi silme DAO'da korundu.

#### 2026-06-27 03:20
- `P2` Onboarding akışına `Usage Access` adımı eklendi.
- `P10` `PermissionsBanner` snooze süresi `BANNER_SNOOZE_DAYS` üzerinden okunur hale getirildi.
- `A8-A18` TalkBack/erişilebilirlik: bildirim sayısı semantics, dock icon semantics, öneri fallback icon, FavoritesRow/RecentAppsRow, klasör swipe ipucu, SwipeHint live region, HomeScreenPageIndicator tab rolü, MiniAppIcon fallback, FolderSheet onClick etiketi.
- `S2` `FolderTile` drag başlangıcında `swipeDy` sıfırlandı.
- `S4-S7` FolderTile erişilebilir semantics, swipe ipucu screen reader dostu hale getirildi.
- `C8-C9` Kategori seçicilerde kapanış davranışı ve semantics hizalandı.
- Denetim otomasyonu saatlik Full + 15 dk Resolve görev akışı ile yeniden kurgulandı.

#### 2026-06-27 09:29
- `Y5` `Theme.kt` içinde `darkTheme` tekrar aktif; sistem açık/koyu tercihi artık uygulanıyor.
- `O7` `DockPrefs.removeFromDock` Boolean dönüyor, ViewModel wrapper toast gösteriyor.
- `O8` `PackageManagerHelper.kt` riskli `endsWith` kaldırıldı; gizleme mantığı prefix bazlı hale getirildi.
- `F1-F4` `LauncherSetupScreen.kt` launcher sonuç kontrolü, güvenli fallback ve doğru başlıkla kapatıldı.
- `Y6`, `F5`, `F6` — yanlış alarm olarak kapatıldı.
- Denetim otomasyonu `scripts/register_audit_cron.ps1` saatlik tam denetim + 5 dk sonra resolve turu modeline güncellendi.

#### 2026-06-27 09:48
- `K9` `AppListViewModel.kt` `getAllCategoriesFlow()` API çağrısı denetlendi; `NoSuchMethodError` clean build + yeniden APK yükleme ile kapanır.
- Denetim sistemine `K9` (KRİTİK) API senkronizasyon kuralı eklendi; `H` grubu (Derleme ve API Senkronizasyonu) kuralları eklendi.
- Denetim sıklığı 15 dakikaya düşürüldü, 8 odak alanı + 1 ekstra denetim rotasyonu aktif.

---

### ÇÖZÜLEMEYEN_SORUNLAR.md Çözülenler Arşivi
| # | Sorun | Çözüm | Tarih |
|---|-------|-------|-------|
| CS-1 | HISTORY.md `→` encoding | `->` ile değiştirildi | 2026-06-21 |
| CS-2 | Windows Defender build lock (kapt) | Admin PS'de `Add-MpPreference` çalıştırıldı | 2026-06-16 |
| — | PowerShell heredoc `<<'EOF'` | `@'...'@` syntax kullanılmalı | 2026-06-16 |
| — | Git push non-fast-forward | `git pull --rebase` | 2026-06-15 |
| — | KAPT incremental cache kilit | `kapt.incremental.apt=false` + robocopy | 2026-06-16 |
| — | HISTORY.md Türkçe mojibake | `fix_encoding.py` TURKISH_DOUBLE_ENCODED | 2026-06-16 |
| E14 | AllAppsDrawer `derivedStateOf` + plain param | `remember(apps)` key-based | 2026-06-21 |
| LD-* | 10 adet saatlik otomatik denetim girişi | K9/Y6/O7 kapatıldı, tekrarlayan girişler temizlendi | 2026-06-28 |

> Append-only. Yeni döngü özetleri sona eklenir.

>

> Kalıcı kurallar -> `CLAUDE.md` | Promote öğrenmeler -> `LEARNINGS.md`



---

## MD Denetim D147 — 2026-06-28
**Yapılanlar:** Rutin MD denetimi (3. geçiş). S1/S7 ÇÖZÜLDÜ (D140-D146 logları eklendi, widget menü düzeltildi). 4 yeni/açık madde tespit edildi: N1 (FİKİRLER 15+ puan maddeleri ROADMAP'a eksik), N2 (ROADMAP stale — D123'te kaldı), N3 (LEARNINGS Promote Bekleyenler temizlik), S6 devam (merged_res + KAPT açık). MD_DENETIM_2026-06-23.md güncellendi.
**Agent:** —
**CLAUDE.md/LEARNINGS.md:** Değişmedi
**Sonraki:** Onay bekleniyor — 4 sorun için ROADMAP + LEARNINGS + FİKİRLER güncellemesi

## Döngü D144 — 2026-06-28
**Yapılanlar:** Local denetim raporu temizliği. K9 [ÇÖZÜLDÜ] — getAllCategoriesFlow tüm katmanlarda tanımlı, clean build ile API senkron. Y6 [ÇÖZÜLDÜ — yanlış alarm] — OnboardingScreen.kt:108 ve 294'te shouldShowRequestPermissionRationale ve ACTION_APPLICATION_DETAILS_SETTINGS zaten mevcut, NOTIFICATIONS isSkippable=true. O7 [ÇÖZÜLDÜ] — DockPrefs.removeFromDock Boolean dönüyor, ViewModel wrapper toast gösteriyor.
**Dosyalar:** local_denetim_otomatik_rapor.md silindi (0 bulgu), local_denetim_raporu.md sıfırlandı, qa/local_denetim_raporu.md senkronize, COZULEMEYEN_SORUNLAR.md 10 adet LD-* saatlik tekrar girişi temizlendi.
**CLAUDE.md/LEARNINGS.md:** Değişmedi
**Sonraki:** Yeni özellik veya ROADMAP görevi

## Döngü D145 — 2026-06-28
**Yapılanlar:** 3 bug/özellik: (1) Kullanım sayısı "23 milyon" bug'ı düzeltildi — NiagaraComponents.kt:77 `"${usageCount}×"` → `formatUsageMs()` (ms→insan okunabilir format). (2) Sort toggle — AllAppsDrawer'da 4 base chip, aynı butona basınca yön değişir (A→Z↔Z→A, Kullanım↓↔↑, Boyut↓↔↑, Yükleme↓↔↑); ALPHA_DESC/USAGE_ASC/INSTALL_DATE_ASC enum değerleri eklendi. (3) Klasör auto-size — ekrana taşmayı önlemek için folderSizeDp her zaman maxFolderSize=(screenWidth-32)/4 ile klamplandı; AppPrefs'e KEY_AUTO_FOLDER_SIZE eklendi; Ayarlar'a "Otomatik Boyut Ayarla" toggle eklendi.
**Dosyalar:** AllAppsDrawerUtils.kt, NiagaraComponents.kt, AllAppsDrawer.kt, FolderSheet.kt, AppPrefs.kt, HomeScreen.kt, SettingsHomeScreenSection.kt
**CLAUDE.md/LEARNINGS.md:** Değişmedi
**Sonraki:** Onboarding ayar sihirbazı (FİKİRLER'e eklendi)

## Döngü D146 — 2026-06-28
**Yapılanlar:** CS-3 (Gradle build kilit) için 4. yöntem: UAC self-elevation PowerShell script (`scripts/add_defender_exclusion.ps1`) oluşturuldu — kullanıcı sağ tıkla çalıştırınca UAC prompt çıkar, Evet deyince exclusion eklenir. local_denetim_otomatik_rapor.md encoding düzeltildi, stale K9/Y6/O7 temizlendi. FİKİRLER "Akşam Önerisi Algoritma Açıklaması" tamamlandı — SettingsHomeScreenSection'a öneri açık olunca algoritma detay kartı eklendi (28 gün, %40 yenilik + %40 sıklık + %20 zaman dilimi).
**Dosyalar:** scripts/add_defender_exclusion.ps1, COZULEMEYEN_SORUNLAR.md, local_denetim_otomatik_rapor.md, SettingsHomeScreenSection.kt, FİKİRLER.md
**Agent:** —
**Sonraki:** Döngü 2 — 45 dk sonra. CS-3 UAC script kullanıcı testi bekleniyor.

---

## Döngü D148 — 2026-06-28
**Yapılanlar:** local_denetim_otomatik_rapor.md encoding düzeltildi, 0 bulgu. audit.ps1 root cause bulundu: K9/Y6/O7 yanlış alarm olarak scriptten kaldırıldı — artık stale bulgu üretmeyecek.
**Dosyalar:** local_denetim_otomatik_rapor.md, scripts/audit.ps1
**Sonraki:** D149 — kalan audit kuralları temizle.

## Döngü D149 — 2026-06-28
**Yapılanlar:** audit.ps1 tüm yanlış alarm kuralları temizlendi: O2 (lastUpdatedTime zaten eklendi), O3 (flag kaldırıldı), O5 (getter değil field), O6 (ThemePreferences Hilt bağlı), O8 (endsWith kaldırıldı D114'te). Script artık 0 yanlış alarm üretiyor.
**Dosyalar:** scripts/audit.ps1
**Sonraki:** D150 — BUILD.

## Döngü D150 — 2026-06-28 BUILD
**Yapılanlar:** assembleDebug BAŞARILI 41s (cache). APK 25.77 MB. Telegram'a gönderildi. CI workflow'ları workflow_dispatch'e alındı (push triggerı kaldırıldı).
**Dosyalar:** .github/workflows/*.yml
**Sonraki:** 45 dk döngü devam — FİKİRLER yüksek puanlı görevler.

## Döngü D151 — 2026-06-28
**Yapılanlar:** 5-skill kurulum ve test: compose-expert (.claude/skills/, 27 ref + 6 source), code-review (built-in), security-review (built-in), caveman (npx skill-caveman, %65 token tasarrufu). Saatlik cron e5e7066c kuruldu. audit.ps1'e CE1-CE5 compose-expert kuralları eklendi (remember config-key, indexOf, Canvas zero-size, derivedStateOf, modifier sırası). Telegram bildirimi test edildi (msg_id:820). Rapor formatı sadeleştirildi (tarih-saat + bug bulunamadı).
**Agent:** WebSearch (aitmpl.com, caveman, compose-skill)
**Dosyalar:** .claude/skills/compose-expert/, .claude/skills/caveman/, scripts/audit.ps1, local_denetim_raporu.md
**Sonraki:** Cron otonom — 5-skill + ekstra rotasyon saatlik.
