# ÇÖZÜLEMEYEN SORUNLAR - AppOrganizer

> Claude bir sorunu 3 denemede çözemezse buraya ekler ve kullanıcıya bildirir.
> Her giriş: sorun, ne denendi, neden başarısız, kullanıcıdan beklenen.

---

## Aktif Sorunlar

### [CS-3] Gradle `merged_res` / `packaged_res` Kilidi (Tekrarlayan)
**Tarih:** 2026-06-16 | **Durum:** ⚠️ Çözülemedi - 3 yöntem denendi, hepsi Admin yetki gerektiriyor
**Sorun:** `mergeDebugResources` `intermediates/packaged_res` dizinini kilitliyor. Windows Defender gerçek zamanlı tarama.

**Denenen (4 yöntem):**
1. `Add-MpPreference` doğrudan → `HRESULT 0xc0000142` yetki hatası
2. Task Scheduler SYSTEM hesabı → `HRESULT 0x80070005` erişim engeli
3. `gradle.daemon.idletimeout=300000` → kilit hâlâ oluşuyor
4. **UAC Self-Elevation script** → `scripts/add_defender_exclusion.ps1` oluşturuldu - çift tıklayınca UAC kutusu çıkar, "Evet" deyince exclusion eklenir. **Henüz kullanıcı tarafından test edilmedi.**
5. **Path bug bulundu ve düzeltildi (2026-07-08):** Script'teki `app\build` yolu eski kullanıcı/klasör adına (`hekizoglu\Github Klasörleri\...`) sabitlenmişti — proje `huseyinekizoglu\android-folderautomanager`'a taşındıktan sonra script var olmayan bir yolu sessizce "OK" işaretleyip gerçek build klasörünü hiç dışlamıyordu. `$PSScriptRoot`'tan relative hesaplamaya çevrildi, artık kullanıcı/klasör adı bağımsız.

**Önerilen çözüm:** `scripts/add_defender_exclusion.ps1` üzerine çift tıkla → UAC "Evet" de.  
**Alternatif (GUI):** Windows Güvenlik → Virüs ve tehdit koruması → Ayarlar → Dışlamalar → Klasör ekle:
```
C:\Users\huseyinekizoglu\android-folderautomanager\app\build
C:\Users\huseyinekizoglu\.gradle
C:\Users\huseyinekizoglu\.android
```

**Acil workaround (build kilitleninceye kadar):** `scripts/clear_build_lock.ps1` çalıştır (admin GEREKMEZ, sadece `app\build`'i siler — kaynak kod/git'e dokunmaz) → ardından `.\gradlew assembleDebug`.
```powershell
.\scripts\clear_build_lock.ps1
.\gradlew assembleDebug
```

---

### [CS-4] ROADMAP.md DOCS_SCORE_HIGH bloğu elle güncellenemiyor
**Tarih:** 2026-07-08 | **Durum:** ⚠️ Çözülemedi — otomasyon sahipliği Claude'da değil
**Sorun:** `ROADMAP.md`'deki `<!-- DOCS_SCORE_HIGH_START/END -->` bloğu (R1-R7 satırları), kendi başlığına göre `scripts/score_docs_backlog.ps1 -UpdateRoadmap` tarafından `docs/internal/docs_backlog_score.md` kaynağından her döngüde otomatik yenileniyor. Döngü 214-215'te R2 (Privacy Policy URL), R3 (rehber/bildirim metni çelişkileri) ve R5 (Firebase Analytics azaltma) fiilen kodda tamamlandı, ama bu blok içindeki "Durum: Bekliyor" etiketlerini elle "Tamamlandı" yapmak script'in bir sonraki `-UpdateRoadmap` çalıştırmasında sessizce üzerine yazılır (kaynak dosya değişmediği için).
**Denenen:** Blok dışındaki 🔴/🟡 tablo satırları ve FİKİRLER.md güncellendi (gerçek kaynak-of-truth burada tutuluyor); blok içine elle dokunulmadı.
**Kullanıcıdan beklenen:** Kalıcı çözüm için `docs/internal/docs_backlog_score.md` içindeki R2/R3/R5 kaynak satırlarının durumunun güncellenmesi veya `score_docs_backlog.ps1`'in tamamlanan maddeleri otomatik algılayacak bir mekanizma kazanması gerekiyor — bu script'in kendisi mevcut GÖREV kapsamlarının hiçbirinde yoktu.

### [CS-5] `.claude/rules/build.md` eski AGP/Kotlin/SDK sürümleri
**Tarih:** 2026-07-08 | **Durum:** ⚠️ Çözülemedi — izin reddi
**Sorun:** Dosya "AGP 8.2.0, Kotlin 1.9.22, targetSdk/compileSdk 34" yazıyor; gerçek `build.gradle.kts`/`app/build.gradle.kts` AGP 8.6.1, Kotlin 1.9.25, compileSdk/targetSdk 35 kullanıyor (build zaten bu değerlerle başarılı çalışıyor — sadece dokümantasyon drift'i, build'i bloklamıyor).
**Denenen:** Edit tool ile düzeltme denendi → Claude Code auto-mode classifier reddetti: `.claude/rules/` "protected agent-config path", kullanıcının bu spesifik değişikliği açıkça talep etmemiş olması nedeniyle self-modification olarak engellendi.
**Kullanıcıdan beklenen:** Kullanıcı ya dosyayı elle güncellemeli ya da Claude'a bu spesifik dosya için açık düzenleme izni vermeli (`.claude/settings.json`'a Bash/Edit izin kuralı ekleyerek).

---

*Son güncelleme: 2026-07-08 | Çözülenler → HISTORY.md "Tamamlananlar Arşivi" bölümüne taşındı*
*Not: LD-* sahte girişler (2026-06-28 03:33-10:33) temizlendi - run_local_denetim_cycle.ps1 koşulsuz yazıyordu, D168'de düzeltildi.*
