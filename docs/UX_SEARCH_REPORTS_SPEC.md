# UX_SEARCH_REPORTS_SPEC.md — AppOrganizer Arama & Raporlar UX Spesifikasyonu

> Oluşturulma: 2026-06-30 · Kaynak: Claude UX analizi
> Kapsam: Tıklanabilir istatistik kartları, Premium Search Bar, Arama Kaynakları Ayarları

---

## 1. Özellik Özeti

1. Ana ekrandaki istatistik bandı tıklanabilir → ilgili rapor ekranı açılır
2. Arama çubuğu tekil, premium görünümlü; saat/widget bölgesi ile aynı kaydırma alanında
3. Long-press + sürükle ile arama çubuğu konumu değiştirilir; `AppPrefs`'e kaydedilir
4. Settings > "Arama Kaynakları" bölümü: kaynak toggle'ları + indeks durumu
5. Uygulama araması: varsayılan açık, izin gerekmez
6. Kategori araması: varsayılan açık, izin gerekmez
7. Kişi araması: varsayılan kapalı, `READ_CONTACTS` izni gerektirir; toggle'a basınca izin gerekçesi gösterilir
8. Dosya adı araması: varsayılan kapalı, `MediaStore` + SAF ile çalışır; ek izin gerekmez, `MANAGE_EXTERNAL_STORAGE` kullanılmaz; "Cihaz depolama indeksi" etiketiyle ayrı sunulur

---

## 2. Kullanıcı Akışları

### Raporlar Akışı

```
HomeScreen istatistik bandı
  → [Yönetilen Uygulama] tıkla  → AppListScreen (tümü, filtre: hepsi)
  → [Klasör sayısı] tıkla        → AppListScreen (kategori filtrelenmiş)
  → [Kullanılmayan] tıkla        → UsageReportScreen (30g+ açılmadı bölümü açık)
  → [Haftalık süre] tıkla        → AppOrganizerDashboardScreen (top apps bölümü)
```

Her istatistik kartı tek bir hedef ekrana bağlı — drill-down'da kaybolmak yok.

### Search Bar Akışı

```
HomeScreen boş durum
  → Search bar'a dokun          → AllAppsDrawer tam ekran açılır, klavye hazır
  → Yaz: "wa"                   → Uygulama + kategori eşleşmesi paralel
  → Kişi araması açıksa         → 3. satırda kişi sonuçları (ikon farklı)
  → Sonucu tıkla                → Uygulama başlatılır / kategori açılır / kişi aranır
  → Geri tuşu                   → HomeScreen, search bar odağı kapalı
```

### Arama Ayarları Akışı

```
Settings > Arama Kaynakları
  → Kaynaklar listesi (toggle sırası sabit)
  → Kişiler toggle kapalı → açmaya çalış → izin diyaloğu → izin verilirse açılır
  → Dosya Adları toggle  → "İndeks oluşturulmuyor — açınca arka planda tarama başlar" notu
  → İndeks durumu chip   → "Kişiler: 342 kişi indexlendi · 2 saat önce"
```

---

## 3. Ekran Yapısı

### HomeScreen

```
[Saat / Tarih Widget]
[Search Bar — snap konumuna göre: TOP veya BOTTOM]
[İstatistik Bandı — 4 tıklanabilir kart, ripple efektli]
[Klasör Grid — HorizontalPager]
[Dock]
```

İstatistik bandı maksimum 4 kart gösterir: kart başlığı + büyük değer + alt metin.

### AppOrganizerDashboardScreen (mevcut — `AppOrganizerDashboardScreen.kt`)

- Genel özet sayfası: hero row, top apps, kategori dağılımı, verimlilik
- "Detaylı Rapor →" linki ile `UsageReportScreen`'e bağlı
- Dashboard = genel özet; UsageReport = uygulama bazlı detay (iç içe geçmemeli)

### Search Settings (yeni — `SearchSettingsScreen.kt`)

```
Settings > Arama Kaynakları
  ─ Uygulamalar    [açık, toggle devre dışı — zorunlu kaynak]
  ─ Kategoriler    [açık]
  ─ Kişiler        [kapalı] → READ_CONTACTS izin badge
  ─ Dosya Adları   [kapalı] → MediaStore indeks badge + "arka planda taranacak" notu
  ─ [İndeks durumu özeti bölümü]
```

---

## 4. Tıklanabilir İstatistik Kartları — Bilgi Mimarisi

| Kart | Değer | Tıklama Hedefi | Rota |
|------|-------|----------------|------|
| Yönetilen Uygulama | `N uygulama` | AppListScreen (tümü) | `Routes.APP_LIST` |
| Aktif Klasör | `N klasör` | HomeScreen scroll | — |
| Bu Hafta Kullanım | `Xh Yd` | Dashboard → top apps | `Routes.DASHBOARD` |
| Kullanılmayan | `N uygulama` | UsageReportScreen | `Routes.USAGE_REPORT` |

- Tüm kartlar ripple efektli
- Kart içinde chevron (`>`) ikonu yok — yalnızca değer tıklanabilir
- Bant ile klasör grid arasında min 12dp boşluk (yanlış tetiklemeyi önler)

---

## 5. Search Bar Davranışı

### Varsayılan Konum

