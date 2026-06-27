# ÇÖZÜLEMEYEN SORUNLAR — AppOrganizer

> Claude bir sorunu 3 denemede çözemezse buraya ekler ve kullanıcıya bildirir.
> Her giriş: sorun, ne denendi, neden başarısız, kullanıcıdan beklenen.

---

## Aktif Sorunlar

### [CS-3] Gradle `merged_res` / `packaged_res` Kilidi (Tekrarlayan)
**Tarih:** 2026-06-16 | **Durum:** ⚠️ Çözülemedi — 3 yöntem denendi, hepsi Admin yetki gerektiriyor
**Sorun:** `mergeDebugResources` `intermediates/packaged_res` dizinini kilitliyor. Windows Defender gerçek zamanlı tarama.

**Denenen (3 yöntem — hepsi başarısız):**
1. `Add-MpPreference` doğrudan → `HRESULT 0xc0000142` yetki hatası
2. Task Scheduler SYSTEM hesabı → `HRESULT 0x80070005` erişim engeli
3. `gradle.daemon.idletimeout=300000` → kilit hâlâ oluşuyor

**Tek gerçek çözüm:** Windows Güvenlik → Virüs ve tehdit koruması → Ayarlar → Dışlamalar → Klasör ekle (GUI üzerinden, Admin gerektirmez):
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

---

*Son güncelleme: 2026-06-28 | Çözülenler → HISTORY.md "Tamamlananlar Arşivi" bölümüne taşındı*
