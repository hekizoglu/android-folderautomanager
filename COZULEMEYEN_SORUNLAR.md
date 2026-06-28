# ÇÖZÜLEMEYEN SORUNLAR — AppOrganizer

> Claude bir sorunu 3 denemede çözemezse buraya ekler ve kullanıcıya bildirir.
> Her giriş: sorun, ne denendi, neden başarısız, kullanıcıdan beklenen.

---

## Aktif Sorunlar

### [CS-3] Gradle `merged_res` / `packaged_res` Kilidi (Tekrarlayan)
**Tarih:** 2026-06-16 | **Durum:** ⚠️ Çözülemedi — 3 yöntem denendi, hepsi Admin yetki gerektiriyor
**Sorun:** `mergeDebugResources` `intermediates/packaged_res` dizinini kilitliyor. Windows Defender gerçek zamanlı tarama.

**Denenen (4 yöntem):**
1. `Add-MpPreference` doğrudan → `HRESULT 0xc0000142` yetki hatası
2. Task Scheduler SYSTEM hesabı → `HRESULT 0x80070005` erişim engeli
3. `gradle.daemon.idletimeout=300000` → kilit hâlâ oluşuyor
4. **UAC Self-Elevation script** → `scripts/add_defender_exclusion.ps1` oluşturuldu — çift tıklayınca UAC kutusu çıkar, "Evet" deyince exclusion eklenir. **Henüz kullanıcı tarafından test edilmedi.**

**Önerilen çözüm:** `scripts/add_defender_exclusion.ps1` üzerine çift tıkla → UAC "Evet" de.  
**Alternatif (GUI):** Windows Güvenlik → Virüs ve tehdit koruması → Ayarlar → Dışlamalar → Klasör ekle:
```
C:\Users\hekizoglu\Github Klasörleri\android-folderautomanager\android-folderautomanager\app\build
C:\Users\hekizoglu\.gradle
C:\Users\hekizoglu\.android
```

**Acil workaround (build kilitleninceye kadar):**
```powershell
Get-Process java | Stop-Process -Force
Remove-Item -Recurse -Force app\build
.\gradlew assembleDebug
```

---

*Son güncelleme: 2026-06-28 | Çözülenler → HISTORY.md "Tamamlananlar Arşivi" bölümüne taşındı*
*Not: LD-* sahte girişler (2026-06-28 03:33–10:33) temizlendi — run_local_denetim_cycle.ps1 koşulsuz yazıyordu, D168'de düzeltildi.*
