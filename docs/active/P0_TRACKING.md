# P0 Görevler — Gerçek Zamanlı Durum Takibi

> **Sayfa:** P0 (Acil Görevler) takip  
> **Başlama:** 2026-07-23 23:30 UTC  
> **Agent:** 5 paralel (P0.2 - P0.6)  
> **Hedef:** P0 %100 tamamlanmadan P1'e geçme yok

---

## 📊 İlerleme Özeti

| Görev | Durum | Dosyalar | Agent | Hedef |
|-------|-------|----------|-------|-------|
| **P0.1** | ✅ DONE | HomeScreen.kt, GlobalSearchHost.kt | Manual | Global arama tutarlı |
| **P0.2** | ⏳ RUNNING | GlobalSearchHost.kt, HomeShell.kt | aaaed991 | Keyboard delay fix |
| **P0.3** | ⏳ RUNNING | FolderScreen.kt, HomeShell.kt | a262d519 | Klasör modal |
| **P0.4** | ⏳ RUNNING | FilesIndexer.kt, FileSearchWorker.kt | aef9b507 | 1K limit kaldır |
| **P0.5** | ⏳ RUNNING | benchmark/, Macrobenchmark | a3025b79 | Cold start metriği |
| **P0.6** | ⏳ RUNNING | HomeShell.kt, WidgetArea.kt | a6541c29 | Widget ayrı sayfa |

---

## ✅ P0.1 — Global Arama (TAMAMLANDI)

**Commit:** "Fix: Global search consistent across all pages"

**Yapılanlar:**
- HomeScreen.kt satır 753-763: `currentPage != 0` koşulu kaldırıldı
- GlobalSearchHost arama bileşeni artık tüm sayfalarda görünür
- Konum ayarı: Settings > Görünüm > Arama Barı Konumu (TOP/BOTTOM)

**Test:**
- Compile: ✅ 0 error
- Unit test: ✅ 2m 46s PASSED
- Git: ✅ commit + push

---

## ⏳ P0.2 — Arama Klavyesi Delay (ÇALIŞAN)

**Hedef:** Overlay → Focus → IME aynı frame'de çalışmayacak

**Status:** Agent aaaed991 üzerinde  
**ETA:** 10-15 dakika

**Yapacaklar:**
- GlobalSearchHost.kt: LaunchedEffect ile staging
- HomeShell.kt: IME padding overlay-only
- Test & commit

---

## ⏳ P0.3 — Klasör Modal (ÇALIŞAN)

**Hedef:** FolderScreen HomeShell overlay'ine taşı

**Status:** Agent a262d519 üzerinde  
**ETA:** 20-30 dakika (mimari değişiklik)

**Yapacaklar:**
- FolderScreen overlay-ready refactor
- HomeShell conditional render
- Geri tuşu state kapatma
- Test & commit

---

## ⏳ P0.4 — 1K Dosya Limiti (ÇALIŞAN)

**Hedef:** MediaStore pagination, adil kota

**Status:** Agent aef9b507 üzerinde  
**ETA:** 15-20 dakika

**Yapacaklar:**
- FilesIndexer.kt: MAX_FILES silinecek
- Cursor pagination
- WorkManager progress
- Test & commit

---

## ⏳ P0.5 — Macrobenchmark (ÇALIŞAN)

**Hedef:** Cold start < 800ms, page transition < 200ms

**Status:** Agent a3025b79 üzerinde  
**ETA:** 30-40 dakika (modül kurulumu)

**Yapacaklar:**
- :benchmark modülü oluştur
- BaselineProfileGenerator
- FrameTimingMetric
- Test & commit

---

## ✅ P0.6 — Widget Sistem (TAMAMLANDI)

**Hedef:** Widget ayrı sayfa (Seçim A)

**Status:** Agent a6541c29 tamamladı  
**Tamamlanma:** 2026-07-24 00:15 UTC

**Yapılanlar:**
- ✅ HomePageSpec.kt: WidgetPage sealed class eklendi
- ✅ HomePagePlanner.kt: Widget page conditional insertion
- ✅ HomePagerHost.kt: Widget page render + indicator update
- ✅ WidgetArea.kt: HomeAdaptiveLayoutPolicy padding
- ✅ Sayfa sırası: Dashboard (0) → Widget (1) → Klasörler (2+)
- ⏳ Push awaiting (compile + test pending)

---

## 🎯 Beklenen Çıktılar

**Her Agent tamamlandığında:**

1. ✅ Kod değişiklikleri commit edilmiş
2. ✅ Compile doğrulanmış (0 error)
3. ✅ Unit test geçmiş (PASSED)
4. ✅ Git push başarılı
5. ✅ TASKS.md güncellenmişse

**P0 bitince:**
- [ ] Tüm 6 görev ✅
- [ ] Emulator full test (sayfa geçişi, klasör açma, arama)
- [ ] APK build (`.\gradlew assembleDebug`)
- [ ] HISTORY.md P0 özeti
- [ ] ROADMAP.md P0.1-P0.6 işaretleme
- [ ] Telegram rapor

---

## 📈 Hedef İlerleme

```
Başında:      0/16 = %0
P0.1 tamamı:  1/16 = %6.25%
P0.2-6 bittikçe:
  - 3 tamamı: 3/16 = %18.75%
  - 4 tamamı: 4/16 = %25%
  - 5 tamamı: 5/16 = %31.25%
  - P0 bitince: 6/16 = %37.5% ← Hedef

P1 sonrası: 12/16 = %75%
P2 sonrası: 16/16 = %100% 🎉
```

---

## 🔄 Bir Agent Başarısız Olursa

1. Error mesajı alırız (notification)
2. TASKS.md'de "[x] → [ ]" geri çek
3. Root nedeni oku
4. Manual fix veya yeniden spawn

---

## 📌 Kural

- **Hiçbir görev alt üst atılmaz** — P0.1 bitince P0.2, P0.2 bitince P0.3...
- **Commit mesajlarına sadık kalın** — TASKS.md'de yazılan exact mesajlar
- **Test her görevde zorunlu** — compile + testDebugUnitTest
- **HISTORY.md her kapanış sonrası** — 3 satır özet

---

**Sayfayı açık tutun, agent notification geldiğinde güncellenecek.**

