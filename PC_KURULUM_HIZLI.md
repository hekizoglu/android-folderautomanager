# PC KURULUM - 1 SAYFADA OZET

## ADIM ADIM (COPY-PASTE)

### 1. GIT INDIR

https://git-scm.com/download/win  (Windows)
https://git-scm.com/download/mac  (Mac)

Kur. Restart bilgisayar.

### 2. GITHUB HESABI ACIYOR

https://github.com/signup

- Email gir
- Şifre yap
- Username seç (hatırla!)
- Email doğrula

### 3. YENI REPOSITORY

https://github.com/new

- Name: app-organizer
- Public seç
- Create

### 4. DOSYALARI INDIR VE ORGANIZE

AppOrganizer klasörü yap:
```
C:\Users\Senin\AppOrganizer  (Windows)
```

Tüm indirdiğin dosyaları buraya at.

### 5. TERMINAL AC

Windows: Başlat → CMD
Mac: Cmd+Space → Terminal
Linux: Ctrl+Alt+T

### 6. TERMINAL KOMUTLARI (COPY-PASTE)

```bash
cd C:\Users\Senin\AppOrganizer

git init

git add .

git commit -m "App Organizer v1"

git branch -M main

git remote add origin https://github.com/USERNAME/app-organizer.git

git push -u origin main
```

(USERNAME'ı kendi username'inle değiştir!)

Kullanıcı adı sor → GitHub username yaz
Şifre sor → GitHub şifreni yaz

### 7. GITHUB'DA ACTIONS

https://github.com/USERNAME/app-organizer

→ Actions tab
→ "Build and Release APK"
→ "Run workflow"
→ "Run workflow" (yeşil buton)

BEKLE! 10 DAKIKA

### 8. APK INDIR

Actions → Latest workflow → Artifacts → "debug-apk" indir

Unzip et → app-debug.apk

### 9. TELEFONA YUKLE

app-debug.apk'yı kendine email'le gönder veya Google Drive'a upload et.

Telefonda:
→ Email aç / Google Drive aç
→ APK indir
→ Tap
→ Install

### 10. TEST

App'i aç → İzin ver → ✨ Buton → BITTI! 🎉

---

## HATALAR VE ÇOZÜMLER

| Hata | Çözüm |
|------|-------|
| git: command not found | Git yükle + restart |
| Permission denied | Personal access token oluştur |
| APK not found | Log'ları oku, bana gönder |
| Installation blocked | Settings → Security → Unknown sources → ON |

---

## KOLAY YOLU

GitHub Desktop kullan:

```
1. https://desktop.github.com indir
2. Aç
3. GitHub'a giriş yap
4. "Add" → Local repository
5. AppOrganizer klasörünü seç
6. "Publish branch"
```

Terminal komutları yok!

---

## TIMING

- Git kurulum: 2 min
- GitHub hesabı: 3 min
- Dosya organize: 3 min
- Terminal: 5 min
- GitHub push: 3 min
- Build bekleme: 10 min
- APK indir: 2 min
- Telefona yükle: 5 min

TOPLAM: 33 DAKİKA

---

## BAŞARI!

Sorun olursa: Error screenshot → bana gönder → çözeriz! 💪

Sorular varsa: Cevap veririm!

GitHub Desktop ile basitlerse onu kullan. Hepsi aynı işi yapıyor.

İYİ ŞANSLAR! 🚀
