# LEARNINGS.md — AppOrganizer Döngü Öğrenme Kaydı

> Claude her anlamlı döngü sonunda günceller. Append-only — eski kayıtlar silinmez, `Promote` alanı güncellenir.
> **Promote eşiği:** 3 tekrar veya ÖNCELİK:YÜKSEK → kural olarak `CLAUDE.md`'ye taşınır.
>
> Döngü-spesifik değişiklik logları buraya değil → `HISTORY.md`. Burası sadece **tekrar eden öğrenme/tuzak** içindir.

---

## Kategoriler
- `build-pattern` — Gradle/Build/CI
- `android-kotlin` — Android API, Kotlin, Jetpack
- `token-optimizasyon` — Token tüketimi keşifleri
- `model-routing` — Model seçimi kararları
- `bug-pattern` — Tekrarlayan hata kalıpları
- `mimari` — Mimari kararlar, refactor
- `araç-kullanımı` — Script/agent kullanım optimizasyonu

---

## 🔼 Promote Edilmiş Kayıtlar (CLAUDE.md'ye Taşınanlar)

Aşağıdaki öğrenmeler 3+ tekrar veya YÜKSEK öncelik ile CLAUDE.md "KRİTİK Mimari Tuzaklar" (Bölüm 5) bölümüne kural olarak taşındı:

| # | Öğrenme | CLAUDE.md Konumu | Tekrar |
|---|---------|------------------|--------|
| P1 | Kotlin smart cast (`by` delegate) | §5 Smart Cast | 5+ (Döngü 12,15,20...) |
| P2 | Bağımlılık uyumluluk matrisi | §5 Bağımlılık Matrisi | 4 (BOM/AGP/Kotlin/Coil) |
| P3 | AppClassifier `mapOf()` duplicate | §5 Duplicate | 8+ (Döngü 34,46,58,73,79,84) |
| P4 | KeywordDatabase duplicate kategori | §5 KeywordDatabase | 2 (Döngü 40,79) |
| P5 | Encoding (curly quote + bozuk UTF-8) | §5 Encoding | 4+ (Döngü 12,69...) |
| P6 | Türkçe `Locale("tr")` | §5 Türkçe Locale | 3 (Döngü 25,84) |
| P7 | Flow `SharingStarted.Eagerly` | §5 Flow Sıcaklığı | 3 (Döngü 15,16,20) |
| P8 | Async ikon `produceState`+LRU | §5 Async İkon | 5+ (Döngü 12,17,22,27,36) |

---

## 📌 Aktif Öğrenmeler (Promote Edilmiş — Detaylı Kayıt)

<!--
ŞABLON:
## [YYYY-MM-DD] [KATEGORİ] [ÖNCELİK: DÜŞÜK|ORTA|YÜKSEK]
**Bağlam:** [1 satır]
**Gözlem:** [max 3 satır]
**Kural:** [1-2 satır]
**Token Etkisi:** [+X tasarruf | yok]
**Tekrar Sayısı:** N
**Promote:** [x] → CLAUDE.md §Y
-->

## [2026-06-12] bug-pattern [ÖNCELİK: YÜKSEK]
**Bağlam:** `by produceState` ile async ikon yükleme, null kontrolü içinde bitmap atama.
**Gözlem:** `by` delegate property `if (x != null)` bloğunda bile smart cast yapamaz — "Smart cast impossible" derleme hatası. Compose'da `produceState` döndüren tüm nullable bitmap'lerde tekrarlandı.
**Kural:** `icon?.let { bmp -> Image(bitmap = bmp) }` kullan, asla `bitmap = icon` yazma.
**Token Etkisi:** yok
**Tekrar Sayısı:** 5+
**Promote:** [x] → CLAUDE.md §5 (P1)

