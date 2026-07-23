# AppOrganizer — Merkezi Görev ve Kanıt Dosyası

> **Amaç:** Tüm aktif işleri tek yerden yönet. Her görev dosya referansları, ölçülebilir kanıt ve bağımlılıklar içerir.  
> **Kural:** Görev `[x]` işaretlenirse **kod commit'lenmiş**, **test yapılmış**, **ROADMAP.md güncellenmişse.**  
> **Kaynak:** ROADMAP.md → TASKS.md (operasyonel) + HISTORY.md (tamamlanmış)  
> **Son güncelleme:** 2026-07-23  

---

## P0 — Acil (Bu Haftada Bitme)

### P0.1 Sabit "Her Şeyi Ara" tüm sayfalarda
**Dosyalar:** HomeScreen.kt, HomeShell.kt, GlobalSearchHost.kt, HeroDashboardPage.kt  
**Sorun:** Arama çubuğu sayfa 0 (Hero) değişiyor, sayfa 1+ uygulamalarıyla tutarlı değil.  
**Kanıt Gereksinimi:**
- [x] HomeScreen.kt içinde `currentPage != 0` koşulu silinmiş
- [x] GlobalSearchHost sabit konumda (üst/alt)
- [x] HeroSearchCard, geçmiş/kişileri açan link (arama alanı değil)
- [x] Compile ✅ + testDebugUnitTest ✅
- [x] Commit mesajı: "Fix: Global search consistent across all pages"
**Durum:** ✅ TAMAMLANDI (2026-07-23 23:30)  

### P0.2 Arama klavyesi gecikmesi (overlay → focus → IME sırası)
**Dosyalar:** GlobalSearchHost.kt, LauncherViewModel.kt, FullScreenSearchOverlayV2.kt, HomeScreenComponents.kt  
**Sorun:** Overlay, focus ve IME aynı frame'de açılıyor → klavye takılıyor.  
**Çözüm:** LaunchedEffect ile 100ms delay, çift IME padding kaldırıldı.  
**Kanıt Gereksinimi:**
- [x] FullScreenSearchOverlayV2.kt: `LaunchedEffect { delay(100); requestFocus(); show() }`
- [x] IME padding: overlay'den kaldırıldı (HomeShell'de zaten var)
- [x] Import: `kotlinx.coroutines.delay` eklendi
- ✅ Commit: HEAD ded2625d
- ⏳ Compile + testDebugUnitTest (assembleDebug devam)
**Durum:** ✅ KOD TAMAMLANDI (2026-07-24 01:30), test bekleniyor  

### P0.3 Klasör ekranı → HomeShell içine taşı
**Dosyalar:** FolderScreen.kt, HomeShell.kt, HomeScreen.kt, LauncherNavGraph.kt  
**Sorun:** FolderScreen ayrı full-screen Activity, dock ve arama kaybolıyor.  
**Çözüm:** HomeShell'de folderOverlay slot, AnimatedVisibility ile modal.  
**Yapılanlar:**
- [x] FolderScreen.kt: BackHandler enable koşulu, padding kaldırıldı
- [x] HomeShell.kt: folderOverlay slot + Z-order (dock/arama sabit)
- [x] HomeScreen.kt: FolderScreen overlay'e entegre
- [x] LauncherNavGraph.kt: ROUTE_FOLDER kaldırıldı (0 reference)
- [x] Geri tuşu: openFolder state kapatıyor
- ⏳ Compile + testDebugUnitTest (build devam)
**Durum:** ✅ KOD TAMAMLANDI (2026-07-24 01:45), test bekleniyor  

### P0.4 1.000 dosya sınırı → parçalı MediaStore indeksleme
**Dosyalar:** FilesIndexer.kt  
**Sorun:** `private const val MAX_FILES = 1000` sabit, sırayla tran → ilk türden sonra biter.  
**Çözüm:** Pagination + type quotas (images 3K, videos 1K, audio 1K, downloads 1K).  
**Yapılanlar:**
- [x] FilesIndexer.kt: MAX_FILES silinmiş
- [x] Type quotas (QUOTA_IMAGES, QUOTA_VIDEOS, vb.) eklenmişse
- [x] Pagination döngüsü: `while (typeCount < quota) { offset += LIMIT }`
- [x] WorkManager progress callback API hazır
- [x] Encoding: em dash → ASCII dash düzeltilmişse
- ⏳ Compile + testDebugUnitTest (build devam)
- [x] Commit: `038d4f1d` "Feat: Paginated file indexing with type-specific quotas"
**Durum:** ✅ KOD TAMAMLANDI (2026-07-24 01:50), build devam  

