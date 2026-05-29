# WINDOWS ICİN ADIM ADIM RESIMLI KILAVUZ

## ADIM 1: GIT KURULUMU

### Adim 1.1: Git Indir
```
1. https://git-scm.com/download/win aç
2. "Click here to download" butonuna tıkla
3. exe dosyası indirilecek
```

### Adim 1.2: Git Kur
```
1. İndirilmiş exe dosyasını çalıştır
2. İnstaller açılacak
3. "Next" "Next" "Next" diye tıkla
4. "Finish" tıkla
5. Bilgisayarı RESTART ET
```

### Adim 1.3: Kontrol Et
```
1. Başlat → CMD aç
   (Ya da: Win+R → cmd → Enter)
   
2. Şu komutu yaz ve Enter'a bas:
   git --version
   
3. Göreceksin:
   git version 2.xx.x...
   
   ✓ BAŞARILI!
```

---

## ADIM 2: GITHUB HESABI

```
1. https://github.com/signup aç

2. "Sign up for GitHub" sayfası açılacak

3. Email gir
   (Gerçek bir email olsun!)
   
4. "Continue" tıkla

5. Şifre yap
   (16+ karakter, büyük harf, sayı, sembol)
   
6. "Continue" tıkla

7. Username seç
   (Örn: hüseyinarmutlu)
   (Başlarda 0-9, - sembolü olamaz)
   
8. "Continue" tıkla

9. Email preferences ayarla
   (Skip edebilirsin)
   
10. "Continue" tıkla

11. CAPTCHA çöz
    (Resimde ne varsa yaz)
    
12. "Create account" tıkla

13. Email doğrula
    (Inbox'ına giden linki tıkla)
    
14. ✓ BAŞARILI!
```

---

## ADIM 3: REPOSITORY OLUSTUR

```
1. GitHub.com'a giriş yap

2. Sağ üst köşede "+" aç

3. "New repository" tıkla

4. Sayfada:
   
   Repository name: app-organizer
   Description: Telefon uygulamalarını kategorize et
   Public: ✓ Seçili
   
5. "Create repository" tıkla

6. ✓ BAŞARILI!
```

---

## ADIM 4: DOSYALARI ORGANIZE ET

### 4.1: Ana Klasör Oluştur

```
1. Masaüstüne sağ click
2. "New" → "Folder"
3. İsim: AppOrganizer
4. Enter
```

### 4.2: Dosyaları İndir ve At

```
1. /mnt/user-data/outputs klasöründen dosyaları indir
2. AppOrganizer klasörüne at
3. Şu klasör yapısını oluştur:

AppOrganizer/
├── .github/
│   └── workflows/
│       └── build.yml (github-actions-build.yml'yi kopyala ve isim değiştir)
├── app/
│   ├── src/main/java/...
│   └── src/test/java/...
├── build.gradle.kts
├── settings.gradle.kts
└── .gitignore
```

(Veya: Tüm dosyaları direkt at, biraz karışık olur ama GitHub Desktop organize edecek)

---

## ADIM 5: TERMINAL'DE KOMUT

### 5.1: CMD Aç

```
Başlat → cmd yazınız → Enter

VEYA

Win+R → cmd → Enter
```

### 5.2: AppOrganizer Klasörüne Git

```
Yazınız (USERNAME yerine senin Windows username'ı koy):

cd C:\Users\USERNAME\AppOrganizer

Örn:
cd C:\Users\Hüseyin\AppOrganizer

Enter'a bas
```

### 5.3: Git Komutları (BIRBIRI ARDINA)

```
Aşağıdaki her satırı yapıştır ve Enter'a bas:

1. git init
   → "Initialized..." göreceksin

2. git add .
   → Hiçbir çıktı yok, normal

3. git commit -m "App Organizer v1.0"
   → Dosya sayısı göreceksin

4. git branch -M main
   → Hiçbir çıktı yok, normal

5. git remote add origin https://github.com/USERNAME/app-organizer.git
   
   ⚠️  USERNAME'ı DEGISTIR! (GitHub username'ın)
   
   Örn:
   git remote add origin https://github.com/hüseyinarmutlu/app-organizer.git

6. git push -u origin main
   
   KOMUTU YAPISTIR VE ENTER'A BAS
   
   Sorulacak:
   "Username for 'https://github.com':"
   → GitHub username'ını yaz (örn: hüseyinarmutlu)
   
   "Password for 'https://github.com/USERNAME':"
   → GitHub şifreni yaz
   (Yaz ama görünmez, normal!)
   
   Enter'a bas
   
   VEYA eğer token ister:
   1. https://github.com/settings/tokens/new aç
   2. Token name: "GitHub Actions"
   3. Checkboxes: repo, workflow işaretle
   4. "Generate token" tıkla
   5. Token'ı kopyala
   6. Terminal'e yapıştır
   7. Enter'a bas
```

