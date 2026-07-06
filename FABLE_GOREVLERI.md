# Fable Görev Listesi — Döngü 201: ROADMAP Kritik + UX Spec + Yıldızlı Fikirler

> Oluşturulma: 2026-07-06 · Kaynak: ROADMAP.md (U1-U10, B1-B5) + docs/UX_SEARCH_REPORTS_SPEC.md + FİKİRLER.md ⭐ "Bekliyor" maddeleri
> **KURAL: Yeni özellik EKLEME.** Sadece listedeki sorunları çöz. Her madde ayrı ayrı incele.
> Build ALMA — Fable kod düzeltmesi yapar, build'i Sonnet (ana model) en sonda ayrı yapar.
> Türkçe yorum/commit mesajı kullan. Her tamamlanan maddeyi bu dosyada [x] işaretle VE HISTORY.md'ye taşı
> (kısa döngü özeti olarak), ROADMAP.md/FİKİRLER.md'den ilgili satırı sil (proje kuralı: tamamlanan görev
> ROADMAP'tan silinir → HISTORY.md'ye geçer).
> **ÖNEMLİ:** Bazı maddeler zaten kodda çözülmüş olabilir (dokümanlar bayat olabilir) — önce koddan doğrula,
> zaten çözülmüşse "zaten mevcut" notuyla işaretle, gereksiz tekrar kod yazma.
>
> **D201 SONUÇ:** Tüm maddeler işlendi. Detay → HISTORY.md "Döngü 201".

---

## A. ROADMAP.md — 🔴 Kritik UX (U1-U10)

- [x] U1 — KISMEN MEVCUT: D199 SettingsExpandableCard (SettingsComponents.kt:50) + bölüm başlıkları
      (Görünüm/Launcher/Bildirim/Güvenlik/Geri Bildirim) mevcut; Arama zaten ayrı ekran. Tam alt-ekran
      mimarisi (her kategori ayrı sayfa) büyük refactor — build alınamayan bu döngüde riskli, ROADMAP'ta
      güncellenmiş notla bırakıldı.
- [x] U2 — ZATEN MEVCUT: Kişi/dosya varsayılan kapalı (AppPrefs), SearchSettingsScreen kartlı/bölümlü
      görsel yapıda (Davranış/Kaynaklar/Sonuç Sırası/Gelişmiş/Görünüm/Limitler). D201: Geçmişi Temizle +
      indeks durumu göstergesi eklendi (spec Risk 4/10).
- [x] U3 — ZATEN MEVCUT: FilesIndexer.indexAll() `withContext(Dispatchers.IO)` + koleksiyon başına
      try/catch (FilesIndexer.kt:37,72-83); toggle açılınca SearchRepository.enableFilesSource →
      FilesIndexWorker.enqueueNow + schedule (WorkManager, SearchRepository.kt:169-172) — ana thread
      bloklanmıyor, worker Result.retry ile hata toleranslı.
- [x] U4 — ZATEN MEVCUT: FolderPager `weight(1f)` ile kalan alanı sabit alıyor (HomeScreen.kt),
      effectivePageSize ekran yüksekliği + aktif özellik sayısına göre adaptif, HomeFavoritesSection
      compactMode (<640dp) öneri/son kullanılan satırlarını gizliyor, PermissionsBanner kapatılabilir.
