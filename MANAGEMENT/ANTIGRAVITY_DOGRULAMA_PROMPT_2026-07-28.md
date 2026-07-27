# Antigravity — Bağımsız Doğrulama Görevi (2026-07-28)

Bu turda Claude (orkestra şefi) iki değişiklik yaptı ve build/test/commit/push+APK teslim zincirini
tamamladı. Senden istenen: bu değişiklikleri KENDİN koddan okuyarak bağımsız doğrulaman — Claude'un
raporuna güvenme, dosyaları aç ve gerçekten kontrol et. Sonucu
`MANAGEMENT/ANTIGRAVITY_DOGRULAMA_RAPORU_2026-07-28.md` dosyasına yaz.

## 1) Hero Dashboard kart genişliği — Dock ile hizalama

**İddia:** Ana ekrandaki Hero kartları (`HomeMissionCard`, `TodayCard`, Hero Dashboard sayfası) artık
Dock ile aynı genişlik formülünü kullanıyor; önceden Hero Dashboard kendi sabit
`contentMaxWidthDp`/`horizontalPaddingDp` değerlerini (304dp/28dp gibi) kullanıyordu, Dock ise
`HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp()` + `HomeHeroTokens.DockHorizontalPadding` (10dp)
kullanıyordu — iki farklı sistemdi, görsel olarak hizasız duruyordu.

**Değişen dosyalar:**
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/hero/HeroDashboardPage.kt`
  — `contentWidth` hesaplaması artık `HomeHeroTokens.DockHorizontalPadding` ve
  `HomeAdaptiveLayoutPolicy.centeredContentMaxWidthDp(deviceClass)` kullanıyor,
  `spec.horizontalPaddingDp`/`spec.contentMaxWidthDp` genişlik için artık kullanılmıyor (spec'in diğer
  alanları — yükseklik, font boyutu, scroll — hâlâ kullanılıyor).
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeScreen.kt`
  — Dock modifier'ındaki `padding(horizontal = 10.dp, ...)` artık sabit değer yerine
  `HomeHeroTokens.DockHorizontalPadding` referansı kullanıyor (aynı sabit, tek kaynak).
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/HomeMissionCard.kt`
  ve `TodayCard.kt` — `GlassCard`'a eksik `fillMaxWidth()` eklendi (bu bir önceki turda zaten
  commit `2bd1d6c3`'e girmişti, sen de doğrulamıştın — bu turla ilgisi yok, sadece bağlam).

**Doğrulaman gerekenler:**
- `HeroDashboardPage.kt`'de `contentWidth` hesaplamasının gerçekten `HomeAdaptiveLayoutPolicy`'ye
  devredildiğini, eski `spec.contentMaxWidthDp`/`spec.horizontalPaddingDp` referanslarının genişlik
  hesaplamasından çıkarıldığını kontrol et.
- `HomeScreen.kt`'deki Dock padding'inin aynı `HomeHeroTokens.DockHorizontalPadding` sabitini
  kullandığını, iki dosyanın artık aynı genişlik mantığına dayandığını teyit et.
- Telefon (PHONE) ve tablet (EXPANDED_TABLET) sınıflarında genişlik formülünün matematiksel olarak
  tutarlı olduğunu kontrol et — `centeredContentMaxWidthDp()` sadece EXPANDED_TABLET'te 720dp tavan
  uyguluyor, PHONE'da null dönüyor (ekran genişliği - 20dp kullanılmalı).
- Mümkünse emülatörde HomeScreen açıp Hero kartlarının kenarlarının Dock'un kenarlarıyla görsel
  olarak hizalı olduğunu screenshot ile doğrula.

## 2) Klasör açılış ekranı — arka plan karartma güçlendirme

**İddia:** Kullanıcı "klasör açılınca arka plan da blur olsun, arka planı görüyoruz" dedi. Araştırma
sonucu şu bulunmuştu: `FolderScreen.kt`'de gerçek blur hiç yoktu, sadece "wallpaper" arka plan
tipinde %35 alfa siyah karartma vardı (duvar kağıdı belirgin şekilde sızıyordu). `AllAppsDrawer.kt`'de
ise `blur(20.dp)` çağrısı vardı ama BOŞ bir `Box`'ın kendi içeriğini bulanıklaştırıyordu (arkasındaki
gerçek launcher ağacını değil) — yani görsel etkisi sıfırdı, sadece gereksiz GPU maliyeti vardı.

Karar: gerçek blur (API 31+ RenderEffect mimarisi) büyük bir iş olduğu için (zorluk 7-8), kullanıcı
"karartmayı güçlendir" seçeneğini onayladı — hızlı ve düşük riskli çözüm.

**Değişen dosyalar:**
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/FolderScreen.kt`
  — `Color.Black.copy(alpha = 0.35f)` → `Color.Black.copy(alpha = 0.78f)` (sadece `bgType == "wallpaper"`
  koşulunda uygulanıyor).
- `app/src/main/java/com/armutlu/apporganizer/presentation/ui/launcher/AllAppsDrawer.kt`
  — Etkisiz `blur(20.dp)` çağrısı kaldırıldı, `bgAlpha` (kullanıcı ayarından gelen, `AppPrefs.getAllAppsBgAlpha`)
  değeri DOKUNULMADI (bu zaten Ayarlar'dan kullanıcı kontrolünde). `androidx.compose.ui.draw.blur` import'u
  da kaldırıldı (artık kullanılmıyor).

**Doğrulaman gerekenler:**
- `FolderScreen.kt`'de alfa değerinin gerçekten 0.78'e çıktığını, bu değişikliğin SADECE
  `bgType == "wallpaper"` koşulunda etkili olduğunu (solid/gradient arka planları etkilemediğini) teyit et.
- `AllAppsDrawer.kt`'de `blur()` çağrısının silindiğini, ama `bgAlpha` background'unun (kullanıcı ayarı)
  hâlâ yerinde olduğunu doğrula — bu satırın kaldırılmaması gerekiyordu, sadece etkisiz blur() kaldırıldı.
  `pixelLookEnabled == true` durumundaki farklı (düz yüzey) davranışın DOKUNULMADIĞINI kontrol et.
- Kaldırılan `blur` import'unun dosyada başka hiçbir yerde kullanılmadığını grep ile teyit et (derleme
  hatası riski).
- Mümkünse emülatörde bir klasörü "Duvar Kağıdı" arka plan tipiyle aç, önceki/sonraki karşılaştırması
  yap (screenshot), duvar kağıdının artık çok daha az sızdığını doğrula.
- Not: Bu gerçek bir blur DEĞİL, sadece daha koyu bir overlay. Kullanıcı ileride "gerçek blur" isterse
  bu, API 31+ RenderEffect tabanlı ayrı bir iş olarak roadmap'e eklenmeli (bu turda kapsam dışı
  bırakıldı) — raporunda bunu bir not olarak düş, roadmap'e ekleme kararını Claude/Hüseyin verecek.

## Rapor formatı

`MANAGEMENT/ANTIGRAVITY_DOGRULAMA_RAPORU_2026-07-28.md` içine:
- Her iki madde için: doğrulandı / kısmen doğrulandı / sorun bulundu
- Bulduğun her sorun için: dosya + satır + ne yanlış + önerilen düzeltme
- Emülatör testi yapabildiysen ekran görüntüsü yolu / bulgu notu
- Genel sonuç: kritik sorun var mı, yok mu