**Göreceksin:**
```
Enumerating objects: 39...
...
To https://github.com/USERNAME/app-organizer.git
 * [new branch]      main -> main
```

✓ BAŞARILI!

---

## ADIM 6: GITHUB'DA KONTROL

```
1. https://github.com/USERNAME/app-organizer aç
2. Dosyaları göreceksin!
3. "Actions" tab'ını tıkla
4. "Build and Release APK" workflow'u göreceksin
```

---

## ADIM 7: WORKFLOW TRIGGER

```
1. GitHub repo sayfasında → Actions tab

2. Sol tarafta "Build and Release APK" seç

3. Sağ tarafta "Run workflow" butonunu tıkla

4. Dropdown:
   Branch: main (zaten seçili)
   
5. Yeşil "Run workflow" butonuna tıkla

6. Başladı! Sayfayı refresh et
   (F5 tuşu)
   
7. Status göreceksin:
   🟡 In progress... (sarı, çalışıyor)
   
8. 10 DAKIKA BEKLE
   (Çay iç, telefonuna bak, relax)
   
9. Bitince:
   ✅ Completed (yeşil)
```

---

## ADIM 8: APK İNDİR

```
1. GitHub → Actions tab

2. En üstteki workflow'u tıkla
   (Status: Completed ✓)

3. Aşağı kaydır → "Artifacts" section

4. "debug-apk" zip'ini tıkla → İndir

5. İndirildi! (Downloads klasörüne)

6. İndirilen zip'i extract et:
   Sağ click → "Extract All"
   
7. İçinde "app-debug.apk" dosyası olacak

✓ BAŞARILI!
```

---

## ADIM 9: TELEFONA YUKLE

### Seçenek A: EMAIL (EN KOLAY)

```
1. app-debug.apk'ı bul
   (C:\Users\USERNAME\Downloads\debug-apk\app-debug.apk)

2. Sağ click → "Send to" → "Mail recipient"

3. Email aç (Gmail, Outlook, etc.)

4. Kendine gönder

5. Telefonda email aç

6. APK ekini tap

7. "Install" tıkla

8. ✓ TAMAMLANDI!
```

### Seçenek B: GOOGLE DRIVE

```
1. https://drive.google.com aç

2. Sağ click → "File upload" veya sürükle bırak

3. app-debug.apk'yı seç

4. Karşı tarafla share et (link al)

5. Telefonda linki aç

6. Download'u tıkla

7. İndir → Install

8. ✓ TAMAMLANDI!
```

### Seçenek C: USB KABLO

```
1. Telefonu USB kablosuyla PC'ye bağla

2. Telefonda: USB notification göreceksin
   → "Transfer files" seç

3. PC'de: Telefon görünür (File Explorer)
   C:\Users\USERNAME\AppOrganizer\... gibi
   
   (Veya: USB depolama olarak)

4. app-debug.apk'yı buraya kopyala

5. Telefonda: Dosya yöneticisi aç
   → İndirmeler
   → app-debug.apk
   → Tap
   → Install

6. ✓ TAMAMLANDI!
```

---

## ADIM 10: TEST

```
1. Telefonda "App Organizer" uygulama açılıyor

2. "Can this app access your installed apps?" yazı göreceksin

3. "ALLOW" tıkla

4. Uygulama başlıyor, uygulamaları tarayıyor
   (Biraz beklat, 30 saniye)

5. Ekranda ✨ floating button göreceksin

6. ✨ butonuna tıkla

7. "Organize apps?" dialog açılacak

8. "START" tıkla

9. Tüm uygulamalar kategorilere taşındı!

10. 🎉 TAMAMLANDI!
```

---

## SORUN OLURSA

### Hata: "git: command not found"

```
1. Git kurulu mu? (kurulum adımını kontrol et)
2. Bilgisayarı restart ettiysen?
3. Git yeniden kur
4. Restart
5. Tekrar dene
```

### Hata: "Repository already exists"

```
Terminal'de:
  
  rmdir /s .git
  (Evet diye cevap ver: Y)
  
  git init
  
  Tekrar dene
```

### Hata: "Repository not found"

```
Username yanlış mı?
Şifre yanlış mı?

Kontrol:
  1. GitHub'a giriş yap
  2. Repo sayfasına git
  3. URL doğru mu?
```

### APK Build Failed

```
1. GitHub → Actions → Latest run
2. Job logs'unu oku (kırmızı hata göreceksin)
3. Error mesajını kopyala
4. Bana gönder
5. Çözeriz!
```

---

## TAMAMLANDI!

Başarılar! Sorular varsa cevap veririm. 💪

İYİ ŞANSLAR! 🚀
