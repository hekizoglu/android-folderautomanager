# Docs Backlog Score

> Generated: 2026-07-08 10:47
> Rule: KV + U + BR + EA >= 15 goes to ROADMAP.

Scoring follows the project idea model:

- KV: kullanici veya stratejik deger
- U: aciliyet
- BR: bagimlilik/risk azaltma
- EA: uygulanabilirlik

## High Score

| # | Score | KV | U | BR | EA | Source | Task | Recommendation | Status |
|---|-------|----|---|----|----|--------|------|----------------|--------|
| R1 | **18** | 5 | 5 | 5 | 3 | FIKIRLER.md; HISTORY.md Dongu 214-215 | Play Store Privacy/Data Safety uyum paketi | Privacy policy, Data Safety, Firebase/Crashlytics/Analytics/FCM, DeepSeek, Drive/SAF ve package inventory beyanlari ayni hikayeye cekilsin. Kod tarafi tamamlandi (Dongu 214-215); Play Console Data Safety formu doldurma dis aksiyon olarak kaliyor. | Bekliyor |
| R4 | **16** | 5 | 4 | 4 | 3 | ROADMAP.md | Play Store release imza ve submission kapisi | Release keystore, content rating, QUERY_ALL_PACKAGES declaration ve final AAB temiz committen build akisina baglansin. | Bekliyor |
| R6 | **15** | 4 | 4 | 4 | 3 | ROADMAP.md | Gercek cihaz Play-oncesi QA paketi | Android 14 NotificationListener, screenshot smoke, backup/restore, worker schedule ve blur/API26 tek kanitli pakette kosulsun. | Bekliyor |
| R7 | **15** | 4 | 3 | 4 | 4 | ROADMAP.md; FIKIRLER.md | Akilli Bildirim Analiz Sistemi | NotificationListener + notification_events + NotificationAnalyzer + SmartInsightWorker hatti privacy-first rapor, oneri ve gunluk akilli bildirim sistemine tamamlanmali. Detay ve kabul kriterleri ROADMAP.md'deki Detay bolumunde. | Bekliyor |

## All Scored Items

