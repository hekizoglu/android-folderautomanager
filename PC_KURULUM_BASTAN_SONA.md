# 🖥️ PC'YE KURULUM - BAŞTAN SONA REHBERI

## İçindekiler
1. Gerekli Programlar
2. GitHub Setup
3. Proje Dosyaları
4. GitHub'a Upload
5. APK Build
6. Telefona Yükleme

---

## ⏱️ TOPLAM ZAMAN: 30 DAKIKA

---

## ADIM 1: Windows/Mac/Linux Kontrolü

Hangi işletim sistemi var?

```
WINDOWS: Başlat → Settings → System → About
MAC: Apple logo → About This Mac
LINUX: Terminal → uname -a
```

---

## ADIM 2: GIT KURULUMU

### Windows'ta

```
1. https://git-scm.com/download/win aç
2. Exe dosyasını indir
3. Çalıştır
4. "Next" "Next" "Finish" diye tıkla
5. Terminal'de kontrol et:
   git --version
```

**Göreceksin:**
```
git version 2.xx.x...
```

### Mac'te

```
1. Terminal aç (Spotlight → Terminal)
2. Şu komutu yapıştır:
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
3. Sonra:
   brew install git
4. Kontrol et:
   git --version
```

### Linux'te

```
1. Terminal aç
2. Şu komutu yapıştır:
   sudo apt-get install git
3. Kontrol et:
   git --version
```

---

## ADIM 3: GITHUB HESABI OLUSTUR (3 DAKİKA)

```
1. https://github.com/signup aç

2. Email gir (gerçek email olsun)
   Örn: hüseyin@armutlu.dev

3. Şifre oluştur (16+ karakter)
   Örn: Armutlu2024!Nabız

4. Username seç (onemli - hatırla)
   Örn: hüseyinarmutlu
   (Sonradan değiştirilebilir ama adım 6'de lazım)

5. Email'i doğrula (mail bak)

6. "Create account" tıkla

7. TAMAMLANDI! ✓
```

---

## ADIM 4: DOSYALARI INDIR (2 DAKIKA)

Bu kurs da verilen outputs klasöründeki dosyaları indir:

```
/mnt/user-data/outputs/

Gerekli dosyalar:
  ✓ Tüm .kt dosyaları (Kotlin)
  ✓ Tüm .xml dosyaları (Config)
  ✓ build.gradle.kts
  ✓ settings.gradle.kts
  ✓ AndroidManifest.xml
  ✓ .gitignore
  ✓ github-actions-build.yml
```

**KOLAY YOLU:**

Çıkış noktasında bir klasör oluştur:

```
C:\Users\Senin\AppOrganizer  (Windows)
/Users/Senin/AppOrganizer    (Mac)
/home/senin/AppOrganizer     (Linux)
```

Tüm dosyaları buraya at.

---

## ADIM 5: DOSYALARI ORGANIZE ET (5 DAKIKA)

İndirdiğin dosyaları şu şekilde organize et:

```
AppOrganizer/
├── .github/
│   └── workflows/
│       └── build.yml          ← github-actions-build.yml'yi buraya koydu
├── .gitignore                 ← .gitignore dosyasını buraya koydu
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/
│       │   │   └── com/armutlu/apporganizer/
│       │   │       ├── domain/
│       │   │       │   ├── models/
│       │   │       │   │   ├── AppInfo.kt
│       │   │       │   │   └── Category.kt
│       │   │       │   └── usecase/
│       │   │       │       └── AppClassifier.kt
│       │   │       ├── data/
│       │   │       │   ├── local/
│       │   │       │   │   ├── AppDatabase.kt
│       │   │       │   │   ├── AppDao.kt
│       │   │       │   │   └── CategoryDao.kt
│       │   │       │   ├── repository/
│       │   │       │   │   └── AppRepository.kt
│       │   │       │   └── remote/
│       │   │       │       └── BackupSyncService.kt
│       │   │       ├── presentation/
│       │   │       │   ├── ui/
│       │   │       │   │   ├── MainActivity.kt
│       │   │       │   │   ├── screens/
│       │   │       │   │   │   ├── AppListScreen.kt
│       │   │       │   │   │   ├── CategoryEditorScreen.kt
│       │   │       │   │   │   └── SettingsScreen.kt
│       │   │       │   │   ├── theme/
│       │   │       │   │   │   ├── Theme.kt
│       │   │       │   │   │   └── Typography.kt
│       │   │       │   ├── viewmodel/
│       │   │       │   │   └── AppListViewModel.kt
│       │   │       │   ├── navigation/
│       │   │       │   │   └── Navigation.kt
│       │   │       │   └── receivers/
│       │   │       │       └── PackageChangeReceiver.kt
│       │   │       └── utils/
│       │   │           ├── PermissionHelper.kt
│       │   │           ├── PackageManagerHelper.kt
│       │   │           └── FolderCreationService.kt
│       │   └── res/
│       │       ├── values/
│       │       │   ├── strings.xml
│       │       │   └── colors.xml
│       │       └── xml/
│       │           ├── backup_rules.xml
│       │           └── data_extraction_rules.xml
│       └── test/
│           └── java/com/armutlu/apporganizer/
│               ├── AppInfoTest.kt
│               ├── CategoryTest.kt
│               ├── AppClassifierTest.kt
│               ├── AppDatabaseTest.kt
│               ├── AppRepositoryTest.kt
│               ├── AppListViewModelTest.kt
│               ├── AppListScreenStateTest.kt
│               ├── PermissionHelperTest.kt
│               └── PackageManagerHelperTest.kt
├── build.gradle.kts
└── settings.gradle.kts
```

