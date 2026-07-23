# Release Build Guide — AppOrganizer v1.0.0+

**Amaç:** Production AAB'yi imzalanmış şekilde üretme.  
**Sorumlu:** Build machine (lokalde, CI/CD yoksa)  
**Güvenlik:** Keystore hassas → git-ignored, yedeklenmiş, şifrelenmeli

---

## 1. Release Keystore Oluşturma (İlk Kez)

```bash
# Admin PowerShell / bash
keytool -genkey -v \
  -keystore release.jks \
  -keyalg RSA -keysize 2048 \
  -validity 10000 \
  -alias apporg_release \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=App Organizer, O=Armutlu, L=Istanbul, ST=Istanbul, C=TR"
```

**Çıkış:** `release.jks` (binary, ~2 KB)

### Keystore Backup (Güvenli Sakla)
- [ ] `release.jks` copy → encrypted USB / cloud vault
- [ ] Password record → password manager (1Password, LastPass)
- [ ] Fingerprint log: `keytool -list -v -keystore release.jks`
  ```
  SHA1: XX:XX:XX:...
  SHA256: YY:YY:YY:...
  ```
  **Yukarıdaki SHA256'yi Play Console'de Play App Signing'e kaydet** (keystore integrity doğrulaması)

---

## 2. Gradle Build Config (keystore.properties)

**Oluştur:** `keystore.properties` (proje kökü, git-ignored)

```properties
storeFile=/absolute/path/to/release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=apporg_release
keyPassword=YOUR_KEY_PASSWORD
```

**Git Ignore'a ekle:**
```
# .gitignore
keystore.properties
release.jks
```

**app/build.gradle.kts (signing block):**

```kotlin
signingConfigs {
    release {
        storeFile = file(rootProject.file("keystore.properties").readLines()[0].split("=")[1])
        storePassword = rootProject.file("keystore.properties").readLines()[1].split("=")[1]
        keyAlias = rootProject.file("keystore.properties").readLines()[2].split("=")[1]
        keyPassword = rootProject.file("keystore.properties").readLines()[3].split("=")[1]
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.release
        minifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
}
```

---

## 3. Release AAB Üretme

```bash
cd /path/to/AppOrganizer
./gradlew bundleRelease

# Çıkış: app/build/outputs/bundle/release/app-release.aab (~50–80 MB)
```

**Doğrulama:**
```bash
unzip -l app/build/outputs/bundle/release/app-release.aab | head -20
# splits/, base/, BundleConfig.pb gösterilmeli
```

---

## 4. Play Console Upload

1. **Release Track:** Internal Testing
   - Upload AAB
   - Version: 1.0.0
   - Release notes: "Initial release"
   - Submit for review

2. **Play App Signing (Automatic)**
   - Google otomatik re-sign eder (Play Signing key farklı)
   - Keystore SHA256 fingerprint → Console'de görünür (verification)

3. **Staged Rollout (Safe):**
   - 1% → 10% → 50% → 100% (risk azaltma)
   - Crash logs gözlemle (Crashlytics)

---

## 5. Mağaza Görselleri (Assets)

### Feature Graphic (1024×500, JPEG/PNG)
- **Tasarım:** Turkuaz arka plan + "Smart App Organizer" + icon mockup
- [ ] Tasarımcı hazırla veya Canva template kullan
- [ ] Export: 1024×500, JPEG, sRGB
- **Dosya:** `assets/store/feature_graphic.jpg`

### App Icon (512×512)
- [ ] Icon design (AppOrganizer logo)
- [ ] Transparent background
- [ ] Export: 512×512, PNG-32
- **Dosya:** `assets/store/app_icon.png`

### Screenshots (5–8, Portrait or Landscape)
- [ ] HomeScreen (klasörler, grid)
- [ ] AllAppsDrawer (arama)
- [ ] Settings (tema, kategori)
- [ ] Permissions guide
- [ ] Dashboard/widgets (opsiyonel)
- **Her screenshot:**
  - 1080×1920 (portrait) veya landscape
  - Text: Türkçe + İngilizce subtitle (opsiyonel)
  - PNG veya JPEG
- **Klasör:** `assets/store/screenshots/`

---

## 6. Versioning Strategy

**Semantic Versioning:** MAJOR.MINOR.PATCH

- **Major:** Architecture, UI overhaul (infrequent)
- **Minor:** New features, category additions (2–4 haftalık)
- **Patch:** Bug fixes, hotfixes (need-based)

**Build Numbers:**
- `versionCode`: Sırasal (1, 2, 3, ...) — Play Console sorting
- `versionName`: "1.0.0", "1.0.1", "1.1.0", "2.0.0"

**Güncelleme Prosesi:**
1. `app/build.gradle.kts` versyon artır
2. Release notes güncelle
3. `./gradlew bundleRelease`
4. Play Console upload
5. Staged rollout start

---

## 7. Hotfix (Production Crash)

**Senaryo:** v1.0.0 crash, hızlı fix gerekir

1. `versionCode` +1 (e.g., 2)
2. `versionName` → "1.0.1"
3. Bug fix code
4. Test (internal APK)
5. `bundleRelease`
6. Play Console → "Release hotfix" → 100% rollout

**Rollback:** Önceki versionCode'a dön (Play Console → Manage Releases → Previous)

---

## 8. Security Checklist

- [ ] Keystore password **not in code, not in git**
- [ ] Keystore backup → encrypted storage
- [ ] SHA256 fingerprint → Play Console recorded
- [ ] ProGuard rules enabled (code obfuscation)
- [ ] API keys (Firebase, DeepSeek) → not in APK strings
- [ ] Secrets `.env` file → git-ignored

---

## Production Readiness

```
✅ Signed AAB built
✅ Play Console uploaded
✅ Assets uploaded (icon, feature graphic, screenshots)
✅ Privacy Policy live
✅ Content rating + Data Safety filled
✅ Staged rollout 1% → 100%
✅ Crash logs monitored (Crashlytics)
✅ Production → stable
```
