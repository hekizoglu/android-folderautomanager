# 📱 GitHub Actions APK Build Guide

## Quick Start (5 dakika)

### Step 1: GitHub'da Repository Oluştur

```bash
1. GitHub.com'a git
2. "New repository" butonuna tıkla
3. Repository adı: "app-organizer"
4. Public yapabilirsin (open source)
5. Create repository
```

### Step 2: Proje Upload Et

```bash
# Proje dosyasında (AppOrganizer klasörü içinde)
git init
git add .
git commit -m "Initial commit - App Organizer"
git branch -M main
git remote add origin https://github.com/SENIN_USERNAME/app-organizer.git
git push -u origin main
```

### Step 3: GitHub Actions Trigger

```
GitHub'da → Actions tab
→ Build and Release APK workflow
→ Run workflow
→ Bekle (5-10 dakika)
```

### Step 4: APK İndir

```
GitHub'da → Actions → Latest workflow run
→ Artifacts section
→ download "debug-apk" veya "release-apk"
```

---

## Detaylı Setup

### Eğer GitHub'ı hiç kullanmadıysan:

1. **GitHub Account Oluştur** (Free)
   - https://github.com/signup
   - Email, password, username

2. **Git Install Et** (Bilgisayarında)
   ```bash
   # macOS
   brew install git
   
   # Windows
   # https://git-scm.com/download/win indir ve install et
   
   # Linux
   sudo apt-get install git
   ```

3. **Git Config** (İlk sefer)
   ```bash
   git config --global user.email "senin@email.com"
   git config --global user.name "Senin Adın"
   ```

4. **Repository Oluştur** (GitHub.com'da)
   - New → Repository
   - Name: app-organizer
   - Public seç
   - Create

5. **Proje Upload Et**
   ```bash
   cd /path/to/AppOrganizer
   
   git init
   git add .
   git commit -m "Initial App Organizer commit"
   git branch -M main
   git remote add origin https://github.com/USERNAME/app-organizer.git
   git push -u origin main
   ```

6. **GitHub Actions Çalışacak**
   - Otomatik olarak APK build'leyecek
   - 5-10 dakika sürer
   - Bitince: Actions → Artifacts

---

## APK İndirme & Telefonda Yükleme

### İndir
```
GitHub → Actions tab
→ Latest build
→ "debug-apk" zip'ini indir
→ Unzip et
→ app-debug.apk dosyasını al
```

### Telefona Yükle

#### Seçenek A: Direct Download (KOLAY)

```
1. Telefonda Chrome aç
2. GitHub'da APK download linki aç
3. APK'yı indir
4. Download klasöründen tap → Install
```

#### Seçenek B: ADB ile (Teknisyen)

```bash
# Bilgisayarında:
adb connect 192.168.1.XX  # Telefonun IP
adb install app-debug.apk
```

#### Seçenek C: Email Gönder Kendine

```
1. APK'yı Google Drive'a upload et
2. Kendine share linki gönder
3. Telefonda aç ve download et
4. Install
```

---

## Her Build'ten Sonra

APK otomatik build'lenecek:
- Her code push'unda
- Her pull request'te
- Manual trigger yapabilirsin

**Actions tab → Run workflow → Manually**

---

## Signing (Optional - Play Store için)

Eğer release'lemek istersen:

1. Keystore oluştur:
```bash
keytool -genkey -v -keystore release.keystore \
  -keyalg RSA -keysize 2048 -validity 10000 -alias armutlu
```

2. GitHub Secrets'a ekle:
   - Settings → Secrets
   - SIGNING_KEY: (keystore base64)
   - SIGNING_KEY_ALIAS: armutlu
   - SIGNING_KEY_STORE_PASSWORD: (şifren)
   - SIGNING_KEY_PASSWORD: (şifren)

3. Workflow otomatik sign'layacak

---

## Troubleshooting

### Build failed: JDK not found
→ Workflow'da Java version'ı check et (şu setup'da 17 var, tamam)

### APK not in artifacts
→ Build logs'u kontrol et (Actions tab → job logs)

### Gradle timeout
→ Workflow'u 30 dakika timeout'a ayarladım, sorun olmaz

---

## Speed Tips

- `.gitignore` dosyasını ekle:
```
build/
.gradle/
local.properties
```

- İlk push biraz yavaş (dependencies download)
- Sonrasında hızlı olacak (cache)

---

## Sonuç

```
1. GitHub'a push et
2. Actions otomatik çalışıyor
3. 5-10 dakika
4. APK hazır
5. Telefona yükle
6. Test et! 🎉
```

---

**Sorular?** GitHub Actions logs'unu check et veya ben buraya error output'unu yapıştır!
