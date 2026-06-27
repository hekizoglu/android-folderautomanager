# Android Lint + Statik Analiz Kuralları

> Kaynak: [Android Developers — Lint](https://developer.android.com/studio/write/lint)

## Genel Kurallar

| Kod | Kural | Açıklama |
|-----|-------|----------|
| L1 | Null/empty kontrolü | `?.` güvenli çağrı, `?:` fallback, `checkNotNull()` |
| L2 | API seviyesi | `@RequiresApi`, `Build.VERSION.SDK_INT >=` kontrolleri |
| L3 | Kod tekrarı | DRY prensibi — aynı işlem tekrar tekrar yazılmamalı |
| L4 | Yorum dokümanı | `kdoc` ve açıklayıcı yorum eksikliği |
| L5 | Performans | `getPackageInfo()` tekrar tekrar çağrılıyor mu? |
| L6 | Kullanılmayan import | `import` kalıntıları |
| L7 | Magic number | `56.dp`, `90f` gibi sabitler |
| L8 | Kapsam | Dosya içi sınıf/nesne kapsamı |

---

## Android Lint Özel Kontroller

Detaylı lint sorunları ve önerileri: [Android Studio Lint Dokümanı](https://developer.android.com/studio/write/lint)

1. Kod kalitesi
- Basitleştirilebilir ifadeler (`if/else` → `when`, `?:` operatörü)
- Dizi kopyasını gerçekten yapmak gerekiyor mu? (`.toList()`, `.toMutableList()`)
- `MutableList` parametresi → `List` olarak değiştirilebilir
- Gereksiz dönüşüm ve kopya (`asList → toList`)
- Dizi sabit listesi → `listOf(...)` (değişmez)

2. Kotlin dil özellikleri
- `requireNotNull`, `checkNotNull` kullanımı (null kontrolü zayıf mı?)
- `lateinit` kullanımı — lifecycle sorunu yaratıyor mu?
- `by lazy` vs `lateinit` kararının tutarlığı
- `object` vs `class` — singleton pattern
- `sealed class` kullanılması gereken yerlerde enum kullanılmış mu?
- `inline class` / `@JvmInline` fırsatları kaçırılmış mı?

3. Coroutine yapısı
- `viewModelScope` dışında `launch` kullanımı
- `async` + `await()` gereksiz yerde kullanılmış mı?
- `SupervisorJob` vs `Job` tercihinin tutarlılığı
- `Dispatchers.IO` → CPU-bound işlerde yanlış kullanılmış mı?

4. Flow yapısı
- `StateFlow` yerine `SharedFlow` olmalıydı mu?
- `shareIn`/`stateIn` kullanılmadan `collect` zinciri bağımlı
- `MutableStateFlow` → dışarıya `StateFlow` exposure
- `try/catch` bloğu `flow` içinde acil mi?
- Backpressure yönetimi eksik mi? (`buffer`, `conflate`, `collectLatest`)

5. Hata yönetimi
- `try/catch` geniş çaplı (`Exception`) — özel hatalar yakalanmalı
- `runCatching` — hata logu yok, sessiz başarısızlık
- ` Timber.e(e)` var ama `throw` yok — arayan var mı?
- `null` dönüşü — `Result<T>` dönüşü daha güvenli olabilir mi?

---

*Kural seti: qa/rules/lint-kotlin.md | O: 9/10*
