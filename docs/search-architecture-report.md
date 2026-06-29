# Birleşik Arama Mimarisi — Teknik Karar Raporu
**Tarih:** 2026-06-29 | **Proje:** AppOrganizer | **Min SDK:** 26

---

## 1. Nihai Öneri (1 paragraf)

Room FTS5 üzerine kurulu, kaynak-bazlı sanal tablolarla beslenen, incremental indexing yapan, single-query cross-source arama motoru. Her aranabilir varlık (app, kategori, kişi, dosya) ortak `SearchDocument` şemasına normalize edilir; FTS5 `content=` özelliğiyle ana tabloya JOIN yapılmadan doğrudan sıralanabilir sonuç döner. Sorgu tek bir `SELECT ... FROM search_fts WHERE search_fts MATCH :query ORDER BY rank` ile çalışır. Yeni kütüphane yok — mevcut Room 2.6.1 altyapısı yeterli. Contacts ve Files varsayılan kapalı, kullanıcı opt-in yapar.

---

## 2. Neden "Everything" Birebir Kopyalanamaz?

1. **NTFS MFT yok:** Everything, NTFS Master File Table'ı kernel-level okur. Android'de dosya sistemi FUSE/EXT4/F2FS üzerinde, tek bir merkezi metadata tablosu bulunmaz.
2. **İzin modeli farklı:** Everything admin yetkisiyle çalışır. Android'de her veri kaynağı farklı permission gerektirir (READ_CONTACTS, QUERY_ALL_PACKAGES, vs.). Tek yetkiyle her şeye erişilmez.
3. **Scoped Storage:** Android 10+ ile dosya sistemi erişimi kum havuzuna alındı. SAF ve MediaStore dışında toplu dosya taraması yapılamaz.
4. **Batarya ve CPU bütçesi:** Everything her dosya değişimini real-time yakalar. Android'de sürekli ContentObserver + tarama bataryayı bitirir. JobScheduler/WorkManager ile batching zorunlu.
5. **Index bellek modeli:** Everything tüm index'i RAM'de tutar (~100MB/1M dosya). Android'de uygulama başına heap sınırı var; FTS5 disk-tabanlı çalışır, RAM'e sığmayanı swap yapmaz — disk okur.

---

## 3. Önerilen Mimari

### Veri Kaynakları
| Kaynak | Okuyucu | Güncelleme Tetikleyici |
|---|---|---|
| Apps | PackageManager (mevcut) | BroadcastReceiver (mevcut) |
| Categories | Room (mevcut) | Room Flow (mevcut) |
| Contacts | ContactsContract | ContentObserver |
| Files | MediaStore + SAF | ContentObserver (MediaStore) |

### İndeksleme Katmanı
- **SearchIndexer** (tek sınıf, ~200 satır): Her kaynaktan `SearchDocument` üretir, FTS5 tablosuna batch-insert yapar.
- Tüm indeksleme `Dispatchers.IO` üzerinde, tek bir CoroutineWorker içinde çalışır.
- Index güncelleme sıklığı: App/Category → anında, Contacts → her 15 dakikada bir delta, Files → her 30 dakikada bir delta.

### Sorgu Katmanı
- **SearchRepository.search(query: String): Flow<Map<SourceType, List<SearchDocument>>>** — kaynak bazında gruplandırılmış sonuç.
- Tek FTS5 MATCH sorgusu, BOOLEAN modunda; sonuçlar `sourceType` ile gruplandırılır, her grup kendi içinde rank ile sıralanır.
- Ek JOIN yok — `content=` tablosu doğrudan veriyi taşır.

### Sonuç Sıralama Mantığı
- Sonuçlar **kaynak bazında gruplandırılır**: "Uygulamalar" / "Kategoriler" / "Kişiler" / "Dosyalar" — her grup kendi section header'ına sahiptir.
- Grup sıralaması sabittir: App → Category → Contact → File (source priority).
- **Grup içi sıralama:** FTS5 `bm25()` skoru ham puanı verir. Recency bonusu: son 7 günde kullanılan app/contact +0.2. Nihai skor = bm25 + recencyBonus.
- Kaynak önceliği yalnızca gruplar arası sıralamayı belirler; grup içinde sadece metin eşleşme kalitesi + recency çalışır.

---

## 4. Kaynak Bazlı Strateji