## [2026-06-12] build-pattern [ÖNCELİK: YÜKSEK]
**Bağlam:** Compose BOM, AGP, Kotlin, compileSdk yükseltme denemeleri ardı ardına hata verdi.
**Gözlem:** BOM 2026.05 → Kotlin 2.x çakışması; Coil 3.x → compileSdk 36 (AGP 8.6 max 35); SDK 35 nullable API kırılması. Her seferinde ekstra build döngüsü harcandı.
**Kural:** Yükseltme öncesi uyumluluk matrisini KONTROL ET (CLAUDE.md §5 tablosu). Kotlin 2.x'te kapt yerine KSP.
**Token Etkisi:** dolaylı (gereksiz build döngüleri önlenir)
**Tekrar Sayısı:** 4
**Promote:** [x] → CLAUDE.md §5 (P2)

## [2026-06-14] bug-pattern [ÖNCELİK: YÜKSEK]
**Bağlam:** AppClassifier `exactMatchMap` 479'dan 3116'ya büyütülürken sürekli duplicate paketler eklendi.
**Gözlem:** Kotlin `mapOf()` duplicate key'de hata VERMEZ — sessizce son entry'i kullanır, öncekini düşürür. Döngü 34/46/58/73/79/84'te toplam 350+ duplicate birikmişti. Veri sessizce kayboluyordu.
**Kural:** AppClassifier her değişince `scripts/check_duplicates.py` çalıştır; bulununca `scripts/dedup_classifier.py`. 0 duplicate doğrulamadan commit etme.
**Token Etkisi:** yok (ama veri bütünlüğü kritik)
**Tekrar Sayısı:** 8+
**Promote:** [x] → CLAUDE.md §5 (P3)

## [2026-06-14] bug-pattern [ÖNCELİK: ORTA]
**Bağlam:** KeywordDatabase'de kategori keyword'leri beklenmedik şekilde eksilmişti.
**Gözlem:** `mapOf()` içinde aynı `CAT_x` iki kez tanımlanmış (CAT_TRAVEL/SHOPPING/FINANCE/HEALTH/UTILITIES). Son tanım kazanıyor, ilk (daha kapsamlı) liste Loop 40'tan beri kayıp. AppClassifier duplicate sorununun KeywordDatabase versiyonu.
**Kural:** Her `CAT_x` map içinde tek bir `to` ile tanımlanmalı. Yeni keyword'leri mevcut listeye EKLE, yeni satır açma.
**Token Etkisi:** yok
**Tekrar Sayısı:** 2
**Promote:** [x] → CLAUDE.md §5 (P4)

## [2026-06-12] bug-pattern [ÖNCELİK: YÜKSEK]
**Bağlam:** Edit tool ile büyük string değişiklikleri sonrası Kotlin derleme hataları.
**Gözlem:** Edit tool bazen curly quote (`"` `"`, 0x201c/0x201d) yazıyor → "Expecting an expression" hatası. Ayrıca bozuk UTF-8 sekansları (`C3 A2 E2 82 AC 22`) yorumlarda görüldü.
**Kural:** Büyük Edit sonrası `scripts/fix_encoding.py <dosya>` veya `xxd | grep e280` kontrol. Her zaman UTF-8 kaydet.
**Token Etkisi:** yok
**Tekrar Sayısı:** 4+
**Promote:** [x] → CLAUDE.md §5 (P5)

## [2026-06-13] android-kotlin [ÖNCELİK: ORTA]
**Bağlam:** AllAppsDrawer ve FolderSheet aramasında Türkçe karakterler eşleşmiyordu.
**Gözlem:** Java varsayılan locale `I` harfini İngilizce kuralıyla küçültüyor (`I→i`), Türkçe ise `I→ı`, `İ→i`. Arama "İ" girişinde uygulama bulamıyordu; sıralama da yanlıştı.
**Kural:** Tüm arama/sıralama `lowercase(Locale("tr"))` ile. `contains(ignoreCase=true)` Türkçe'de güvenilmez.
**Token Etkisi:** yok
**Tekrar Sayısı:** 3
**Promote:** [x] → CLAUDE.md §5 (P6)