### P0.5 Performans temel ölçümler (Macrobenchmark)
**Dosyalar:** `:benchmark` module, ComposablesToCheck.kt  
**Hedef:** Cold start, sayfa geçiş, klasör açma, arama açma jank metriği.  
**Kanıt Gereksinimi:**
- [ ] `:benchmark` Gradle modülü oluşturulmuş
- [ ] `androidx.profileinstaller` + `androidx.benchmark` bağımlılığı
- [ ] HomeScreen açılış, klasör açma, AllAppsDrawer, settings dönüş akışları baseline profile capture edilmiş
- [ ] FrameTimingMetric ile önce/sonra karşılaştırması (en az 2 run)
- [ ] Rapor: cold start < 800ms, page transition < 200ms, folder open < 300ms
- [ ] Compile ✅ + benchmark çalıştırılmış
- [ ] Commit mesajı: "Perf: Add macrobenchmark baseline, cold start/page/folder metrics"
**Durum:** ⏳ Beklemede  

### P0.6 Widget sistem karar + entegrasyon
**Dosyalar:** HomeShell.kt, HeroDashboardPage.kt, WidgetArea.kt, HomeAdaptiveLayoutPolicy.kt  
**Sorun:** Widget kodlar var fakat Hero dashboard'a bağlı değil.  
**Karar Seçenekleri:**
- **(A) Tercih:** Widget'lar ayrı yatay sayfa (sayfa 0 Hero, sayfa 1 Widget, sayfa 2+ klasörler)
- **(B):** Widget'lar Hero dashboard alt bölüm
- **(C):** Widget'lar kaldırılıyor (scope dışı)

**Kanıt Gereksinimi (seçim A):**
- [ ] HomeShell HorizontalPager sayfa sayısı +1 (widget page)
- [ ] Widget page, HomeAdaptiveLayoutPolicy padding'i kullanıyor
- [ ] WidgetArea fillMaxWidth() yerine adaptive genişliğe bağlı
- [ ] Compile ✅ + testDebugUnitTest ✅
- [ ] Commit mesajı: "Refactor: Widget system — separate page, consistent padding"
**Durum:** ⏳ **Karar bekleniyor** (Hüseyin A/B/C seçme)

---

## P1 — Yüksek Öncelikli (Önümüzdeki 2 Hafta)

### P1.1 Ana ekrana Düzenleme/Öneri Merkezi
**Dosyalar:** HomeScreen.kt, LauncherViewModel.kt, EditingCenterState.kt (yeni)  
**İçerik:**
```
10 uygulama onay bekliyor
3 klasör birleştirilebilir
4 uygulama yanlış klasörde
2 izin eksik
8 uygulama uzun süredir kullanılmadı
```
Kart seçildiğinde → ilgili inceleme ekranı.

**Kanıt Gereksinimi:**
- [ ] EditingCenterState.kt oluşturulmuş
- [ ] LauncherViewModel içinde state flow bağlı
- [ ] HomeScreen'de kart/badge gösteriliyor
- [ ] Tüm gözlemci (onay, merge, izin, stale) entegre edilmiş
- [ ] Compile ✅ + testDebugUnitTest ✅
- [ ] Commit mesajı: "Feat: Editing Center card on home screen"
**Durum:** ⏳ Beklemede

### P1.2 Sınıflandırma Onay sayısı → Hero dashboard bağla
**Dosyalar:** AppDao.kt, AppRepository.kt, LauncherViewModel.kt, HeroDashboardPage.kt, HomeScreen.kt  
**Yapılanlar:**
- [x] AppDao.kt: `observePendingClassificationCount()` Flow
- [x] AppRepository.kt: wrapper (IO dispatcher)
- [x] LauncherViewModel.kt: `pendingClassificationsCount` StateFlow
- [x] HeroDashboardPage.kt: `PendingClassificationBadge` composable (Material 3)
- [x] HomeScreen.kt: state wiring + navigation
- [x] Real-time updates (Room Flow + SharingStarted.Eagerly)
- [x] Conditional render (count > 0)
- [x] Tap → ClassificationReviewScreen
- ⏳ Compile + testDebugUnitTest (pending)
**Durum:** ✅ KOD TAMAMLANDI (2026-07-24 02:50), test bekleniyor