Klasör grid'inin hemen üstü, dock'un üzerinde (`BOTTOM` snap). Kullanıcı sürükleyene kadar sabit.

### Sürükle-Bırak

- Long-press (300ms) → drag handle görünür, bar hafifçe `scale(1.04f)` olur
- Snap noktaları: **yalnızca 2** — `TOP` (saat widget'ının altı) veya `BOTTOM` (klasör grid'inin üstü)
- Serbest yerleştirme yok — sadece 2 snap noktası (UX karmaşıklığını önler)
- Bırakma sonrası `AppPrefs.KEY_SEARCH_BAR_POSITION` → `"TOP"` / `"BOTTOM"`
- İlk sürüklemede ghost zone göster: "Üst" ve "Alt" alanlar highlight'lanır

### Boş Durum (Odak Yok)

- Placeholder: `"Uygulama, kategori ara…"`
- Kişi/dosya kaynağı açık olsa bile placeholder'a eklenmez (kalabalık önlenir)

### İzin Gerektiren Kaynak Açıkken

- Sonuç listesinin başında kart olarak permission banner gösterilir (Snackbar değil)
- Kullanıcı bir kez reddederse o oturumda tekrar sorulmaz
- Sonuçlar kaynak bazında gruplandırılır: "Uygulamalar" / "Kişiler" / "Dosyalar"

---

## 6. Riskler / UX Hataları

| # | Risk | Çözüm |
|---|------|-------|
| 1 | İstatistik bandı tıklanabilir olduğu belli değil | Ripple + subtle chevron chip |
| 2 | 2 snap noktası sezgisel değil | İlk sürüklemede ghost zone göster |
| 3 | Kişi sonuçları uygulama sonuçlarıyla karışır | Kaynak etiketiyle grupla |
| 4 | Dosya araması gecikme yaratır | İndeks yoksa "İndeks oluşturuluyor…" chip; gerçek zamanlı arama yapma |
| 5 | Kişi toggle → izin reddedilirse toggle açık kalır | Toggle izin reddedilince otomatik kapansın (race condition) |
| 6 | Dashboard ile UsageReportScreen çakışıyor | Dashboard = özet; UsageReport = detay; Dashboard'dan link |
| 7 | Klasör tıklaması / istatistik kartı yanlış tetiklenir | Bant ile grid arasında 12dp boşluk zorunlu |
| 8 | Dosya aramasında indeks tarama süresi kullanıcıyı bekletir | Toggle açılınca arka planda başlar; UI'da "İndeks oluşturuluyor…" chip göster |
| 9 | Search bar konum değişince IME açıkken layout shift | Konum değişimi yalnızca drag bırakıldıktan sonra, IME açıkken hiçbir şey oynamamalı |
| 10 | Arama geçmişi gizlilik riski | Geçmiş sadece cihazda; Settings > Arama > "Geçmişi Temizle" her zaman görünür |

---

## 7. Kabul Kriterleri

- [ ] Ana ekranda 4 istatistik kartının her biri tıklanabilir, hedef ekran doğru açılır
- [ ] Search bar long-press'te drag handle görünür; bırakınca snap noktasına oturur; `AppPrefs`'e kaydedilir; uygulama yeniden açıldığında aynı konumdadır
- [ ] Kişiler toggle kapalıyken `READ_CONTACTS` izni talep edilmez
- [ ] Kişiler toggle açılmaya çalışılırken sistem izin diyaloğu gösterilir; reddedilirse toggle kapanır
- [ ] Dosya araması toggle'ı açıkken indeks arka planda başlar, UI bloklanmaz
- [ ] "Uygulamalar" toggle devre dışı bırakılamaz (zorunlu kaynak)
- [ ] Arama sonuçları kaynak bazında gruplandırılmış görünür (Uygulamalar / Kişiler / Dosyalar)
- [ ] Search bar odaksızken placeholder `"Uygulama, kategori ara…"` gösterir
- [ ] Dashboard'dan UsageReportScreen'e "Detaylı Rapor →" linki mevcut
- [ ] Tüm yeni arama özellikleri Settings > Arama Kaynakları'ndan kapatılabilir toggle'a sahip

---

## 8. Uygulama Sırası (Öneri)

| Adım | Görev | Bağımlılık |
|------|-------|------------|
| 1 | `HomeScreen` istatistik bandı tıklanabilir hale getirilir | Mevcut istatistik widget'ı |
| 2 | `SearchSettingsScreen.kt` + `AppPrefs` arama kaynak toggle'ları | — |
| 3 | `AllAppsDrawer` arama: kategori gruplama + kaynak bazlı sonuç | SearchSettings prefs |
| 4 | Search bar snap konum (TOP/BOTTOM) + drag handle | HomeScreen layout |
| 5 | Kişi araması: `READ_CONTACTS` izin akışı + sonuç gruplandırma | SearchSettings |
| 6 | Dosya adı araması: `MediaStore` indeks + arka plan WorkManager | SearchSettings |

---

*İlgili dosyalar: `AppOrganizerDashboardScreen.kt` · `UsageReportScreen.kt` · `SearchSettingsScreen.kt` · `AppNavigation.kt` · `AppPrefs.kt` · `AllAppsDrawer.kt` · `HomeScreen.kt`*
