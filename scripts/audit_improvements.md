# Audit Improvement Tracking

> Her 0 sonuclu dongude 1 yeni tespit yontemi ekle.
> Amac: 40 tur regex ezberi degil, surekli gelisen cok katmanli denetim.

---

## Aktif Tespit Katmanlari

### L1 - Regex Pattern (hizli, her dongu)
| # | Kural | Ne Zaman Eklendi | Sonuc |
|---|-------|-----------------|-------|
| 1 | K1, Y1-Y5, Y7-Y8, O1, D1 | D144-D151 | 40 tur, 0 gercek bug |
| 2 | CE1-CE8 compose-expert | D151 | 40 tur, 0 gercek bug |
| 3 | CE9 AppPrefs cross-ref | D191 | **2 BULGU!** CE7+CE9 |

### L2 - Cross-Reference (3 dongude bir)
| # | Yontem | Ne Zaman Eklendi | Sonuc |
|---|--------|-----------------|-------|
| 1 | AppPrefs remember{} vs DisposableEffect | D191 | CE7(Settings)+CE9(HomeScreen) |

### L3 - Agent-Based Deep Analysis (10 dongude bir)
| # | Yontem | Ne Zaman Eklendi | Sonuc |
|---|--------|-----------------|-------|
| 1 | Compose stability metrics | D191 | metrics yok (build gerekli) |
| 2 | Dependency matrix | D191 | BOM/SDK tutarli |
| 3 | APK size trend | D191 | 24.74 MB OK |
| 4 | Dead code / eski TODO | D191 | temiz |

---

## Bekleyen Tespit Yontemleri (0 sonuc geldikce sirayla ekle)

1. **Null safety audit** — tum !! operatorleri, ?.let zincirleri, platform type nullability
2. **Coroutine scope leak** — viewModelScope.launch {} icinde Dispatchers.Main olmamasi gereken IO islemler
3. **Composable @Stable/@Immutable audit** — unstable class'larin recomposition etkisi
4. **Timber log kalitesi** — Timber.e() exception'siz, Timber.d() production kodda
5. **String resource hardcoding** — Turkce/Ingilizce string'lerin strings.xml'de olup olmadigi
6. ~~**BackHandler tutarliligi** — her ekranda back press dogru handle ediliyor mu~~ **Eklendi D168** (OnboardingScreen.kt stepIndex-- BackHandler)
7. **Permission flow butunlugu** — izin isteyen her ekranin rationale + deny fallback'i var mi
8. **ViewModel scope dogrulama** — AndroidViewModel vs ViewModel secimi, Application leak riski
9. ~~**Room query performansi** — @Query'de SELECT *, LIMIT olmayan sorgular~~ **Eklendi D164** (CS13 — AppDao SELECT * ORDER BY LIMIT yok)
10. **Compose preview derleme** — @Preview fonksiyonlarinin parametresiz olup olmadigi

---

*Olusturma: 2026-06-29 D191 | Her 0 sonuclu dongude guncelle*
