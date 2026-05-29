# 🚀 APP ORGANIZER - GITHUB ACTIONS APK BUILD

## TÜM KOMUTLAR BİR SAYFADA

### GitHub Hesabı
```
https://github.com/signup
Kayıt ol ve username seç
```

### New Repository
```
https://github.com/new
Name: app-organizer
Public seç → Create
```

### Komut: Proje Upload (Terminal'de)

```bash
# 1. AppOrganizer klasörüne git
cd /path/to/AppOrganizer

# 2. Repository initialize et
git init

# 3. Dosyaları ekle
git add .

# 4. Commit
git commit -m "App Organizer v1.0"

# 5. Branch
git branch -M main

# 6. Remote ekle (USERNAME'ı değiştir!)
git remote add origin https://github.com/USERNAME/app-organizer.git

# 7. Push
git push -u origin main
```

### GitHub'da Workflow Çalıştır
```
1. GitHub → repo → Actions tab
2. "Build and Release APK" workflow'u seç
3. "Run workflow" → Run workflow
4. Bekle (5-10 dakika)
```

### APK İndir
```
Actions → Latest run → Artifacts
↓
"debug-apk" indir → unzip → app-debug.apk
```

### Telefona Yükle
```
Email gönder kendine → Telefonda indir → Tap → Install
VEYA
Google Drive'a upload → Share link → Telefonda indir → Install
```

---

## HATAlı KOMUTLAR?

### Hata: "git command not found"
```
Çözüm: Git yükle
https://git-scm.com
```

### Hata: "Authentication failed"
```
Çözüm: GitHub token kullan
Settings → Developer settings → Personal tokens
```

### Hata: "Repository already exists"
```bash
rm -rf .git
git init
# Tekrar dene
```

### Hata: "Permission denied"
```bash
sudo git push origin main  # Mac/Linux
# Veya GitHub Desktop kullan
```

---

## GitHub Desktop Alternatifi

Komut satırı zor ise:

```
GitHub Desktop indir:
https://desktop.github.com

1. Aç
2. Sign in GitHub'a
3. "Clone a repository"
4. Projeni seç
5. "Publish branch"

Daha kolay! 🎨
```

---

## BITTI MI?

✅ GitHub'a push ettiysen
✅ Actions workflow'u çalıştıysa
✅ 10 dakika sonra APK'yı indirdiysen
✅ Telefona yüklediysen

**🎉 HEPSİ TAMAMLANDI!**

Test et ve kullan!

---

## İLETİŞİM

Sorun olursa:
1. GitHub Actions log'u screenshot'ını al
2. Error mesajını kopyala
3. Bana gönder
4. Çözeriz! 💪

**Başarılar!** 🚀