**Veya:** Tüm dosyaları tek klasöre at, GitHub Desktop otomatik organize edecek.

---

## ADIM 6: TERMINAL ACIYOR (5 DAKIKA)

### Windows'ta

```
1. Başlat → CMD aç (Ya da PowerShell)
2. Şu komutu yapıştır:
   cd C:\Users\Senin\AppOrganizer
```

### Mac'te

```
1. Cmd+Space → Terminal yaz → Enter
2. Şu komutu yapıştır:
   cd /Users/Senin/AppOrganizer
```

### Linux'te

```
1. Terminal aç (Ctrl+Alt+T)
2. Şu komutu yapıştır:
   cd /home/senin/AppOrganizer
```

---

## ADIM 7: GIT INITIALIZE ET (3 DAKIKA)

Terminal'de şu komutları yapıştır (birbiri ardına):

```bash
# 1. Git initialize et
git init

# 2. Dosyaları stage'e ekle
git add .

# 3. İlk commit
git commit -m "App Organizer v1.0 - Initial commit"

# 4. Main branch'a geç
git branch -M main
```

**ÇIKTI GÖRECEKSIN:**
```
Initialized empty Git repository
...
create mode 100644 ...
...
```

---

## ADIM 8: GITHUB'A BAGLA (2 DAKIKA)

GitHub'da repository oluşturdunuz mu? (Evet diye varsayıyorum)

Terminal'de:

```bash
# GitHub'a remote ekle (USERNAME'ı kendi username'inle değiştir!)
git remote add origin https://github.com/USERNAME/app-organizer.git

# Örn:
# git remote add origin https://github.com/hüseyinarmutlu/app-organizer.git
```

---

## ADIM 9: GITHUB'A PUSH ET (5 DAKIKA)

Terminal'de:

```bash
git push -u origin main
```

**ISIM VE SIFRE ISTEYECEK:**

```
Username for 'https://github.com': USERNAME
Password for 'https://github.com/USERNAME': SIFRE
```

Veya

```
Sorabilir: "Create a personal access token instead"
→ YES de → Browser'da GitHub Settings → Personal tokens
→ "Generate new token (classic)"
→ Checkboxes: "repo", "workflow"
→ Generate token
→ Token'ı kopyala
→ Terminal'e yapıştır
```

**TAMAMLANDI:**
```
Enumerating objects: 39, done.
...
To https://github.com/USERNAME/app-organizer.git
 * [new branch]      main -> main
Branch 'main' is set up to track remote branch 'main' from 'origin'.
```

---

## ADIM 10: GITHUB'DA KONTROL ET (1 DAKIKA)