## [2026-06-13] mimari [ÖNCELİK: YÜKSEK]
**Bağlam:** Launcher'dan başka uygulamaya geçip dönünce kısa "Yükleniyor..." flaşı görülüyordu.
**Gözlem:** `WhileSubscribed(5000)` ile akış 5sn sonra duruyor; dönüşte DB yeniden sorgulanana kadar boş state. Launcher'ın akışı asla durmamalı.
**Kural:** Launcher kök akışları (`folders`/`allApps`/`filteredAllApps`) `SharingStarted.Eagerly`. queryIntentActivities ile tek sorgu (per-package değil) ~5x hız.
**Token Etkisi:** yok
**Tekrar Sayısı:** 3
**Promote:** [x] → CLAUDE.md §5 (P7)

## [2026-06-12] mimari [ÖNCELİK: YÜKSEK]
**Bağlam:** İkonlar main thread'de senkron yükleniyor, scroll takılıyordu.
**Gözlem:** `remember(pkg) { toBitmap() }` main thread'i bloke ediyor. Her drawer açılışında 100+ ikon diskten yeniden yükleniyordu (cache paylaşılmıyordu).
**Kural:** İkonlar `produceState<ImageBitmap?>` + IO thread + ortak `iconCacheInternal` (LRU-200). Cache hit'te `initialValue` ile anında göster. Cache key: `"${pkg}_${px}"` (ikon paketi varsa `+"_${iconPackPkg}"`).
**Token Etkisi:** yok
**Tekrar Sayısı:** 5+
**Promote:** [x] → CLAUDE.md §5 (P8)

## [2026-06-13] araç-kullanımı [ÖNCELİK: YÜKSEK]
**Bağlam:** Remote (bu) ortamda APK build denemeleri başarısız.
**Gözlem:** `dl.google.com` ve `maven.google.com` erişim listesinde yok → AGP indirilemiyor → `assembleDebug` çalışmıyor. Telegram da engelli (`api.telegram.org`).
**Kural:** Remote ortamda kod değişikliği + commit + push yap, ama **build ve Telegram'ı yerel makineye bırak**. Döngü özetinde "Uzak Ortam Notu" ile belirt.
**Token Etkisi:** dolaylı (boşa build denemesi önlenir)
**Tekrar Sayısı:** çok (her remote döngü)
**Promote:** [x] → CLAUDE.md §4 (Remote ortam notu)

## [2026-06-13] mimari [ÖNCELİK: ORTA]
**Bağlam:** Settings'ten dönünce UI değerleri (tema, arka plan, toggle'lar) güncellenmiyordu.
**Gözlem:** `remember {}` ile okunan AppPrefs değerleri tek sefer hesaplanıyor; SharedPrefs değişince Compose haberdar olmuyor. Launcher yeniden başlatmak gerekiyordu.
**Kural:** Settings'ten okunan reaktif değerler için `mutableStateOf` + `DisposableEffect` + `OnSharedPreferenceChangeListener` üçlüsü. Conditional `remember` yasak — değeri dışarı al.
**Token Etkisi:** yok
**Tekrar Sayısı:** 3 (Döngü 26,27,36)
**Promote:** [ ] → Aday (3. tekrar oldu, sonraki incelemede CLAUDE.md'ye)

## [2026-06-13] araç-kullanımı [ÖNCELİK: ORTA]
**Bağlam:** Merge conflict'ler remote ve local döngüler aynı dosyaya (AppClassifier) yazınca sık çıktı.
**Gözlem:** Aynı döngü hem remote hem local çalışınca `exactMatchMap` çakışıyor. Python ile birleştirme (her iki tarafın entry'lerini set'te toplama) güvenli çözüm oldu.
**Kural:** AppClassifier merge conflict'inde manuel düzeltme yerine `scripts/dedup_classifier.py` mantığı — iki tarafı birleştir, set ile dedup, alfabetik sırala.
**Token Etkisi:** yok
**Tekrar Sayısı:** 4+ (Döngü 58,62,69,73,79,84)
**Promote:** [ ] → Aday

---

## 🆕 İzlenen / Henüz Promote Olmayan Gözlemler
_(3 tekrara ulaşınca yukarı taşınır)_

_(Yeni döngüler buraya gözlem ekler)_

---

*Son güncelleme: 2026-06-15 — LEARNINGS.md v2: 76 döngülük geçmişten 11 tekrar eden öğrenme çıkarılıp promote edildi. 8'i CLAUDE.md §5'e taşındı.*
