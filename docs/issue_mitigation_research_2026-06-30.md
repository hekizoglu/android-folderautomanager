# AppOrganizer Sorun Cozum Arastirmasi

Tarih: 2026-06-30

Temel giris:

- `harcananvakit.md`
- `docs/time_token_analysis_2026-06-30.md`

Bu rapor, zaman ve token israfina neden olan ana darbozazlar icin resmi kaynaklara dayali cozum arastirmasidir.

## Hedef Sorunlar

1. Windows tarafinda Gradle ve build dizini lock sorunlari
2. `merged_res` ve resource merge kaynakli tekrarli build maliyeti
3. KAPT, local unit test ve non-ASCII path sorunlari
4. Git `non-fast-forward` tekrar maliyeti
5. Uzun KOD, ARASTIRMA ve DOKUMAN turlarinin token maliyeti

## 1. Windows Gradle Lock Sorunlari

### Bulgular

Android Developers dokumani, Windows tarafinda antivirus taramasinin build sirasinda uretilen cok sayida dosya nedeniyle derlemeyi ciddi yavaslatabilecegini acikca belirtiyor. Google; proje dizini, Gradle cache, Android SDK ve Android Studio sistem dizinleri icin exclusion oneriyor.

Gradle dokumani, file system watching ozelliginin acik oldugunu; gerekirse `--no-watch-fs` veya `org.gradle.vfs.watch=false` ile kapatilabilecegini soyluyor. Gradle TestKit dokumani da Windows'ta watcher kaynakli lock davranislarina dikkat cekiyor.

### Uygulanabilir Cozumler

#### A. Kalici exclusion politikasi

Ilk ve en dusuk riskli adim:

- repo klasoru
- `%USERPROFILE%\.gradle`
- Android SDK dizini
- Android Studio system ve cache dizinleri

icin Defender exclusion tanimlamak.

Mumkunse su process tabanli exclusionlar da degerlendirilebilir:

- `java.exe`
- `gradle`
- `kotlinc`

#### B. ASCII-only calisma klasoru

Google, gerekirse bu dizinleri exclude edilmis ayri bir klasore tasimayi da oneriyor. Bu proje icin pratik yorum:

- repo'yu ASCII-only bir path altina tasimak
- veya sabit bir junction kullanmak
- `GRADLE_USER_HOME` icin de ASCII-only konum kullanmak

Ornek:

- `C:\WorkFolder\AndroidStudioProjects\android-folderautomanager`
- `C:\WorkFolder\.gradle`

Bu adim hem antivirus hem non-ASCII path riskini birlikte azaltir.

#### C. `--no-watch-fs` A/B deneyi

Semptomunuz dosya ve dizin silerken lock olmasi oldugu icin, Windows local dev tarafinda su test mantikli:

- normal build
- `--no-watch-fs` ile build

Eger lock belirgin azalir ve toplam sure bozulmazsa, sadece local Windows gelistirme icin:

- `org.gradle.vfs.watch=false`

opsiyonel profil olarak tutulabilir.

#### D. Olcmeden ayar degistirmeme

Android tarafi Build Analyzer, `--profile` ve build profiling kullanmayi oneriyor. Yani artik hisse dayali degil, olcum tabanli ilerlemek lazim.

## 2. `merged_res` ve Resource Merge Maliyeti

### Bulgular

Android build profiling dokumani, `merge...Resources` task'i pahaliysa su alanlara bakilmasini oneriyor:

- agir PNG ve drawable varliklari
- WebP donusumu
- gereksiz duplicate kaynaklar

Android build speed rehberi de su ayarlari vurguluyor:

- non-transitive R classes
- non-constant R classes
- Jetifier'i kapatma
- configuration cache

### Uygulanabilir Cozumler

#### A. `mergeDebugResources` icin profil cikarma

Her yavaslamada direkt clean yapmak yerine once:

- `gradlew --profile --offline --rerun-tasks assembleDebug`

veya Android Studio Build Analyzer ile:

- resource merge ne kadar suruyor
- hangi task agir
- hangi resource tipi maliyetli

olculmeli.

#### B. Gorsel varliklari hafifletme

Eger agir PNG veya fazla varyant varsa:

