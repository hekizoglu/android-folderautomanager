# PC KURULUM - BASMAK ICIN KONTROL LISTESI

```
ADIM 1: GIT KURULUMU
========================================================================
☐ https://git-scm.com/download/win aç (Windows)
☐ Exe dosyasını indir
☐ Çalıştır ve kur (Next, Next, Finish)
☐ Bilgisayarı restart et
☐ Terminal aç (Başlat → cmd)
☐ "git --version" yaz ve Enter'a bas
☐ Git version göründü? (Başarılı!)

Başlama saati: _____ Bitiş saati: _____


ADIM 2: GITHUB HESABI
========================================================================
☐ https://github.com/signup aç
☐ Email gir ________________
☐ Şifre oluştur ________________ (16+ karakter)
☐ Username seç ________________ (hatırla!)
☐ Email doğrula (inbox'ına giden linke tıkla)
☐ GitHub'da giriş yap
☐ Profil sayfasını gör? (Başarılı!)

Başlama saati: _____ Bitiş saati: _____


ADIM 3: REPOSITORY OLUSTUR
========================================================================
☐ GitHub'da → "+" → "New repository"
☐ Name: app-organizer yaz
☐ Public seç (✓)
☐ "Create repository" tıkla
☐ Repo sayfasında dosya listesi görmek istersen:
  ☐ Henüz boş (normal, hemen dolacak)

Başlama saati: _____ Bitiş saati: _____


ADIM 4: DOSYALARI ORGANIZE
========================================================================
☐ Masaüstüne "AppOrganizer" klasörü oluştur
☐ /mnt/user-data/outputs'tan dosyaları indir
☐ Tüm dosyaları AppOrganizer klasörüne at
☐ Kontrol: 30+ dosya var mı?

Başlama saati: _____ Bitiş saati: _____


ADIM 5: TERMINAL KOMUTLARI
========================================================================
☐ Terminal aç (Başlat → cmd)
☐ "cd C:\Users\USERNAME\AppOrganizer" yaz (USERNAME'ı değiştir)
☐ Enter'a bas
☐ "git init" yaz ve Enter
☐ "git add ." yaz ve Enter
☐ "git commit -m 'App Organizer v1.0'" yaz ve Enter
☐ "git branch -M main" yaz ve Enter
☐ "git remote add origin https://github.com/USERNAME/app-organizer.git" yaz ve Enter
  (USERNAME'ı değiştir!)
☐ Tüm komutlar çalıştı? (Başarılı!)

Başlama saati: _____ Bitiş saati: _____


ADIM 6: GITHUB'A PUSH
========================================================================
☐ "git push -u origin main" yaz ve Enter
☐ GitHub username gir: ________________
☐ GitHub şifresi gir: ________________ (görünmez, normal)
  (Veya token ister → GitHub settings → token oluştur → yapıştır)
☐ İletişim başarılı? (Başarılı!)
☐ GitHub'da repo sayfasını aç → dosyaları göreceksin

Başlama saati: _____ Bitiş saati: _____


ADIM 7: GITHUB ACTIONS TRIGGER
========================================================================
☐ GitHub repo sayfasında → Actions tab
☐ "Build and Release APK" workflow'u gör
☐ "Run workflow" butonuna tıkla
☐ Branch: main seçili? ✓
☐ "Run workflow" tıkla (yeşil buton)
☐ Build başladı mı? (sarı durum göreceksin)
☐ URL: github.com/USERNAME/app-organizer/actions

Başlama saati: _____ Bitiş saati: _____


ADIM 8: APK BUILD BEKLE
========================================================================
☐ GitHub Actions'da build'i izle
☐ Status: "In progress..." (sarı)
☐ 10 DAKIKA BEKLE
☐ Status: "Completed" oldu mu? (yeşil ✓)
☐ Build başarılı!

Bekleme saati: _____ dakika
Başlama saati: _____ Bitiş saati: _____


ADIM 9: APK INDIR
========================================================================
☐ GitHub Actions → Latest workflow
☐ Aşağı kaydır → "Artifacts" section
☐ "debug-apk" zip'ini tıkla
☐ İndir
☐ İndirildi mi? (Downloads klasörü kontrol et)
☐ Unzip et (Sağ click → Extract)
☐ İçinde "app-debug.apk" dosyası var mı?

Başlama saati: _____ Bitiş saati: _____


ADIM 10: TELEFONA YUKLE
========================================================================

SEÇENEK A: EMAIL
☐ app-debug.apk'ı bul
☐ Sağ click → "Send to" → "Mail recipient"
☐ Kendine gönder
☐ Telefonda email aç
☐ APK indirini tap
☐ "Install" tıkla

SEÇENEK B: GOOGLE DRIVE
☐ https://drive.google.com aç
☐ app-debug.apk upload et
☐ Share linki al
☐ Telefonda linki aç
☐ Download et
☐ Install et

SEÇENEK C: USB
☐ Telefonu USB kablosuyla bağla
☐ "Transfer files" modu seç
☐ app-debug.apk'ı telefona kopyala
☐ Telefonda: Dosya yöneticisi → İndir → APK tap
☐ Install et

☐ Kurulum başarılı? (App Organizer gördün mü?)

Başlama saati: _____ Bitiş saati: _____


ADIM 11: TEST
========================================================================
☐ App Organizer'ı tap (aç)
☐ Ekrana hoşgeldin mesajı geldi mi?
☐ "Can access installed apps?" sorusu?
  ☐ "ALLOW" tıkla
☐ Uygulamalar taranıyor... (30 saniye bekle)
☐ ✨ Floating button göründü?
☐ ✨ butonuna tap
☐ "Organize apps?" dialog açıldı?
☐ "START" tıkla
☐ Uygulamalar kategorilere taşındı mı?

Başlama saati: _____ Bitiş saati: _____


FINAL
========================================================================
☐ TÜM ADIMLAR TAMAMLANDI!
☐ TELEFONDA ÇALIŞAN APP VAR!
☐ BAŞARILI! 🎉

Toplam zaman: _____ dakika


SORUN ÇIKTI MI?
========================================================================
Hata: ________________________
Terminal mesajı: ________________________
Screenshot: Aldı mı? ☐
Bana gönderdin mi? ☐


NOT ALANI
========================================================================
________________________________________________
________________________________________________
________________________________________________
________________________________________________


TAMAMLANDI SAATI: _____/_____/_____  _____:_____

ÖĞRETMEN İMZASI (Bana bildir): ________________


========================================================================

BAŞARILAR! 🚀

Bu formu iki kez okuyup yazdırabilirsin.
Birinci yapışken çıktı, ikincisi yedek.

GitHub Desktop kullanmak istersen hepsini atla:
https://desktop.github.com indir, GUI ile yap!

========================================================================