- [x] U5 — DÜZELTİLDİ: `searchBarPosition` prefs'i okunuyordu ama layout'a uygulanmıyordu.
      HomeScreen.kt: arama çubuğu bölümü `searchBarSection` lambda'sına alındı, TOP=saat altı /
      BOTTOM=Google araması altı (grid'in üstü) konumlandırması eklendi. Bar her iki konumda da grid'in
      ÜSTÜNDE sabit — arama yapınca klasörler kaymaz (spec §5 uyumlu).
- [x] U6 — ZATEN MEVCUT: FolderPager detectDragGesturesAfterLongPress + haptic (HomeScreenFolderPager.kt:124)
      + sürüklenen tile scale(1.08f)+arka plan (ghost, 137-140) + drop target highlight (141-145) +
      diğerleri alpha(0.72f).
- [x] U7 — KISMEN: GlassCard ortak bileşeni + StatChip/InsightChip tutarlı stil (12dp corner,
      white-alpha arka plan) zaten mevcut; D201 Risk 7 boşluk düzeltmesi uygulandı. Kapsamlı redesign
      U10'a bağlı — ROADMAP'ta notla bırakıldı.
- [x] U8 — ZATEN MEVCUT: Routes.SEARCH_SETTINGS + Settings→SearchSettings navigasyonu
      (AppNavigation.kt:64,90-92; SettingsScreen onNavigateToSearchSettings → SettingsHomeScreenSection).
- [x] U9 — ZATEN MEVCUT: AppListScreen FAB işlevli — "Sınıflandır" (classifyUnclassifiedApps), yalnızca
      sınıflandırılmamış uygulama varken görünür (AppListScreen.kt:114-138).
- [x] U10 — KAPSAM DIŞI bırakıldı (görev tanımı gereği) — ROADMAP'ta not olarak duruyor, silinmedi.

## B. ROADMAP.md — 🔴 Kritik Build & Ortam (B1-B5)

- [x] B1 — ZATEN MEVCUT: `org.gradle.vfs.watch=false` gradle.properties:6'da.
- [x] B2 — İNCELENDİ, İŞLEM GEREKMEZ: res klasöründe hiç PNG yok (0 adet), en büyük kaynak 16KB
      strings.xml — WebP dönüşümü/duplicate drawable temizliği anlamsız, spekülatif optimizasyon yapılmadı.
- [x] B3 — ZATEN MEVCUT: `kotlin.build.report.output=file` gradle.properties:36'da.
- [x] B4 — KAPSAM DIŞI (görev tanımı gereği): git config değişikliği güvenlik kuralına aykırı —
      ROADMAP'tan kaldırıldı, HISTORY.md'ye "kullanıcı manuel yapmalı" notu düşüldü.
- [x] B5 — ZATEN DENENMİŞ: gradle.properties:15-16 — "Configuration Cache (KAPT + Hilt uyumsuz — KAPALI)"
      notuyla bilinçli devre dışı. Yeniden deneme build gerektirir, bu döngüde build yasak.

## C. docs/UX_SEARCH_REPORTS_SPEC.md — Riskler ve Kabul Kriterleri

- [x] Risk 1 — ZATEN MEVCUT: StatChip'ler clickable (Material ripple) + Dashboard/Kullanım chip'lerinde
      ">" değeri (HomeScreenOverlays.kt:42-65).
- [x] Risk 2 — ZATEN MEVCUT: HomeAppSearchBar long-press → drag + "↑ Üst"/"↓ Alt" ghost zone'ları +
      scale(1.04f) + snap kaydı (HomeScreenComponents.kt:673-748,814-835). D201: kaydedilen konum artık
      layout'a da uygulanıyor (U5 fix'i ile kabul kriteri 2 tamamlandı).
- [x] Risk 3 — ZATEN MEVCUT: HomeAppSearchBar kişi sonuçları ayraç + "Kişiler" etiketiyle
      (HomeScreenComponents.kt:911-927); AllAppsDrawer 4 kaynak grubu başlığı (Uygulamalar/Kategoriler/
      Kisiler/Dosyalar — AllAppsDrawer.kt:418-498).
- [x] Risk 4 — EKLENDİ: SearchSettingsScreen kaynaklar kartına `sourceOpInFlight` iken
      CircularProgressIndicator + "İndeks oluşturuluyor…" satırı.
- [x] Risk 5 — ZATEN MEVCUT: Kişiler toggle izin yoksa false kalır + ContextualPermissionDialog
      RequestPermission launcher granted→onGranted / denied→onDismiss→toggle prefs'ten geri okunur
      (SearchSettingsScreen.kt:225-251, ContextualPermissionDialog.kt:89-93).
- [x] Risk 6 — EKLENDİ: AppOrganizerDashboardScreen'e "Detaylı Rapor →" TextButton +
      AppNavigation'da Routes.USAGE_REPORT bağlantısı.