- uygun olanlari WebP'ye cevir
- duplicate drawable ve qualifier kaynaklarini temizle
- gereksiz varyantlari azalt

#### C. Jetifier'i kaldirma deneyi

Android docs'a gore:

- `android.enableJetifier=false`

performans kazanci saglayabilir. Ancak bunu once uyumluluk ve Build Analyzer ile test etmek gerekir.

#### D. Configuration cache A/B

Android docs, configuration cache'in Build Analyzer ile kontrol edilmesini oneriyor. Denenebilecek ayarlar:

- `org.gradle.configuration-cache=true`
- `org.gradle.configuration-cache.problems=warn`

Ama Hilt, KAPT ve custom script etkisi mutlaka benchmark ile gorulmeli.

## 3. KAPT, Kotlin, Unit Test ve Non-ASCII Path

### Bulgular

Kotlin kapt docs, compile avoidance ve incremental davranis icin annotation processor classpath yonetimine dikkat cekiyor. Kotlin K2 migration rehberi ise Kotlin 2.x ile derleme performansinda anlamli iyilesme potansiyeli oldugunu belirtiyor.

Bu projedeki ozel ve onemli bulgu:

- non-ASCII path altinda unit test classpath ve test loading sorunlari gercek
- ASCII mirror ve temp classpath workaround bu nedenle mantikli

### Uygulanabilir Cozumler

#### A. ASCII path workaround'ini kalici politika yap

Bugun icin en guvenli kural:

- repo clone path ASCII-only olmali
- test mirror path ASCII-only olmali
- `GRADLE_USER_HOME` ASCII-only olmali

Bu yaklasim hata olasiligini ciddi azaltir.

#### B. Kotlin build reports'i ac

Kotlin dokumanina gore:

- `kotlin.build.report.output=file`

ile compile fazlari olculebilir. Boylece:

- analysis mi pahali
- init mi pahali
- incremental gercekten calisiyor mu

net gorulur.

#### C. K2, Kotlin ve AGP'yi stabil guncelde tut

K2 migration rehberi, analysis ve incremental asamalarinda hizlanma potansiyeli oldugunu belirtiyor. Bu nedenle:

- Kotlin
- AGP
- Gradle

ucgeni guncel ve stabil kombinasyonda tutulmali.

#### D. KAPT borcunu izle, sonra azalt

Hemen migration zorunlu degil. Dogru sira:

1. build report ve profile ile pahali task'lari olc
2. KAPT gercekten ana darbogazsa tespit et
3. sadece uygun kutuphanelerde daha modern yola gecisi degerlendir

## 4. Git `non-fast-forward` Kaybi

### Bulgular

Git dokumani, branch ayrismasi durumunda `git pull --rebase` veya `pull.rebase` config kullanilmasini acikca anlatiyor.

### Uygulanabilir Cozumler

#### A. Varsayilan yap

Makine bazli:

```powershell
git config --global pull.rebase true
```

#### B. Push oncesi sabit akis

Her push oncesi:

```powershell
git fetch origin
git rebase origin/main
git push origin main
```

Bu, tekrar eden 2-3 dakikalik kaybi kucultur.

## 5. Token Israfini Azaltmak Icin Surec Cozumleri

### Bulgular

OpenAI resmi dokumanlarinda su basliklar acikca yer aliyor:

- prompt caching
- token counting
- background ve batch mantigi
- cost optimization

Bu, su sonucu destekliyor:

- buyuk tekrarli baglam bloklari sabitlenmeli
- token kullanimi olculmeli
- buyuk analiz isleri parcalanmis akista yurutmeli

### Uygulanabilir Cozumler

#### A. Uzun refactor'lari alt paketlere bol

En buyuk token yiyiciler uzun tek parca oturumlar. Daha iyi model:

- bir oturum = tek feature
- bir oturum = tek risk alani
- dokuman sync = sonda tek tur

#### B. Dokuman yazimini toplu sona birak

Her adimda `HISTORY`, `ROADMAP` ve benzeri dosyalari guncellemek yerine:

- oturum sonunda tek toplu sync

token ve dikkat maliyetini azaltir.

#### C. Arastirmayi once topla, sonra tek sentez yap

Forum ve haber taramalarinda:

