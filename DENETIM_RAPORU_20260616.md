# 🔍 MD Denetim Raporu — 2026-06-16

> Otomatik denetim: CLAUDE.md · LEARNINGS.md · ROADMAP.md · HISTORY.md · harcananvakit.md
> Durum: ⚠️ **12 sorun tespit edildi** (3 kritik · 5 orta · 4 düşük)
> ONAY GEREKİYOR — bu rapor onay gelmeden değişiklik yapılmamıştır.

---

## 🔴 KRİTİK (acil düzeltme)

### K1 — CLAUDE.md §7: Room DB versiyonu stale
- **Dosya:** CLAUDE.md satır 293
- **Sorun:** "Room DB: v7 (18 yeni kategori eklendi)" yazıyor
- **Gerçek:** Döngü 71'de `app/schemas/8.json` oluşturuldu → Room artık **v8**
- **Öneri:** `Room DB: v8` olarak güncelle; "18 yeni kategori" notu v6→v7 geçişine aitti, v7→v8 boş migration

### K2 — LEARNINGS.md P11/P12 CLAUDE.md §5'te yok
- **Dosya:** LEARNINGS.md satır 43-44 (promote tablosu) → CLAUDE.md §5
- **Sorun:** P11 (`derivedStateOf` pattern) ve P12 (`installSplashScreen()` sırası) "§5'e taşındı" işaretli ama CLAUDE.md §5 (Kritik Mimari Tuzaklar) bölümünde bu maddeler **yok**
- **Öneri:** Ya CLAUDE.md §5'e ekle ya da LEARNINGS promote tablosundaki statüyü düzelt

### K3 — LEARNINGS.md: DVM register limiti eksik
- **Dosya:** HISTORY.md Döngü 47 "LEARNINGS.md: DVM register limiti eklendi" diyor → LEARNINGS.md'de **yok**
- **Sorun:** Kritik Compose tuzağı (büyük Composable → VerifyError → 4 parçaya böl) kayıt altına alınmamış; aynı hata tekrarlanabilir
- **Öneri:** LEARNINGS.md Hata Kataloğu'na E13 olarak ekle: "VerifyError/DVM register limit — büyük @Composable (300+ satır) → fonksiyona böl"

---

## 🟡 ORTA (sonraki döngüde)

### O1 — ROADMAP.md: Telegram token rotasyonu hâlâ "Bekliyor"
- **Dosya:** ROADMAP.md satır 201 (Onay Bekleyen Kararlar tablosu)
- **Sorun:** "Bekliyor" yazıyor ama Sprint bölümü satır 70'de ✅ tamamlandı
- **Öneri:** Tablodan çıkar veya "✅ Tamamlandı" yap

### O2 — ROADMAP.md: Stale paket sayısı (3594)
- **Dosya:** ROADMAP.md satır 131 (Döngüden Gelen)
- **Sorun:** "3594 paketlik `mapOf`" yazıyor (2026-06-15 tarihli); güncel sayı **3717**
- **Öneri:** Sayıyı 3717 olarak güncelle

### O3 — ROADMAP.md: Sprint Metrikleri Döngü #67-80 eksik
- **Dosya:** ROADMAP.md satır 193 (Sprint Metrikleri tablosu)
- **Sorun:** Son satır "Döngüler #62-66, 3680 benzersiz" (2026-06-16). Döngüler #67-80 eklenmemiş (3717 pakete ulaşıldı, 10+ özellik tamamlandı)
- **Öneri:** En az 1 satır ekle: "2026-06-16 | Döngüler #67-80 | ... | 3717 benzersiz"

### O4 — CLAUDE.md: Meta header "Son güncelleme: 2026-06-15" stale
- **Dosya:** CLAUDE.md satır 3
- **Sorun:** Döngü 70'de (2026-06-16) 5 düzeltme yapıldı, CLAUDE.md v5 güncellendi → tarih hâlâ 2026-06-15
- **Öneri:** "Son güncelleme: 2026-06-16" yap

### O5 — ROADMAP.md: Footer "Son güncelleme: 2026-06-15" stale
- **Dosya:** ROADMAP.md satır 208
- **Sorun:** 2026-06-16 tarihli Döngü 69-80 değişiklikleri var (satır 139-148 dahil)
- **Öneri:** "Son güncelleme: 2026-06-16" yap

---

## 🟢 DÜŞÜK (zaman bulununca)

### D1 — HISTORY.md: "Mimari Notlar" bölümü yanlış dosyada
- **Dosya:** HISTORY.md satır 127-148
- **Sorun:** Tema Sistemi, HorizontalPager, Bildirim Metni Mimarisi gibi **kalıcı** mimari kararlar döngü log arşivinin (HISTORY.md) içinde duruyor; CLAUDE.md kuralına göre mimari kararlar LEARNINGS.md'ye ait
- **Öneri:** Bu içeriği LEARNINGS.md § Mimari Kararlar bölümüne taşı (uygun olanları)

### D2 — HISTORY.md Loop 84-94 Özeti: Stale paket sayısı
- **Dosya:** HISTORY.md satır 445
- **Sorun:** "AppClassifier: 3375 benzersiz paket" yazıyor; bu o döneme ait doğru sayı ama tarihsel bağlamı belli değil
- **Öneri:** Satıra "(o dönem)" notu ekle: "3375 benzersiz paket ← o dönem; güncel: 3717 (Döngü 67)"

### D3 — LEARNINGS.md: Firebase Metrik Hedefleri bölümü yanlış dosyada
- **Dosya:** LEARNINGS.md satır 8-25
- **Sorun:** "Metrik Hedefler (Firebase ile İzlenecek)" ve "İzlenecek Firebase Events" **future planning** içeriği — LEARNINGS "tekrar eden öğrenme, tuzak, mimari karar" içindir; bu içerik ROADMAP'e ait
- **Öneri:** ROADMAP.md'ye "Firebase Analitik Hedefleri" bölümü olarak taşı

### D4 — harcananvakit.md: Tekrar Eden Sorunlar tablosu güncellenmemiş
- **Dosya:** harcananvakit.md satır 53-57
- **Sorun:** Tablo "Gradle build dir kilitlenme / Sık / 20-40 dk" yazıyor ama Döngü 72'de Defender exclusion ile 74x hız elde edildi; merged_res kilidi Döngü 78-79'da hâlâ devam ediyor
- **Öneri:** Tabloya "Çözüm Durumu" sütunu ekle: KAPT kilidi → ✅ Kısmen (Defender excl.), merged_res kilidi → 🔄 Devam ediyor

---

## 📊 Özet

| Öncelik | Adet | Aciliyet |
|---------|------|---------|
| 🔴 Kritik | 3 | Hemen |
| 🟡 Orta | 5 | Sonraki döngü |
| 🟢 Düşük | 4 | Zaman bulununca |
| **Toplam** | **12** | |

**En acil:** K1 (Room DB v8), K2 (P11/P12 CLAUDE.md'de yok), K3 (DVM register limiti kayıp)

---

*Rapor: otomatik MD denetim döngüsü · 2026-06-16 · Telegram engellendiği için commit'e yazıldı*