### Apps
- **İndekslenen alanlar:** appName, packageName, categoryId → categoryName (JOIN'li).
- **Güncelleme:** Mevcut `PackageChangeReceiver` + `AppDao` Flow'u. Yeni app install → hemen FTS insert. Uninstall → hemen FTS delete.
- **Özel durum:** Türkçe karakter normalizasyonu (İ→i, Ş→s, Ğ→g) FTS5 `unicode61` tokenizer'ında `tokenchars` ile yapılır.

### Categories
- **İndekslenen alanlar:** categoryName, description.
- **Güncelleme:** Room Flow ile anlık. Kullanıcı rename/değiştirdiğinde FTS update.
- **Özel durum:** Kategori sonuçları tıklandığında o kategorideki app'lere filtreleme yapar.

### Contacts
- **Tercih:** `ContactsContract` + `READ_CONTACTS` izni.
- **İndekslenen alanlar:** DISPLAY_NAME, PHONE_NUMBER (sadece sayısal normalize), STARRED, LAST_TIME_CONTACTED.
- **Güncelleme:** `ContentObserver` ile `ContactsContract.Contacts.CONTENT_URI` dinlenir. Delta: sadece `CONTACT_LAST_UPDATED_TIMESTAMP > lastSyncTime` olanlar.
- **İzin:** Kullanıcı Settings'ten "Kişilerde ara" toggle'ını açar → `READ_CONTACTS` runtime permission istenir. Varsayılan KAPALI.
- **Güvenlik:** Telefon numaraları indekslenir ama UI'da gösterilmez, sadece isim eşleşmesi döner.

### Files
- **Tercih:** `MediaStore` (INDEXABLE_FOLDERS kapsamındaki dosyalar) + SAF ile kullanıcı seçimi.
- **MANAGE_EXTERNAL_STORAGE: KULLANILMAYACAK.** Gerekçe: Play Store'da "All files access" beyanı launcher uygulaması için reddedilme riski taşır; üstelik bu izin kullanıcıyı korkutur. Launcher'ın dosya araması "nice-to-have" — core function değil.
- **İndekslenen alanlar:** DISPLAY_NAME (MediaStore), MIME_TYPE, DATE_MODIFIED, SIZE.
- **Kapsam:** Sadece MediaStore'da indexed olan dosyalar (Downloads, DCIM, Pictures, Music, Documents). SAF ile kullanıcı ek klasör seçerse onlar da taranır.
- **Güncelleme:** MediaStore ContentObserver + WorkManager periodic (30 dk).
- **Varsayılan:** KAPALI. Kullanıcı Settings'ten "Dosyalarda ara" toggle'ını açar.

---

## 5. Incremental Indexing Stratejisi

### İlk Bootstrap
- Uygulama ilk açıldığında `SearchBootstrapWorker` (OneTimeWorkRequest + expedited) çalışır.
- Sırayla: apps (mevcut Room'dan ~1ms/döküman) → categories (mevcut Room'dan) → contacts (izin varsa) → files (izin varsa, MediaStore query).
- Sistem notification'ı gösterilmez (arka planda sessiz). UI'da "İndeks oluşturuluyor…" inline chip ile durum belirtilir.

### Delta Update
- **Apps:** Anlık. BroadcastReceiver zaten mevcut, üzerine FTS insert/delete eklenecek.
- **Categories:** Anlık. Room Flow koleksiyon farkı (diff) ile.
- **Contacts:** `SearchDeltaWorker` (periodic, 15 dk). `CONTACT_LAST_UPDATED_TIMESTAMP` filtresiyle delta alınır. Update sayısı < 50 ise tekil update, > 50 ise full reindex.
- **Files:** `SearchDeltaWorker` (periodic, 30 dk). MediaStore `DATE_MODIFIED` filtresiyle delta.

### Observer / Worker Mantığı
- `SearchObserverManager`: ContentObserver'ları register/unregister eden singleton. LauncherActivity onStart'ta register, onStop'ta unregister.
- Observer callback'leri direkt FTS yazmaz — `Channel<SearchEvent>` üzerinden debounce (500ms) ile `SearchIndexer`'a iletilir. Bu sayede hızlı ardışık değişiklikler tek batch'te işlenir.
- WorkManager periodic worker'lar `ExistingWorkPolicy.KEEP` ile schedule edilir, duplikasyon önlenir.

---

## 6. Veri Modeli

### SearchDocument (minimum alanlar)
```
sourceType: String       // "app" | "category" | "contact" | "file"
sourceId: String         // packageName / categoryId / contactId / fileUri
title: String            // görünen isim (appName, DISPLAY_NAME, vs.)
subtitle: String?        // paket adı / telefon / dosya yolu (opsiyonel)
searchText: String       // FTS5'in indekslediği birleşik metin (title + subtitle + keywords)
keywords: String?        // ek arama terimleri (kategori adı, MIME type, vs.)
iconKey: String          // icon lookup key (packageName / "category:{id}" / "contact:{uri}" / mime icon)
sourceGroup: String      // grup sıralaması için: "app" > "category" > "contact" > "file"
lastModified: Long       // epoch millis, recency bonus için
```

FTS5 sanal tablosu sadece `searchText` ve `keywords` sütunlarını indeksler. Diğer alanlar `content=` ile bağlı olduğu gölge tablodan okunur — JOIN yok, tek satır.

---

## 7. Ranking Kuralları

| Eşleşme Tipi | FTS5'te Karşılığı | Grup İçi Ağırlık |
|---|---|---|
| Exact match | `"query"` (phrase) | bm25 × 1.5 |
| Prefix match | `query*` | bm25 × 1.2 |
| Contains | `query` (default token) | bm25 × 1.0 |
| Fuzzy | `query*` + edit distance ≤ 2 (post-filter) | bm25 × 0.6 |
| Source priority | Grup sıralaması (gruplar arası) | App → Category → Contact → File |

Sorgu stratejisi: Önce exact phrase dene (hızlı), boşsa prefix, o da yetmezse fuzzy fallback. Fuzzy, FTS5 tarafından yapılmaz; prefix sonuçları üzerinde Kotlin'de `editDistance(query, title) <= 2` post-filter uygulanır.

---

## 8. Riskler

| Risk | Şiddet | Azaltma |
|---|---|---|
| **READ_CONTACTS izin reddi** | Düşük | Contacts varsayılan kapalı; izin isteği bağlamsal (sadece toggle açılınca) |
| **Stale index** | Orta | Observer + periodic worker ikilisi; app her foreground'da `lastModified` kontrolü |
| **Play Policy reddi** | Düşük | QUERY_ALL_PACKAGES zaten onaylı (launcher). MANAGE_EXTERNAL_STORAGE kullanılmıyor. Contacts için READ_CONTACTS declaration + runtime prompt yeterli |
| **FTS5 index boyutu** | Düşük | ~500 app + ~50 kategori + ~1000 contact + ~2000 dosya ≈ < 2MB index. Sorun değil |
| **Mediastore query yavaşlığı** | Orta | Sadece INDEXABLE_FOLDERS scope; ilk bootstrap max 5 saniye; delta query'ler `LIMIT 100` ile |
| **Türkçe karakter** | Düşük | FTS5 `unicode61` tokenizer + `tokenchars` parametresiyle İ/Ş/Ğ/Ü/Ö/Ç normalizasyonu |

---

## 9. MVP Sıralaması

### Sprint 1 (App + Category Search)
- FTS5 tablosu ve `SearchDocument` entity'si oluştur.
- `SearchIndexer` yaz, App ve Category kaynaklarını indeksle.
- `SearchRepository.search()` implementasyonu.
- Mevcut `AllAppsDrawer` arama kutusunu yeni FTS sorgusuna bağla, eski LIKE sorgusunu kaldır.
- Sonuç: App ve kategori araması FTS5 üzerinden, anlık, sıralı çalışır.

### Sprint 2 (Contacts)
- `READ_CONTACTS` runtime permission akışı (Settings toggle → rationale dialog → system prompt).
- Contacts indexer: ilk bootstrap + ContentObserver delta.
- Contacts sonuçlarını arama UI'ına ekle (Contact list item composable, tıklama → dialer intent).
- Sonuç: Kişi araması opt-in çalışır.

### Sprint 3 (Files)
- MediaStore indexer: bootstrap + periodic delta.
- SAF "klasör ekle" akışı (kullanıcı Settings'ten ek klasör seçer).
- Dosya sonuçlarını arama UI'ına ekle (File list item composable, tıklama → VIEW intent).
- Sonuç: Dosya araması opt-in çalışır.

---

## Zorunlu Kararlar (Özet)

| Karar | Seçim | Gerekçe |
|---|---|---|
| **Room FTS vs AppSearch** | **Room FTS5** | Mevcut bağımlılık, sıfır ek kütüphane, tokenizer kontrolü, minSDK 26 yeterli |
| **Contacts yaklaşımı** | **ContactsContract + ContentObserver** | Standart API, runtime permission, Play-safe |
| **Files yaklaşımı** | **MediaStore + SAF** | Scoped Storage uyumlu, izinsiz çalışır |
| **MANAGE_EXTERNAL_STORAGE** | **Kullanılmayacak** | Play Store red riski, launcher için overkill |
| **Varsayılan açık kaynaklar** | **Apps + Categories** | Apps zorunlu (toggle devre dışı); Categories toggle açık ama kapatılabilir |
| **Varsayılan kapalı kaynaklar** | **Contacts + Files** | Privacy-first, opt-in model |