- once kisa not toplama
- sonra tek sentez turu

yapilmali. Ayni kaynaga tekrar tekrar donmek maliyetlidir.

#### D. Tekrarlayan prompt omurgasini sabitle

Eger benzer agent akislari cok kullaniyorsan:

- sabit checklist
- sabit audit rubric
- sabit cikti iskeleti

korunursa caching benzeri avantajlardan daha fazla yararlanilir.

#### E. Ucuz model ve guclu model ayrimi

En iyi pratik:

- tarama, listeleme, ilk eleme = ucuz model
- son karar, patch, sentez = daha guclu model

Ozellikle cok yorumlu arastirma islerinde bu fark anlamli olur.

#### F. Sayisal token takibi ekle

Zaman logu var ama token logu yok. Bir sonraki iyilestirme:

- buyuk oturumlarda tahmini token notu
- veya tool ve agent bazli cost kaydi

Olcmedigin seyi optimize etmek zordur.

## Onerilen Eylem Sirasi

En yuksek ROI sirasi:

1. Repo, Gradle ve SDK icin kalici Defender exclusion ve gerekirse ASCII path standardi
2. Windows local dev icin `--no-watch-fs` A/B testi
3. Build Analyzer, `--profile` ve benzeri olcumlerle 3 senaryo benchmark'i
4. Kotlin build reports'i acmak
5. Jetifier ve configuration cache uyumlulugunu kontrollu denemek
6. `pull.rebase=true` standardi
7. Token logu eklemek ve buyuk refactor/dokuman turlarini parcali yurutmek

## Dogrudan Uygulanabilir Mini Checklist

### Build ve Ortam

- [ ] Defender exclusion'larini gozden gecir
- [ ] `GRADLE_USER_HOME` ve repo path'i ASCII-only yap
- [ ] `--no-watch-fs` benchmark yap
- [ ] Build Analyzer raporu al
- [ ] `gradlew --profile --offline --rerun-tasks assembleDebug` raporu kaydet

### Kotlin, KAPT ve Test

- [ ] `kotlin.build.report.output=file` ac
- [ ] K2, Kotlin ve AGP kombinasyonunu benchmark et
- [ ] Non-ASCII test classpath workaround'ini koru

### Git

- [ ] `git config --global pull.rebase true`

### Token

- [ ] uzun feature'lari daha kisa oturumlara bol
- [ ] dokuman update'lerini sonda toplu yap
- [ ] token tahmini veya cost log alani ekle

## Kaynaklar

### Android, Gradle, Kotlin ve Microsoft

- Android Studio Windows antivirus guidance:
  https://developer.android.com/studio/intro/studio-config
- Android build speed:
  https://developer.android.com/build/optimize-your-build
- Android Build Analyzer:
  https://developer.android.com/build/build-analyzer
- Android build profiling:
  https://developer.android.com/build/profile-your-build
- Gradle file system watching:
  https://docs.gradle.org/current/userguide/file_system_watching.html
- Gradle build environment:
  https://docs.gradle.org/current/userguide/build_environment.html
- Gradle CLI profiling and watch flags:
  https://docs.gradle.org/current/userguide/command_line_interface.html
- Gradle Windows watcher lock note:
  https://docs.gradle.org/current/javadoc/org/gradle/testkit/runner/GradleRunner.html
- Kotlin kapt docs:
  https://kotlinlang.org/docs/kapt.html
- Kotlin K2 migration guide:
  https://kotlinlang.org/docs/k2-compiler-migration-guide.html
- Microsoft Defender exclusions:
  https://learn.microsoft.com/en-us/defender-endpoint/configure-exclusions-microsoft-defender-antivirus

### Git

- Git pull docs:
  https://git-scm.com/docs/git-pull
- Git rebasing book section:
  https://git-scm.com/book/en/v2/Git-Branching-Rebasing

### OpenAI ve token optimizasyonu

- OpenAI Platform docs:
  https://platform.openai.com/docs
- OpenAI Developers Apps SDK use case planning:
  https://developers.openai.com/apps-sdk/plan/use-case
- OpenAI Developers Apps SDK metadata optimization:
  https://developers.openai.com/apps-sdk/guides/optimize-metadata