### P1.3 Klasör birleştirme motoru UI + undo
**Dosyalar:** FolderMergeCandidateScorer.kt, FolderMergeUiState.kt (yeni), FolderMergeScreen.kt (yeni)  
**Sorun:** FolderMergeCandidateScorer domain kodu var, UI/undo yok.  
**Kanıt Gereksinimi:**
- [ ] FolderMergeUiState oluşturulmuş (source, target, apps, confidence, reason)
- [ ] FolderMergeScreen ekranı (compare view: source ← → target)
- [ ] Seçenekler: Birleştir / Reddet / 1 hafta sessize al / 1 ay sessize al / Geri al
- [ ] Merge atomik Room transaction
- [ ] Undo WorkManager ile geçmişe kaydetmiş
- [ ] Compile ✅ + testDebugUnitTest ✅
- [ ] Commit mesajı: "Feat: Folder merge UI + atomic transaction + undo"
**Durum:** ⏳ Beklemede

### P1.4 Bildirim izin uyarısı ilk kullanım akışına bağla
**Dosyalar:** OnboardingScreen.kt, ContextualPermissionDialog.kt, LauncherActivity.kt  
**Sorun:** İzin dialog var fakat onboarding içinde değil.  
**Çözüm:** Ana ekranda izin kartı, özellik ilk kullanıldığında açıklamalı dialog.  
**Kanıt Gereksinimi:**
- [ ] HomeScreen'de bildirim izin kartı (varsayılan kapalı, sessize alınabilir)
- [ ] Bildirim badge özelliğinden biri ilk kez açılırsa dialog
- [ ] Dialog: açıkla / "Ayarlar" / "Daha sonra" seçenekleri
- [ ] Compile ✅ + testDebugUnitTest ✅
- [ ] Commit mesajı: "UX: Add contextual notification permission flow"
**Durum:** ⏳ Beklemede

### P1.5 Arama sonuç skoru model + Google fallback
**Dosyalar:** SearchRepository.kt, SearchScore.kt (yeni), GlobalSearchHost.kt  
**Sorun:** Mevcut `contains()` basit, skor yok. Google fallback sadece 0 sonuçta.  
**Skor Sistemi:**
```
Tam isim: 100
Kelime başlangıcı: 90–99
İçeriyor: 75–89
Fuzzy/Yazım hatası: 50–74
Fonetik: 40–49
```

**Kanıt Gereksinimi:**
- [ ] SearchScore.kt enum/data class (type, score, detail)
- [ ] SearchRepository tüm kaynaklar için skor hesaplıyor
- [ ] Best yerel skor < 85 → "Google'da ara" gösteriliyor
- [ ] Google link sorguyla parametre taşıyor
- [ ] Compile ✅ + testDebugUnitTest (SearchScoring) ✅
- [ ] Commit mesajı: "Feat: Search result scoring + Google fallback below 85%"
**Durum:** ⏳ Beklemede

### P1.6 Arama debounce optimization
**Dosyalar:** LauncherViewModel.kt, SearchRepository.kt  
**Sorun:** Tüm kaynaklar 250 ms debounce, uygulama anında gösterilecek.  
**Çözüm:** Uygulama/klasör anında, FTS sonuçlar 120–150 ms debounce.  
**Kanıt Gereksinimi:**
- [ ] SearchRepository iki akış: instant (app/folder) ve debounced (FTS)
- [ ] debounce 120–150 ms olarak ayarlanmış
- [ ] İlk karakter < 16 ms uygulama filtresi
- [ ] Compile ✅ + testDebugUnitTest ✅
- [ ] Commit mesajı: "Perf: Split search debounce — instant app/folder, delayed FTS"
**Durum:** ⏳ Beklemede

---

## P2 — İnce Ayarlar (Sonraki Ay)

### P2.1 Kompakt filtre tasarımı zorunlu varsayılan
**Dosyalar:** AllAppsDrawer.kt, SettingsScreen.kt  
**Sorun:** Eski 2-satırlı chip tasarımı eski kurulumlardan kalıyor.  
**Kanıt Gereksinimi:**
- [ ] AllAppsDrawer varsayılan compact tune button
- [ ] SettingsScreen migration: `chipRowsEnabled → false`
- [ ] Eski tasarım kod kaldırılmış
- [ ] Compile ✅ + testDebugUnitTest ✅
**Durum:** ⏳ Beklemede

