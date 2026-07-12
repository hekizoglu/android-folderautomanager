---
name: apk-teslim
description: "AppOrganizer APK teslim zinciri: versiyon bump + duplicate kontrol + build + test + commit/push + Telegram APK gonderimi. Kullanici 'apk gonder', 'teslim et', 'build edip telegrama at' dediginde veya dongu kapanisinda kullan."
---

# APK Teslim Zinciri

Sirayla uygula. Her adim basarisiz olursa once asagidaki "Bilinen Sorunlar" bolumune bak, cozumu uygula, kullaniciya AGIKCA bildir (Ortam Sorunu Bildirim Kurali — sessiz retry yasak).

## Adimlar

1. **Versiyon bump** — `app/build.gradle.kts`: `versionCode` +1, `versionName` PATCH +1. Commit'e dahil edilecek.

2. **Duplicate kontrol** — `python scripts/check_duplicates.py assets/app_categories.json` (pre-commit hook da calistirir; onceden yakala).

3. **Build + test** (arka planda calistir, timeout 600000):
   ```powershell
   cd "c:\Users\hekizoglu\Documents\AppOrganizer"
   .\gradlew assembleDebug testDebugUnitTest -PskipGoogleServices --console=plain -q
   ```
   `google-services.json` repo'da varsa `-PskipGoogleServices` fladini KALDIR.

4. **Commit + push** — mesajda cift tirnak KULLANMA (PowerShell native arg gecisini bozar, commit sessizce basarisiz olur). Tek here-string `@'...'@`, ASCII, sonunda:
   `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`
   Ardindan `git push origin main`. `git log --oneline -1` ile commit'in gercekten olustugunu DOGRULA.

5. **Telegram APK gonderimi**:
   ```powershell
   Get-Content .env | ForEach-Object { if ($_ -match '^([^=#]+)=(.*)$') { Set-Item -Path "env:$($Matches[1].Trim())" -Value $Matches[2].Trim() } }
   $t = $env:TELEGRAM_BOT_TOKEN; $c = $env:TELEGRAM_CHAT_ID
   curl.exe -s -X POST "https://api.telegram.org/bot$t/sendDocument" -F "chat_id=$c" -F "caption=$cap" -F "document=@app\build\outputs\apk\debug\app-debug.apk"
   ```
   Caption Turkce, ASCII (curl -F icinde Turkce ozel karakter sorun cikarabilir), formatı: surum + YENI/DUZELTILDI maddeleri + varsa ortam sorunu ozeti. Yanittaki `"ok":true` dogrulanmali.

6. **Dokuman guncellemeleri** — HISTORY.md'ye dongu girisi (en ustteki `## Dongu` girisinden once), harcananvakit.md'ye BUILD satiri + APK boyutu (`(Get-Item app\build\outputs\apk\debug\app-debug.apk).Length / 1MB`). Turkce karakterli MD dosyalarina PowerShell Add-Content KULLANMA — Python `read_text/write_text(encoding='utf-8')` ile yaz.

## Bilinen Sorunlar (D229-236 otopsileri)

| Belirti | Cozum |
|---|---|
| `Unable to delete ... R.jar` / `checkDebugAarMetadata` / `packageDebug` kilidi | `Get-Process java \| Stop-Process -Force` (VSCode Java LS dahil — kendini yeniden baslatir) → 3 sn bekle → yeniden build. Hala kilitliyse `.\scripts\clear_build_lock.ps1` |
| `FileAlreadyExistsException` (KSP generated) veya `Cannot access output property` (kotlin-classes MD5) | Incremental state bozuk — `app\build` KOMPLE silinir (secici silme YASAK), sonra build |
| `Resource and asset merger ... merged.dir` | Ayni: tam temizlik + build |
| `google-services.json is missing` | `-PskipGoogleServices` ekle |
| Commit "pathspec ... did not match" hatasi | Commit mesajindaki cift tirnaklar — mesaji tirnak icermeyecek sekilde yeniden yaz |

## Kurallar
- Yarim birakma: build kirmiziysa cozmeden commit ATMA.
- `java_pid*.hprof`, `.vscode/`, kullanici dosyalari commit'e girmez (`git add -A -- ':!.vscode'`).
- Ayni sorun oturumda 2+ kez tekrarlarsa kalici cozum oner (Ortam Sorunu Bildirim Kurali).
