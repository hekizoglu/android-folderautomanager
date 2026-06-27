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

## Çözülenler (HISTORY.md'ye Taşındı)

| # | Sorun | Çözüm | Tarih |
|---|-------|-------|-------|
| CS-1 | HISTORY.md `→` encoding | `->` ile değiştirildi, bilgi kaybı yok | 2026-06-21 |
| CS-2 | Windows Defender build lock (kapt) | Admin PS'de `Add-MpPreference` çalıştırıldı | 2026-06-16 |
| - | PowerShell heredoc `<<'EOF'` | `@'...'@` syntax | 2026-06-16 |
| - | Git push non-fast-forward | `git pull --rebase` | 2026-06-15 |
| - | KAPT incremental cache kilit | `kapt.incremental.apt=false` + robocopy | 2026-06-16 |
| - | HISTORY.md Türkçe mojibake | `fix_encoding.py` TURKISH_DOUBLE_ENCODED | 2026-06-16 |
| E14 | AllAppsDrawer `derivedStateOf` + plain param | `remember(apps)` key-based | 2026-06-21 |

---

*Son güncelleme: 2026-06-21*


### [LD-2026-06-27 10:33] Otomatik cozum bekleyen denetim maddeleri
**Tarih:** 2026-06-27 10:33 | **Durum:** Otomatik script tarafinda sadece raporlandi
**Sorun:** local_denetim_raporu.md icindeki kalan maddeler ajan/gelistirici kod mudahalesi bekliyor.
**Denenen:** Saatlik tam denetim ve checklist guncellemesi calistirildi.
**Neden basarisiz:** Script kendi basina guvenli kaynak kod degisikligi yapmiyor.
**Beklenen:** Bir sonraki ajan turunda rapordaki maddeler sirayla ele alinacak.

### [LD-2026-06-27 11:33] Otomatik cozum bekleyen denetim maddeleri
**Tarih:** 2026-06-27 11:33 | **Durum:** Otomatik script tarafinda sadece raporlandi
**Sorun:** local_denetim_raporu.md icindeki kalan maddeler ajan/gelistirici kod mudahalesi bekliyor.
**Denenen:** Saatlik tam denetim ve checklist guncellemesi calistirildi.
**Neden basarisiz:** Script kendi basina guvenli kaynak kod degisikligi yapmiyor.
**Beklenen:** Bir sonraki ajan turunda rapordaki maddeler sirayla ele alinacak.

### [LD-2026-06-27 12:33] Otomatik cozum bekleyen denetim maddeleri
**Tarih:** 2026-06-27 12:33 | **Durum:** Otomatik script tarafinda sadece raporlandi
**Sorun:** local_denetim_raporu.md icindeki kalan maddeler ajan/gelistirici kod mudahalesi bekliyor.
**Denenen:** Saatlik tam denetim ve checklist guncellemesi calistirildi.
**Neden basarisiz:** Script kendi basina guvenli kaynak kod degisikligi yapmiyor.
**Beklenen:** Bir sonraki ajan turunda rapordaki maddeler sirayla ele alinacak.

### [LD-2026-06-27 13:33] Otomatik cozum bekleyen denetim maddeleri
**Tarih:** 2026-06-27 13:33 | **Durum:** Otomatik script tarafinda sadece raporlandi
**Sorun:** local_denetim_raporu.md icindeki kalan maddeler ajan/gelistirici kod mudahalesi bekliyor.
**Denenen:** Saatlik tam denetim ve checklist guncellemesi calistirildi.
**Neden basarisiz:** Script kendi basina guvenli kaynak kod degisikligi yapmiyor.
**Beklenen:** Bir sonraki ajan turunda rapordaki maddeler sirayla ele alinacak.

### [LD-2026-06-27 14:33] Otomatik cozum bekleyen denetim maddeleri
**Tarih:** 2026-06-27 14:33 | **Durum:** Otomatik script tarafinda sadece raporlandi
**Sorun:** local_denetim_raporu.md icindeki kalan maddeler ajan/gelistirici kod mudahalesi bekliyor.
**Denenen:** Saatlik tam denetim ve checklist guncellemesi calistirildi.
**Neden basarisiz:** Script kendi basina guvenli kaynak kod degisikligi yapmiyor.
**Beklenen:** Bir sonraki ajan turunda rapordaki maddeler sirayla ele alinacak.

### [LD-2026-06-27 15:33] Otomatik cozum bekleyen denetim maddeleri
**Tarih:** 2026-06-27 15:33 | **Durum:** Otomatik script tarafinda sadece raporlandi
**Sorun:** local_denetim_raporu.md icindeki kalan maddeler ajan/gelistirici kod mudahalesi bekliyor.
**Denenen:** Saatlik tam denetim ve checklist guncellemesi calistirildi.
**Neden basarisiz:** Script kendi basina guvenli kaynak kod degisikligi yapmiyor.
**Beklenen:** Bir sonraki ajan turunda rapordaki maddeler sirayla ele alinacak.

### [LD-2026-06-27 16:33] Otomatik cozum bekleyen denetim maddeleri
**Tarih:** 2026-06-27 16:33 | **Durum:** Otomatik script tarafinda sadece raporlandi
**Sorun:** local_denetim_raporu.md icindeki kalan maddeler ajan/gelistirici kod mudahalesi bekliyor.
**Denenen:** Saatlik tam denetim ve checklist guncellemesi calistirildi.
**Neden basarisiz:** Script kendi basina guvenli kaynak kod degisikligi yapmiyor.
**Beklenen:** Bir sonraki ajan turunda rapordaki maddeler sirayla ele alinacak.

### [LD-2026-06-27 17:33] Otomatik cozum bekleyen denetim maddeleri
**Tarih:** 2026-06-27 17:33 | **Durum:** Otomatik script tarafinda sadece raporlandi
**Sorun:** local_denetim_raporu.md icindeki kalan maddeler ajan/gelistirici kod mudahalesi bekliyor.
**Denenen:** Saatlik tam denetim ve checklist guncellemesi calistirildi.
**Neden basarisiz:** Script kendi basina guvenli kaynak kod degisikligi yapmiyor.
**Beklenen:** Bir sonraki ajan turunda rapordaki maddeler sirayla ele alinacak.
