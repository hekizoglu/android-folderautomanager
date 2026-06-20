# MD Denetim Raporu — 2026-06-20

> Otomatik 6 saatlik MD denetim rutini. Telegram bu ortamda engellendiği için GitHub commit olarak raporlandı.
> **ONAY GEREKİYOR** — Bu dosya ve HISTORY.md dışında hiçbir değişiklik yapılmadı.

---

⚠️ **13 sorun tespit edildi** (12 önceki açık + 1 KRİTİK yeni)

---

## Özet Tablo

| # | Dosya | Sorun | Öncelik | Durum |
|---|-------|-------|---------|-------|
| 1 | ROADMAP.md | Paket sayısı "3116" stale (2 yer) | Orta | Açık (4. denetimden beri) |
| 2 | LEARNINGS.md | E13 duplicate satır (satır 145-146) | Düşük | Açık |
| 3 | LEARNINGS.md | Footer "HISTORprojY.md" encoding hatası | Düşük | Açık |
| 4 | ROADMAP.md | Sprint Metrikleri son satır kesilmiş ("K1/K") | Düşük | Açık |
| 5 | CLAUDE.md | Dipnot "v4" ↔ HISTORY.md D69 "v5" uyuşmazlık | Düşük | Açık |
| 6 | harcananvakit.md | D88-91 zaman logları eksik | Orta | Açık |
| 7 | harcananvakit.md | D86 satırı "Dongue 86" / "uyari" encoding bozuk | Orta | Açık |
| 8 | harcananvakit.md | "Tekrar Eden Sorunlar" tablosu güncel değil | Düşük | Açık |
| 9 | LEARNINGS.md | E14 eksik (derivedStateOf + String parametresi reaktif olmaz) | Orta | Açık |
| 10 | LEARNINGS.md | Room DB versiyon geçmişi v7'de bitiyor, v8 kaydı yok | Düşük | Açık |
| 11 | LEARNINGS.md | Onboarding adım listesi güncel değil (CLASSIFY_MODE eksik, 14 adım vs CLAUDE.md "14+2") | Orta | Açık |
| 12 | ROADMAP.md | Sprint Metrikleri D88-D91 döngü satırları eksik | Düşük | Açık |
| **13** | **HISTORY + CLAUDE + ROADMAP** | **FCM push özelliği (commit 34070c4, 2026-06-18 23:28) döngü logu yok — 8 dosya değişti, hiç belgelenmedi** | **🔴 KRİTİK** | **YENİ** |

---

## Yeni Kritik Bulgu — #13

### FCM Push Özelliği Belgelenmemiş (commit 34070c4)

**Commit:** `34070c4 feat: FCM push ile AppDatabase uzaktan güncelleme (#4)`  
**Tarih:** 2026-06-18 23:28 (D91 dark mode fix'ten SONRA)

**Değiştirilen dosyalar (244 satır ekleme / 97 satır silme):**

| Dosya | Değişiklik |
|-------|------------|
| `AppFirebaseMessagingService.kt` | YENİ — 71 satır FCM push handler |
| `AppOrganizerApp.kt` | +34 satır FCM init |
| `AllAppsDrawer.kt` | 90 satır değişim |
| `FolderSheet.kt` | 120 satır değişim |
| `AndroidManifest.xml` | +9 satır (FCM service + permission) |
| `app/build.gradle.kts` | +3 satır (Firebase Messaging dep) |
| `AppPrefs.kt` | +7 satır |
| `HISTORY.md` | 7 satır (D92 döngü logu YOK — sadece format düzeltmesi olabilir) |

**Sorunlar:**
1. HISTORY.md'de bu commit için döngü logu yok (Döngü 91'den sonra Döngü 92 girişi yok)
2. CLAUDE.md §7 Özellik Durum Özeti'nde Firebase hâlâ ❌ — FCM push uygulandı ama güncellenmedi
3. ROADMAP.md'de FCM push tamamlandı olarak işaretlenmemiş
4. LEARNINGS.md'de FCM mimari kararı yok
5. AllAppsDrawer.kt + FolderSheet.kt D91 dark mode fix'ten sonra tekrar değişti — dark mode gerilemeleri olabilir
6. Yeni Manifest permission/service — güvenlik açısından dikkat gerektiriyor

