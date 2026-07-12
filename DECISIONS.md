# DECISIONS

## 2026-07-13 — MemPalace hafif kullanim modeli

- AppOrganizer icin MemPalace kullanimi `wing=apporganizer` ile sinirlanacak.
- Ilk tercih `search`; `wake-up` sadece oturum basi veya buyuk baglam kaybi oldugunda calistirilacak.
- `mine .` yerine hedefli mine tercih edilecek:
  - `mempalace mine app --wing apporganizer`
  - `mempalace mine docs --wing apporganizer`
  - `mempalace mine scripts --wing apporganizer`
- Buyuk hacimli mine calismalarinda `--max-chunks-per-file` siniri kullanilacak.
- Eski veya silinmis kaynaklar icin periyodik `mempalace sync --wing apporganizer` uygulanacak.

## 2026-07-13 — Yuksek sinyalli hafiza dosyalari

- `NOW.md` kisa operasyonel durum icin kullanilacak.
- `DECISIONS.md` kalici kararlar icin kullanilacak.
- Uzun gecmis ve buyuk arsiv yerine bu iki dosya once okunacak.

## 2026-07-12 — Uygulama davranisi

- Usage report ve dashboard tarafinda `sure` ile `adet` ayni veri gibi gosterilmeyecek.
- Bildirim ve rapor kartlari tiklaninca ilgili hedefe gitmeli; bosa tiklama kalmamali.
- Launcher acilisinda tam reconcile sadece gerekliyse yapilmali.

## 2026-07-12 — Build ve dagitim

- `compileDebugKotlin` ve `assembleDebug` ile dogrulanan debug APK esas alinacak.
- APK ve durum raporu Telegram uzerinden paylasilabilir.
