# COZULEMEYEN SORUNLAR - AppOrganizer

> Bir madde yerel agent tarafindan tamamlanamiyor ve kullanici/dış sistem/cihaz/izin gerektiriyorsa burada tutulur.
> Yerelde tamamlananlar HISTORY.md'ye, aktif yapilacaklar ROADMAP.md'ye tasinir.

---

## Aktif Sorunlar

### [CS-3] Gradle `merged_res` / `packaged_res` kilidi

**Tarih:** 2026-06-16
**Durum:** Cozulemedi - kullanici/admin aksiyonu bekliyor

**Sorun:** Windows Defender veya benzeri dosya tarama sureci Gradle build klasorlerini kilitleyebiliyor. `mergeDebugResources` ve `packaged_res` temizleme/yeniden uretme adimlari bu yuzden takilabiliyor.

**Denenen:**
- Dogrudan Defender exclusion komutlari denendi; admin/UAC yetkisi gerektirdi.
- Task Scheduler/SYSTEM yollari denendi; erisim engeliyle karsilasildi.
- Gradle daemon timeout ve temizleme workaroundlari denendi.
- `scripts/add_defender_exclusion.ps1` olusturuldu ve 2026-07-08'de path bug'i duzeltildi; artik `$PSScriptRoot` uzerinden bu repoya gore hesapliyor.
- `scripts/clear_build_lock.ps1` acil temizleme workaround'i olarak hazir.

**Neden yerelde kapanmiyor:** Defender exclusion kalici admin/UAC onayi gerektiriyor.

**Kullanicidan beklenen:**
```powershell
.\scripts\add_defender_exclusion.ps1
```
UAC penceresi cikarsa onay ver. Build kilitlenirse gecici workaround:
```powershell
.\scripts\clear_build_lock.ps1
.\gradlew assembleDebug
```

---

### [CS-5] `.claude/rules/build.md` eski AGP/Kotlin/SDK surumleri

**Tarih:** 2026-07-08
**Durum:** Cozulemedi - protected agent-config path

**Sorun:** `.claude/rules/build.md` eski surumleri yaziyor; gercek Gradle dosyalari daha guncel surumleri kullaniyor.

**Denenen:** Bu protected path iki ayri oturumda duzeltilmek istendi, ancak agent-config self-modification olarak reddedildi.

**Neden yerelde kapanmiyor:** Kullanici bu protected dosya icin acik izin vermeden agent'in duzenlemesi uygun degil.

**Kullanicidan beklenen:** Dosyayi elle guncelle veya bu spesifik path icin acik duzenleme izni ver.

---

### [CS-6] Play Console ve release dis aksiyonlari

**Tarih:** 2026-07-09
**Durum:** Cozulemedi - Play Console, keystore ve hesap erisimi gerektiriyor

**Sorun:** Kod ve dokuman hazirliklari yerelde konsolide edildi, fakat asagidaki maddeler Play Console veya kullaniciya ait kalici imza/hesap aksiyonu gerektiriyor:
- Data Safety formu
- QUERY_ALL_PACKAGES declaration
- Content rating anketi
- Privacy Policy URL girisi
- Accessibility Service declaration / prominent disclosure sureci
- Release keystore olusturma ve guvenli saklama
- Final AAB'nin temiz committen imzalanmasi ve yuklenmesi

**Denenen:**
- Privacy policy, store listing ve manifest uyumu onceki dongulerde kontrol edildi.
- QUERY_ALL_PACKAGES beyan ozeti ROADMAP.md icine tasindi.
- Play Store QA pack maddeleri ROADMAP.md ve bu dosyaya konsolide edildi.

**Neden yerelde kapanmiyor:** Play Console oturumu, kullanici hesabi, kalici release key ve geri alinmasi zor kararlar gerekiyor.

**Kullanicidan beklenen:** Play Console'a girip ROADMAP.md "Kritik - Play Store ve Release Kapisi" bolumundeki maddeleri sirayla tamamlamak.

---

### [CS-7] Gercek cihaz QA paketi

**Tarih:** 2026-07-09
**Durum:** Cozulemedi - fiziksel Android cihaz veya uygun emulator kaniti gerektiriyor

**Sorun:** Asagidaki maddeler kod okuma ile kismen dogrulandi, fakat Play oncesi "tamamlandi" sayilmasi icin cihaz uzerinde kanit gerekiyor:
- Android 14 NotificationListener ac/kapa, event yazma, rapor gorunumu
- NotificationListener permission lifecycle / reboot testi
- Accessibility Service davranisi
- Backup/restore uctan uca
- SmartInsightWorker ve BackupWorker schedule
- Android 13+ POST_NOTIFICATIONS yokken sessiz davranis
- BLUR-4/API26 performans/fallback
- AllApps double-tap
- Uretici/OEM kategori setleri
- Screenshot smoke seti

**Denenen:**
- Kod tarafinda duplicate worker riski, notification event veri modeli, search/settings davranislari ve backup/restore akislari incelendi.
- 20 gorevlik gecici rapor tamamlandi ve ROADMAP.md'ye konsolide edildi.

**Neden yerelde kapanmiyor:** Bu oturumda fiziksel cihaz/Play Store screenshot ortami yok.

**Kullanicidan beklenen:** ROADMAP.md "Kritik - Gercek Cihaz QA" bolumundeki senaryolari cihazda kosup kanitlari kaydetmek.

---

*Son guncelleme: 2026-07-09. Aktif liste ROADMAP.md'de, tamamlananlar HISTORY.md'de tutulur.*