- [x] Risk 7 — EKLENDİ: FolderStatsRow alt boşluk 4dp→12dp (HomeScreenOverlays.kt).
- [x] Risk 8 — ZATEN MEVCUT: Dosya toggle → FilesIndexWorker arka planda (WorkManager); D201 Risk 4
      göstergesi ile kullanıcı bilgilendirmesi tamamlandı.
- [x] Risk 9 — ZATEN MEVCUT (tasarım gereği): Konum değişimi yalnızca onDragEnd'de uygulanıyor
      (HomeScreenComponents.kt:723-733); ayrıca ayarlar üzerinden değişim launcher'a dönünce yansır —
      IME açıkken bar oynamaz.
- [x] Risk 10 — EKLENDİ: SearchSettingsScreen "Geçmişi Temizle" butonu (her zaman görünür,
      SearchHistoryPrefs.clear + Toast).
- [x] Kabul kriterleri (bölüm 7): 1✓ (FolderStatsRow 4 kart→APP_LIST/REPORTS_CENTER/DASHBOARD/
      USAGE_REPORT), 2✓ (D201 fix ile tam), 3✓, 4✓, 5✓, 6✓ (Uygulamalar enabled=false), 7✓,
      8✓ (placeholder "Uygulama, kategori ara…" HomeScreenComponents.kt:798), 9✓ (D201 eklendi),
      10✓ (tüm arama toggle'ları SearchSettingsScreen'de).

## D. FİKİRLER.md — ⭐ Yüksek Puanlı "Bekliyor" Maddeleri (Kodda Doğrula, Eksikse Tamamla)

- [x] 19⭐ Onboarding — KOD GERÇEĞİ: OnboardingModels.kt 6 adım (WELCOME→SET_LAUNCHER→THEME_SELECT→
      QUICK_SETTINGS→BROWSER_SELECT→DONE) — radikal kesme YAPILMIŞ. FİKİRLER.md [TAMAMLANDI] yapıldı.
      NOT: CLAUDE.md/LEARNINGS.md hâlâ "17 adım (D173)" diyor — bayat, rapor edildi.
- [x] 16⭐ Tarayıcı Seçimi — KODDA VAR: OnboardingScreen.kt:294-295 RoleManager.ROLE_BROWSER +
      createRequestRoleIntent. FİKİRLER.md [TAMAMLANDI] yapıldı.
- [x] 17⭐ Yerel Arama İndeksi v1 — KODDA VAR: ContactsIndexer.kt, FilesIndexer.kt, FilesIndexWorker.kt,
      SearchSettingsScreen kaynak toggle'ları. FİKİRLER.md [TAMAMLANDI] yapıldı.
- [x] 16⭐ Arama Geçmişi — KODDA VAR: SearchHistoryPrefs.kt (Room yerine prefs tabanlı uygulanmış),
      chip row + temizleme mevcut. FİKİRLER.md [TAMAMLANDI — prefs tabanlı] yapıldı.
- [x] 15⭐ FTS5 Türkçe Arama Testi — KODDA VAR: app/src/test/.../TurkishSearchTest.kt mevcut.
      FİKİRLER.md [TAMAMLANDI] yapıldı.
- [x] 15⭐ Arama Kaynakları Ayar Bölümü — KODDA VAR: SearchSettingsScreen.kt (415 satır).
      FİKİRLER.md [TAMAMLANDI] yapıldı.

## E. Genel Kurallar
- Değişiklik yaparken CLAUDE.md ve LEARNINGS.md'deki mimari kurallara uy (Reaktif AppPrefs pattern'i vb.).
- Her yeni toggle/ayar CLAUDE.md §3 kuralına uysun (varsayılan açık, AppPrefs KEY + SettingsScreen toggle).
- Import/kullanım noktası doğrulaması grep ile yap.
- Commit YAPMA — sadece dosyaları değiştir, kullanıcı/Sonnet commit atacak.
- İşin sonunda: hangi maddelerin gerçekten kod değişikliği gerektirdiğini, hangilerinin zaten mevcut/çelişkili
  doküman olduğunu, hangilerinin kapsam dışı bırakıldığını (U10, B4 gibi) özetleyen kısa Türkçe rapor ver.
