# DECISIONS

## 2026-07-13 - Memory Palace kullanimi

- AppOrganizer icin Memory Palace kullanimi mevcut `memory-palace` CLI yuzeyine gore belirlenecek.
- `list` oturum basinda hizli durum kontrolu icin kullanilacak.
- `recover` ve `save` disinda eski `search/mine/wake-up/sync` akislari kullanilmeyecek.
- `mcp` ihtiyac halinde stdio server olarak calistirilacak.

## 2026-07-13 - Yuksek sinyalli hafiza dosyalari

- `NOW.md` kisa operasyonel durum icin kullanilacak.
- `DECISIONS.md` kalici kararlar icin kullanilacak.
- Uzun gecmis ve buyuk arsiv yerine bu iki dosya once okunacak.

## 2026-07-12 - Uygulama davranisi

- Usage report ve dashboard tarafinda `sure` ile `adet` ayni veri gibi gosterilmeyecek.
- Bildirim ve rapor kartlari tiklaninca ilgili hedefe gitmeli; bosa tiklama kalmamali.
- Launcher acilisinda tam reconcile sadece gerekliyse yapilmali.

## 2026-07-12 - Build ve dagitim

- `compileDebugKotlin` ve `assembleDebug` ile dogrulanan debug APK esas alinacak.
- APK ve durum raporu Telegram uzerinden paylasilabilir.
