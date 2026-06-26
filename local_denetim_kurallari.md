# Local Denetim Kuralları

> Bu dosya, kod tabanı denetiminde uygulanacak kuralları belirler.
> Tüm kurallar **son kullanıcıya görünen katman** (UI, ViewModel, business logic, data flow) odaklıdır.
> Her kural "maddeler halinde liste" formatında `local_denetim.md`'de bulgulara dönüştürülür.

---

## 1. Kapsam

- **Dosyalar:** `app/src/main/java/**` (tüm Kotlin kaynakları)
- **Hariç:** `test/`, `androidTest/`, üçüncü parti kütüphane kaynakları
- **Odak:** Son kullanıcıya doğrudan görünen mantık hatası, bağlam hatası, akış hatası

---

## 2. Denetim Kategorileri

### 🔴 A. Mantık Hataları (Logic Errors)

| Kural | Açıklama |
|-------|----------|
| A1 | State değişkenleri `remember`/`mutableStateOf` ile yönetiliyor mu? |
| A2 | `remember` key parametreleri eksik veya yanlış mı? |
| A3 | `LaunchedEffect`/`DisposableEffect` race condition riski var mı? |
| A4 | SharedPreferences okuma/yazma tutarlılığı var mı? |
| A5 | Flow/StateFlow collect ediliyor mu, yokluğu `by`/`collectAsState` ile yölneniyor mu? |
| A6 | Null safety eksikliği var mı? (`!!`, unsafe call, null olabilecek referans) |
| A7 | Koşul/break/continue mantığı doğru mu? |
| A8 | Döngü sınırları (index out of bounds) riski var mı? |

### 🟡 B. Bağlam Hataları (Context Errors)

| Kural | Açıklama |
|-------|----------|
| B1 | Aynı veri kaynağı birden fazla yerde okunuyor mu? (redundant I/O) |
| B2 | Composable içinde PackageManager/SharedPreferences direkt çağrısı var mı? (recomposition'da tekrar okur) |
| B3 | Cache anahtarları tutarlı mı? (farklı dosyalarda farklı key formatı) |
| B4 | Locale/bölge ayarları tutarlı mı? (bazı yerlerde `Locale("tr")`, bazılerinde `getDefault()`) |
| B5 | Singleton/global state mutation riski var mı? |

### 🟠 C. Kullanıcı Görünüm Hataları (UI/UX Errors)

| Kural | Açıklama |
|-------|----------|
| C1 | Kullanıcı aksiyonu (tıklama/dokunma/sürükleme) doğru hedefleniyor mu? |
| C2 | Yükleme/boş/hata durumları kullanıcıya doğru gösteriliyor mu? |
| C3 | Toggle/ayar değişikliği anında yansıtılıyor mu? |
| C4 | İkon/ görsel güncellemeleri cache prop ile yölneniyor mu? |
| C5 | accessibility (semantics, contentDescription) eksik var mı? |

### 🔵 D. Performans Hataları

| Kural | Açıklama |
|-------|----------|
| D1 | `LazyColumn`/`LazyRow` için `key` parametresi eksik var mı? |
| D2 | `remember`/`derivedStateOf`/`produceState` kullanımı doğru mu? |
| D3 | Gereksiz rekomposizyon tetikleyen state yapısı var mı? |
| D4 | IO işlemi ana thread'de mi yapılıyor? |

### 🟢 E. Kod Sağlığı

| Kural | Açıklama |
|-------|----------|
| E1 | Kullanılmayan parametre/değişken var mı? |
| E2 | Kırık import/bozuk referans var mı? |
| E3 | Hardcoded değerler (string, boyut, renk) yönetiliyor mu? |
| E4 | Log/hata mesajları anlamlı ve Türkçe mi? |

---

## 3. Öncelik Sistemi

| Öncelik | Kriter |
|---------|--------|
| 🔴 KRİTİK | Kullanıcı verisi kaybı, app crash, yanlış işlem (farklı app açma, yanlış kategori) |
| 🟠 YÜKSEK | UI çalışmaz, toggle etkisiz, arama/sıralama hatalı |
| 🟡 ORTA | Görsel hata, geçici state tutarsızlığı, performans kaybı |
| 🟢 DÜŞÜK | Kod kalitesi, minor UX, log eksikliği |

---

## 4. Rapor Formatı

Her bulgu şu formatta kaydedilir:

```
### [Öncelik] [Kod] — Kısa başlık
**Dosya:** `dosya.kt` (satır x-y)
**Sorun:** Tek cümle ile açıklama
**Etki:** Kullanıcı ne yaşar?
**Öneri:** Tek cümle ile düzeltme önerisi
```

---

## 5. Çalıştırma Sıklığı

- Her **döngü sonunda** (cycle.ps1 çalıştırıldıktan sonra)
- Yeni kod eklendiyen/merge olduysa **anında**
- Manuel tetikleme: `.\scripts\audit.ps1` (planlanan)

---

*Oluşturulma: 2026-06-26 | Denetçi: Otomatik + Kilo*