1. GitHub.com'a git
2. Kendi username'ini tıkla
3. app-organizer repository'sini tıkla
4. Dosyaları göreceksin (tüm kodlar GitHub'da!)

---

## ADIM 11: GITHUB ACTIONS TRIGGER ET (1 DAKIKA)

1. Repo sayfasında → "Actions" tab
2. Sol tarafta "Build and Release APK" workflow'u göreceksin
3. Sağ tarafta "Run workflow" butonuna tıkla
4. Dropdown → "main" seçili
5. Yeşil "Run workflow" butonuna tıkla

---

## ADIM 12: BUILD'I BEKLE (10 DAKIKA)

```
GitHub'da:
→ Actions tab
→ Latest workflow (status göreceksin)
→ Build başladı...
→ 5-10 dakika bekle
→ Status: "Completed ✓"
```

Yapabilecekleri:
- Email al (build complete)
- Diğer şeyler yap
- Ara sıra refresh et

---

## ADIM 13: APK İNDİR (2 DAKIKA)

Build bittiğinde:

1. GitHub → Actions
2. Latest workflow'u tıkla
3. Aşağı kaydır → "Artifacts" section
4. "debug-apk" zip'ini indir

```
debug-apk.zip indirilecek
↓
Unzip et
↓
Klasörde "app-debug.apk" dosyası
```

---

## ADIM 14: TELEFONA YUKLE (5 DAKIKA)

### Seçenek A: Email Gönder (KOLAY)

```
1. app-debug.apk dosyasını bul
2. Sağ click → Send to → Email
3. Kendine gönder
4. Telefonda email aç
5. APK'yı indir
6. Tap → Install
```

### Seçenek B: Google Drive

```
1. Google Drive'a git (drive.google.com)
2. app-debug.apk'yı upload et
3. Share linki al
4. Telefonda linki aç
5. Download et
6. Install et
```

### Seçenek C: Dropbox

```
1. app-debug.apk'yı Dropbox'a upload et
2. Share linki al
3. Telefonda aç
4. Download et
5. Install et
```

### Seçenek D: USB Kablosu

```
1. Telefonu USB kablosu ile PC'ye bağla
2. USB mode: "Transfer Files"
3. app-debug.apk'yı telefona kopyala
4. Telefonda tab et
5. Install et
```

---

## ADIM 15: TELEFONDA TEST ET (3 DAKIKA)

1. Uygulamayı aç
2. İzin iste: "Can access installed apps?" → ALLOW
3. Uygulamalar taranıyor... (biraz beklat)
4. ✨ Floating button görünecek
5. Tıkla → "Organize apps?"
6. Tüm uygulamalar kategorilere taşındı! 🎉

---

## SORUN ÇOZME

### Hata: "git: command not found"

```
Çözüm: Git kurulu değil
→ Tekrar kur: https://git-scm.com
→ Bilgisayarı restart et
```

### Hata: "Permission denied (publickey)"

```
Çözüm: GitHub SSH key'i setup'la
Terminal'de:
  ssh-keygen -t ed25519 -C "email@example.com"
  cat ~/.ssh/id_ed25519.pub
  GitHub → Settings → SSH keys → Add SSH key
```

### Hata: "Repository already exists"

```
Çözüm: 
  rm -rf .git
  git init
  Tekrar dene
```

### APK Build Failed

```
Çözüm:
  1. GitHub Actions → Latest workflow → Logs
  2. Error mesajı oku
  3. Bana gönder → Çözerim
```

### App açılmıyor (Installation blocked)

```
Çözüm:
  Telefonda:
  Settings → Security → Unknown sources → ON
  Tekrar install et
```

---

## HIZLI REFERANS

```bash
# Proje klasörüne git
cd /path/to/AppOrganizer

# Git başlat
git init
git add .
git commit -m "Initial commit"
git branch -M main

# GitHub'a bağla
git remote add origin https://github.com/USERNAME/app-organizer.git

# Push et
git push -u origin main
```

---

## TIMELINE

```
09:00 - Git kurulum (5 min)
09:05 - GitHub hesabı (5 min)
09:10 - Dosyaları organize (5 min)
09:15 - Terminal komutları (5 min)
09:20 - GitHub'a push (3 min)
09:23 - Workflow trigger (1 min)
09:24 - Build başladı... (10 min bekliyoruz)
09:34 - APK indir (2 min)
09:36 - Telefona yükle (5 min)
09:41 - Test başladı! (3 min)
09:44 - TAMAMLANDI! 🎉
```

**TOPLAM: 44 DAKİKA**

---

## TAMAMLANDI!

Başarılar! Eğer sorun olursa:

1. Error screenshot'ını al
2. Error mesajı'nı kopyala
3. Bana gönder
4. 5 dakika içinde çözeriz

İyi şanslar! 🚀
