# SETUP.md — AppOrganizer İlk Kurulum

> Sıfırdan başlarken bu dosyayı takip et. CLAUDE.md daha kapsamlı ama bu dosya "ilk çalıştırma" için yeterli.

---

## 1. Gereksinimler

- Android Studio (Ladybug veya üzeri)
- JDK 17 (Eclipse Adoptium önerilir)
- Python 3.10+
- Git

---

## 2. Repo Klonla

```powershell
git clone https://github.com/hekizoglu/android-folderautomanager.git AppOrganizer
cd AppOrganizer
```

---

## 3. .env Dosyası Oluştur

`.env` dosyasını proje köküne oluştur (git'e commit edilmez):

```
TELEGRAM_BOT_TOKEN=<BotFather'dan al>
TELEGRAM_CHAT_ID=937179261
DEEPSEEK_API_KEY=<platform.deepseek.com'dan al>
```

---

## 4. Git Hooks Aktifleştir

```powershell
git config core.hooksPath .githooks
```

Bu komut pre-commit hook'u aktifleştirir (AppClassifier duplicate kontrolü).

---

## 5. Git Global Config

```powershell
git config --global pull.rebase true
git config --global rebase.autoStash true
git config --global push.autoSetupRemote true
```

---

## 6. Windows Defender Exclusion (Admin PowerShell)

```powershell
Add-MpPreference -ExclusionPath "C:\Users\<kullanici>\AppOrganizer\app\build"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.gradle"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.android"
```

---

## 7. İlk Build

```powershell
.\build.ps1          # Normal build (cache + parallel)
.\build.ps1 -Clean   # Kilit sorunu varsa
```

---

## 8. Emülatör

```powershell
$em = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\emulator\emulator.exe"
Start-Process $em -ArgumentList "-avd","Pixel6_AOSP33","-no-snapshot-save"

$adb = "C:\Users\hekizoglu\AppData\Local\Android\Sdk\platform-tools\adb.exe"
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n "com.armutlu.apporganizer/.presentation.ui.launcher.LauncherActivity"
```

---

## 9. smart_push (PowerShell Profile)

PowerShell profiline ekle (`$PROFILE`):

```powershell
function smart_push {
    param([string]$Branch = "main", [string]$Remote = "origin")
    git pull --rebase $Remote $Branch
    if ($LASTEXITCODE -eq 0) { git push $Remote $Branch }
}
```

Sonra `smart_push` komutuyla push yapılır (non-fast-forward önlenir).

---

*Son güncelleme: 2026-06-16*