### P2.2 Sayfa göstergesi dock üstü overlay → 48 dp alanı kurtarma
**Dosyalar:** HomeShell.kt, DockArea.kt  
**Sorun:** Gösterge visual 5–9 dp, fakat dokunma hedefi 28 × 48 dp → alan boşa gidiyor.  
**Çözüm:** Overlay gösterge, görünmez 48 dp dokunma hedefi, görsteller 4–7 dp.  
**Kanıt Gereksinimi:**
- [ ] Sayfa göstergesi dock üstünde overlay (Z: 1)
- [ ] Yalnız dokunma hedefi 48 dp, görseller küçük
- [ ] Compile ✅ + testDebugUnitTest ✅
**Durum:** ⏳ Beklemede

### P2.3 Klasör içi swipe (folderCarouselEnabled) varsayılan açık
**Dosyalar:** FolderScreen.kt, SettingsScreen.kt  
**Kanıt Gereksinimi:**
- [ ] FolderScreen yatay drag varsayılan açık
- [ ] SettingsScreen toggle ile kapatılabilir
- [ ] Klasör içi swipe hızı/eşik ana pager ile aynı
- [ ] Compile ✅ + testDebugUnitTest ✅
**Durum:** ⏳ Beklemede

### P2.4 Dock beşinci slot karar + entegrasyon
**Dosyalar:** DockArea.kt, DefaultDockPopulator.kt  
**Sorun:** 5. slot random otomatik seçiliyor → güven sorunu.  
**Tercihim:** İlk 4 sabit (Tel, Mesaj, Kamera, Tarayıcı), 5. boş veya Galeri.  
**Kanıt Gereksinimi:**
- [ ] DefaultDockPopulator 5. slot kuralı yazılı açıklama ile kodlanmış
- [ ] Seçim A (boş) veya B (Galeri) ürün kararı uygulanmış
- [ ] Compile ✅ + testDebugUnitTest ✅
- [ ] Commit mesajı: "UX: Fix dock 5th slot — [SEÇIM: boş/Galeri]"
**Durum:** ⏳ **Seçim bekleniyor** (Hüseyin)

### P2.5 Basit filtre seçeneği hızlı göster
**Dosyalar:** AllAppsDrawer.kt  
**Kanıt Gereksinimi:**
- [ ] Filtre tuşunda 3 çoğun seçenek (Sıralama: A-Z/Z-A/En Yeni, Gizli aç/kapat)
- [ ] Compile ✅ + testDebugUnitTest ✅
**Durum:** ⏳ Beklemede

---

## Arşiv & Bağlantılar

| Dosya | Rol | Durum |
|-------|-----|-------|
| **ROADMAP.md** | Faz/ilerleme, bağımlılıklar, kaynaklar | ✅ Aktif |
| **HISTORY.md** | Tamamlanan döngüler + kanıt | ✅ Güncel |
| **LEARNINGS.md** | Tuzaklar, mimari kararlar, SOP | ✅ Referans |
| **CLAUDE.md** | Çalışma kuralları, Agent, Telegram | ✅ Referans |
| **FİKİRLER.md** | Puanlı fikir havuzu | ⏳ Eski |
| **DECISIONS.md** | Ürün karar tarihi | ⏳ Backup |
| **README.md** | Genel tanıtım | ⏳ Eski |
| **NOW.md** | Periyodun snapshot | ⏳ Eski (tarih 2026-07-13) |

**Temizlik yapılmalı:**
- `NOW.md`, `README.md`, `DECISIONS.md` → archive
- Tablet test dosyaları (`TABLET_*.md`) → `docs/testing/tablet/`
- Release guide → `docs/release/`

---

## İstatistik

- **Toplam görev:** 16 (P0: 6, P1: 6, P2: 5)
- **Tamamlanan:** 0
- **Devam eden:** 0
- **Beklemede:** 16
- **Beklenen kararlar:** 2 (Widget A/B/C, Dock 5. slot)

**Hedef:** P0 bitince %70 → P1 bitince %85 → P2 bitince %100 ✨