**Öneri:** Onay gelince bu feature'ı belgele:
- HISTORY.md'ye D92 girişi ekle
- CLAUDE.md Firebase satırını güncelle (FCM push ✅)
- ROADMAP.md'de tamamlandı işaretle  
- LEARNINGS.md'ye FCM mimarisi ekle
- AllAppsDrawer/FolderSheet dark mode regressionlarını kontrol et

---

## Daha Önce Tespit Edilen 12 Sorunun Detayı

### #1 — ROADMAP.md: Paket sayısı stale

| Yer | Mevcut | Doğru |
|-----|--------|-------|
| §Tamamlananlar / Akıllı Kategorizasyon | "3116+ benzersiz paket" | **3717** |
| §Backlog / Akıllı Kategorizasyon Aşama 3 | "3116 paketlik exactMatchMap" | **3717** |

---

### #2 — LEARNINGS.md: E13 duplicate

Satır 145-146 bire bir aynı E13 kaydı. **Fix:** İkinci E13 satırını sil.

---

### #3 — LEARNINGS.md: Footer encoding hatası

`HISTORprojY.md` → `HISTORY.md` olmalı.

---

### #4 — ROADMAP.md: Sprint Metrikleri son satır kesilmiş

Son satır `...düzeltme (K1/K` ile bitiyor. **Fix:** Tamamla veya sil.

---

### #5 — CLAUDE.md: Versiyon dipnotu uyuşmazlığı

- CLAUDE.md son satır: `CLAUDE.md v4`
- HISTORY.md Döngü 69: `CLAUDE.md v5`
**Fix:** CLAUDE.md son satırını `v5` olarak güncelle.

---

### #6 — harcananvakit.md: D88-91 logları eksik

Son kayıt Döngü 87. Eksikler:
- D88: AllApps arama kritik bug fix (~20dk, KOD+BUILD)
- D89: LauncherViewModelTest 4 yeni test (~15dk, TEST)
- D90: BUILD #17 — 24.79MB, 1s cache (~3dk, BUILD)
- D91: Dark mode hardcode renk düzeltmesi (~30dk, KOD)

---

### #7 — harcananvakit.md: D86 encoding bozuk

Satır 73: `Dongue 86 — AutoMirrored+Divider 55->18 uyari`  
→ `Döngü 86 — AutoMirrored+Divider 55->18 uyarı` olmalı.

---

### #8 — harcananvakit.md: "Tekrar Eden Sorunlar" stale

Gradle build dir kilitlenme hâlâ "Sık" yazıyor oysa Döngü 72'de Defender exclusion ile çözüldü (74x hız). merged_res kilidi ayrı satır gerekiyor.

---

### #9 — LEARNINGS.md: E14 eksik

D88 kritik bug: `derivedStateOf` + plain String parametresi reaktif değil → `remember(searchQuery) { ... }` ile çözüldü. Hata kataloğuna eklenmedi.

---

### #10 — LEARNINGS.md: Room DB v8 kaydı yok

LEARNINGS.md Room DB Versiyon Geçmişi v7'de bitiyor. CLAUDE.md §7: "Room DB: v8 (v7→v8 boş migration, 2026-06-16)".

---

### #11 — LEARNINGS.md: Onboarding adım listesi

CLAUDE.md: `14+2 adım (CLASSIFY_MODE → DEFAULT_LAUNCHER → DONE)`  
LEARNINGS.md: 14 adım, CLASSIFY_MODE yok, "SET_LAUNCHER" → uyuşmazlık.

---

### #12 — ROADMAP.md: Sprint Metrikleri D88-D91 satırları eksik

D88 (AllApps fix), D89 (LauncherViewModelTest), D90 (BUILD #17), D91 (dark mode) tabloya hiç eklenmemiş.

---

## Denetim Durumu

- **Önceki açık sorunlar:** 12 (MD_DENETIM_2026-06-20.md, ilk kontrol)
- **Bu denetimde yeni:** 1 (#13 FCM push belgelenmemiş — KRİTİK)
- **Düzeltilen:** 0
- **Toplam açık:** 13
- **Son kod değişikliği:** commit 34070c4 (2026-06-18 23:28 — FCM push)
- **Denetim tarihi:** 2026-06-20

**Telegram engellendiği için bu rapor GitHub commit olarak iletildi.**

---

*Denetim: Claude otomatik rutin | ONAY GEREKİYOR — değişiklik yapılmadı*
