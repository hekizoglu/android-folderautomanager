# ÇÖZÜLEMEYEN SORUNLAR — AppOrganizer

> Claude bir sorunu 3 denemede çözemezse buraya ekler ve kullanıcıya bildirir.
> Her giriş: sorun, ne denendi, neden başarısız, kullanıcıdan beklenen.

---

## Aktif Sorunlar

### [CS-3] Gradle `merged_res` / `packaged_res` Kilidi (Tekrarlayan)
**Tarih:** 2026-06-16 | **Durum:** Workaround mevcut (full clean), kök çözüm eksik
**Sorun:** Monochrome icon eklenmesi sonrası `mergeDebugResources` her değişiklikte `intermediates/packaged_res` ve `incremental/debug/mergeDebugResources` dizinlerini kilitliyor. Mevcut Defender exclusion `app\build` kökünü kapsıyor ama `intermediates` alt dizinleri yeniden oluşturunca taranıyor.
**Çözüm adayı:** Admin PS'de şunu ekle:
```powershell
Add-MpPreference -ExclusionPath "C:\Users\hekizoglu\Documents\AppOrganizer\app\build\intermediates"
```
**Kullanıcıdan beklenen:** Admin PowerShell'de yukarıdaki komutu çalıştır.

### ~~[CS-1] HISTORY.md Arrow Karakteri (→) Tam Kurtarılamadı~~ ✅ KAPANDI 2026-06-21
**Sonuç:** `→` işaretleri `->` haline dönüştürüldü. Bilgi kaybı yok — müdahale gerekmez. HISTORY.md şu an temiz UTF-8.

---

### ~~[CS-2] Windows Defender Build Lock~~ ✅ ÇÖZÜLDÜ 2026-06-16
**Tarih:** 2026-06-16 | **Durum:** Kullanıcı Admin PowerShell'de exclusion ekledi
**Sorun:** Gradle build sırasında `app\build\tmp\kapt3\` ve `generated\source\kapt\` dizinleri Windows Defender tarafından kilitleniyor.
**Yapılan:**
1. `gradle.properties`: `kapt.use.worker.api=false`, `kapt.incremental.apt=false` — kilit sıklığı azaldı ama devam ediyor
2. `build.ps1`: daemon durdur + robocopy purge — workaround çalışıyor ama her seferinde ~5dk
3. Defender exclusion: Admin yetkisi gerekli, Claude Code'un yetkisi yok
**Kullanıcıdan beklenen:** Admin PowerShell'de bir kez çalıştır:
```powershell
Add-MpPreference -ExclusionPath "C:\Users\hekizoglu\Documents\AppOrganizer\app\build"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.gradle"
Add-MpPreference -ExclusionPath "$env:USERPROFILE\.android"
```

---

## Çözülen (Referans)

| # | Sorun | Çözüm | Tarih |
|---|-------|-------|-------|
| - | PowerShell heredoc `<<'EOF'` | `@'...'@` syntax — kapatan `'@` sıfır indent | 2026-06-16 |
| - | Git push non-fast-forward | `git pull --rebase` + `git config --global pull.rebase true` | 2026-06-15 |
| - | KAPT incremental cache kilit | `kapt.incremental.apt=false` + robocopy purge | 2026-06-16 |
| - | HISTORY.md Türkçe mojibake | `fix_encoding.py` TURKISH_DOUBLE_ENCODED tablosu | 2026-06-16 |
| CS-2 | Windows Defender build lock | Kullanıcı Admin PS'de `Add-MpPreference` çalıştırdı | 2026-06-16 |

---

*Son güncelleme: 2026-06-16*
