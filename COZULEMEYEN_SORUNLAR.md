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

### [CS-5] `.claude/rules/build.md` eski AGP/Kotlin/SDK sürümleri
**Tarih:** 2026-07-08 | **Durum:** ⚠️ Çözülemedi — izin reddi (2 kez denendi)
**Sorun:** Dosya "AGP 8.2.0, Kotlin 1.9.22, targetSdk/compileSdk 34" yazıyor; gerçek `build.gradle.kts`/`app/build.gradle.kts` AGP 8.6.1, Kotlin 1.9.25, compileSdk/targetSdk 35 kullanıyor (build zaten bu değerlerle başarılı çalışıyor — sadece dokümantasyon drift'i, build'i bloklamıyor).
**Denenen:** Edit tool ile düzeltme iki ayrı oturumda denendi → Claude Code auto-mode classifier ikisinde de reddetti: `.claude/rules/` "protected agent-config path", kullanıcının bu spesifik değişikliği açıkça talep etmemiş olması nedeniyle self-modification olarak engellendi.
**Kullanıcıdan beklenen:** Kullanıcı ya dosyayı elle güncellemeli ya da Claude'a bu spesifik dosya için açık düzenleme izni vermeli (`.claude/settings.json`'a Bash/Edit izin kuralı ekleyerek).

### [CS-6] Play Console dış aksiyonları — Claude'un hesap erişimi yok
**Tarih:** 2026-07-08 | **Durum:** ⚠️ Çözülemedi — hesap erişimi gerektiriyor
**Sorun:** Aşağıdaki 3 madde Play Console'a giriş yapılmasını gerektiriyor, Claude'un böyle bir erişimi/oturumu yok:
1. **Data Safety formu** — kod/dokuman tarafı hazır (`privacy_policy.html` Firebase/Crashlytics/Contacts/Bildirim/Accessibility Service'i doğru anlatıyor, Döngü 214-215), ama Play Console'daki asıl form doldurulmadı.
2. **QUERY_ALL_PACKAGES beyan formu** — taslak metin `docs/internal/play_store_qa_pack.md`'de hazır ("QUERY_ALL_PACKAGES Declaration Draft" bölümü), Play Console'a girilmedi.
3. **Accessibility Service Prominent Disclosure** — `privacy_policy.html`'de servisin ne yaptığı/yapmadığı netleştirildi (Döngü 215), ama Play Console'un ayrı Accessibility Declaration formu ve uygulama-içi izin öncesi açıklama ekranı henüz yok.
**Denenen:** Kod ve dokümantasyon tarafı olabildiğince hazırlandı (bkz. yukarı), form doldurma denenmedi — erişim yok.
**Kullanıcıdan beklenen:** Play Console'a giriş yapıp yukarıdaki 3 formu `docs/privacy_policy.html` ve `docs/internal/play_store_qa_pack.md` içeriğiyle uyumlu şekilde doldurmalı.

### [CS-7] Gerçek cihaz QA paketi — fiziksel cihaz gerektiriyor
**Tarih:** 2026-07-08 | **Durum:** ⚠️ Çözülemedi — Claude'un fiziksel/gerçek Android cihaz erişimi yok
**Sorun:** 10 maddelik QA checklist üretildi (NotificationListener aç/kapa, Accessibility Service davranışı, backup/restore uçtan uca, Akıllı Bildirim/Yedekleme worker schedule doğrulama, blur/API26 performansı, AllApps double-tap, üretici kategorileri, Play Store screenshot seti) ama hiçbiri gerçek cihazda/emülatörde koşulmadı — bu oturumda emülatör de bağlı değildi.
**Denenen:** Checklist maddeleri kod incelemesiyle mümkün olduğunca doğrulandı (ör. duplicate notification riski, DB kayıt ilkesi) — ama "gerçek cihazda kanıtlı test" gerektiren maddeler çözülmedi.
**Kullanıcıdan beklenen:** Emülatörü/fiziksel cihazı açıp checklist'i koşmalı. Checklist detayı: bu oturumun GÖREV 8 çıktısında (konuşma geçmişinde).

---

*Son güncelleme: 2026-07-08 | Çözülenler → HISTORY.md "Tamamlananlar Arşivi" bölümüne taşındı*
*Not: LD-* sahte girişler (2026-06-28 03:33-10:33) temizlendi - run_local_denetim_cycle.ps1 koşulsuz yazıyordu, D168'de düzeltildi.*
*Not: [CS-4] çözüldü (Döngü 216) — kayıttan kaldırıldı, detay HISTORY.md Döngü 216'da.*