| # | Score | Source | Task | Recommendation | Status |
|---|-------|--------|------|----------------|--------|
| DSR1 | 18 | docs/time_token_analysis_2026-06-30.md; docs/AI_ORCHESTRATION_PLAN.md | Docs backlog skorlayici donguye eklensin | Her dongude docs raporlari puanlansin; 15+ maddeler ROADMAP'teki otomatik blokta yenilensin. | Tamamlandi |
| R1 | 18 | FIKIRLER.md; HISTORY.md Dongu 214-215 | Play Store Privacy/Data Safety uyum paketi | Privacy policy, Data Safety, Firebase/Crashlytics/Analytics/FCM, DeepSeek, Drive/SAF ve package inventory beyanlari ayni hikayeye cekilsin. Kod tarafi tamamlandi (Dongu 214-215); Play Console Data Safety formu doldurma dis aksiyon olarak kaliyor. | Bekliyor |
| DSR2 | 17 | docs/UX_SEARCH_REPORTS_SPEC.md; docs/search-architecture-report.md | Arama sonuclari kaynak bazinda gruplansin | AllApps/Search UI sonuclari Uygulamalar, Kategoriler, Kisiler, Dosyalar bolumleriyle gostersin. | Tamamlandi |
| DSR4 | 17 | docs/UX_SEARCH_REPORTS_SPEC.md; docs/internal/local_denetim_raporu.md | Permission reddi fallback ve ayar yonlendirme | Kisiler/dosya izin reddinde toggle geri kapansin; kalici redde sistem ayarlari deeplink'i gosterilsin. | Tamamlandi |
| R2 | 17 | FIKIRLER.md; HISTORY.md Dongu 214 | Privacy Policy URL ve GitHub Pages dogrulama | Manifest/store listing URL'i gercek yayin URL'iyle ayni olsun. Dongu 214'te PrivacyPolicyScreen.kt ve store_listing.md'deki 404 veren /docs/ onekli URL duzeltildi, curl ile 200 dogrulandi. | Tamamlandi |
| DSR3 | 16 | docs/UX_SEARCH_REPORTS_SPEC.md; docs/search-architecture-report.md | Uygulama arama kaynagi kilitli kalsin | SearchSettings'te Uygulamalar kaynagi acik ve kapatilamaz olsun; bos/yanlis arama durumlari engellensin. | Tamamlandi |
| DSR5 | 16 | docs/competitor_user_research_2026-06-30.md; docs/store_listing.md | Play Store gorsel ve mesaj QA paketi | Light/dark screenshot seti, privacy-first metin ve QUERY_ALL_PACKAGES aciklamasi tek QA paketinde kontrol edilsin. | Tamamlandi |
| R3 | 16 | FIKIRLER.md; HISTORY.md Dongu 214-215 | Rehber ve bildirim metni privacy policy celiskilerini duzelt | ContactsIndexer ve NotificationListener gercegi policy'deki iddialarla uyumlu hale getirilsin. Dongu 214'te Firebase/kisi rehberi/bildirim metni celiskileri, Dongu 215'te Accessibility Service beyani duzeltildi. | Tamamlandi |
| R4 | 16 | ROADMAP.md | Play Store release imza ve submission kapisi | Release keystore, content rating, QUERY_ALL_PACKAGES declaration ve final AAB temiz committen build akisina baglansin. | Bekliyor |
| DSR6 | 15 | docs/internal/build_benchmark_latest.md; docs/issue_mitigation_research_2026-06-30.md | Build warning debt cleanup | Deprecated/unused compose ve icon uyarilari temizlenip build ciktisi daha okunur hale getirilsin. | Tamamlandi |
| R5 | 15 | FIKIRLER.md; HISTORY.md Dongu 214 | Firebase Analytics veri azaltma ve beyan uyumu | PackageName/category/query_length eventleri Data Safety ve privacy-first mesajiyla uyumlu hale getirilsin veya azaltilsin. Dongu 214'te AppAnalytics.kt'den package_name kaldirildi (appLaunched/categoryReclassified/shortcutUsed). | Tamamlandi |
| R6 | 15 | ROADMAP.md | Gercek cihaz Play-oncesi QA paketi | Android 14 NotificationListener, screenshot smoke, backup/restore, worker schedule ve blur/API26 tek kanitli pakette kosulsun. | Bekliyor |
| R7 | 15 | ROADMAP.md; FIKIRLER.md | Akilli Bildirim Analiz Sistemi | NotificationListener + notification_events + NotificationAnalyzer + SmartInsightWorker hatti privacy-first rapor, oneri ve gunluk akilli bildirim sistemine tamamlanmali. Detay ve kabul kriterleri ROADMAP.md'deki Detay bolumunde. | Bekliyor |
| DSR7 | 14 | docs/time_token_analysis_2026-06-30.md; docs/issue_mitigation_research_2026-06-30.md | Token ve sure telemetry logu | Dongu sonunda yaklasik token/sure notu append eden sade bir log tutulabilsin. | Bekliyor |
| DSR8 | 14 | docs/competitor_user_research_2026-06-30.md | Rakip referans tasarim karar belgesi | Smart Launcher, Niagara ve Kvaesitso referanslari icin uygulanabilir UI karar listesi cikarilsin. | Bekliyor |
| DSR9 | 13 | docs/issue_mitigation_research_2026-06-30.md; docs/internal/build_benchmark_latest.md | Configuration cache guard benchmark | Configuration cache sadece benchmark/CLI profilinde kalsin; kalici ayar icin uyumluluk kaniti istensin. | Bekliyor |

## Notes

- This file is generated by scripts/score_docs_backlog.ps1.
- ROADMAP sync is idempotent between DOCS_SCORE_HIGH_START and DOCS_SCORE_HIGH_END.
- Pre-existing docs may be untracked locally; the score still uses the files present in docs/.
