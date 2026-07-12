# MemPalace Playbook

Tarih: 2026-07-13

## Amac

AppOrganizer icin MemPalace'i daha dogru, daha ucuz ve daha hizli kullanmak.

Resmi referanslara gore MemPalace katmanli hafiza modeliyle dusuk wake-up maliyeti hedefler; resmi repo GitHub'da, resmi dokuman alanlari ise repo ve `mempalaceofficial.com` olarak belirtiliyor. Proje tabanli `mempalace.yaml` de `init` akisi ile olusur.

Kaynaklar:

- GitHub repo: https://github.com/mempalace/mempalace
- Init/config tartismasi: https://github.com/MemPalace/mempalace/issues/185

## Bu projede calisma kurali

1. Once `mempalace status`
2. Sonra hedefli `search`
3. Sadece gerekirse `wake-up --wing apporganizer`
4. Buyuk degisikliklerden sonra hedefli `mine`
5. Haftalik veya ihtiyac halinde `sync --dry-run`, sonra gerekirse `--apply`

## Tercih edilen komutlar

### 1. Hizli arama

```powershell
mempalace search "notification report ticker weekly digest" --wing apporganizer
```

### 2. Room daraltmali arama

```powershell
mempalace search "usage report launchCount" --wing apporganizer --room app
```

### 3. Oturum basi baglam

```powershell
mempalace wake-up --wing apporganizer
```

Not:
Bu komut her istekte degil, yalnizca oturum basi veya baglam koptugunda kullanilmali.

### 4. Hedefli mine

```powershell
mempalace mine app --wing apporganizer --mode projects --max-chunks-per-file 2000
mempalace mine docs --wing apporganizer --mode projects --max-chunks-per-file 1000
mempalace mine scripts --wing apporganizer --mode projects --max-chunks-per-file 1000
```

### 5. Temizlik

```powershell
mempalace sync --wing apporganizer --dry-run
mempalace sync --wing apporganizer --apply
```

## Token verimi icin kural listesi

- Genis `mine .` yerine hedefli klasor mine et.
- Ayni oturum icinde `wake-up` tekrar etme.
- Arama sorgusunu her zaman `wing` ile daralt.
- Mümkunse `room` ile ikinci daraltma yap.
- Uzun arsiv dokumanlari yerine `NOW.md` ve `DECISIONS.md` gibi kisa kaynaklari guncel tut.
- Yeni karar ciktiysa once bu dosyalari guncelle, sonra gerekli ise mine et.
- Buyuk build loglarini ve gecici raporlari surekli hafizaya alma.
- Mine sirasinda chunk limit kullan; bellek ve CPU tuketimini frenler.

## Is verimi icin oneriler

- Kod gorevlerinde once repo aramasi + MemPalace search kullan; wake-up'ı sonra dusun.
- Test/QA icin `docs/qa` ve `docs/internal` ayri parcalarda mine edilmeli.
- Mimari kararlar `DECISIONS.md`'de tutulursa ayni tartismalar tekrar acilmaz.
- Gecici calisma durumu `NOW.md`'de tutulursa oturum basinda uzun anlatim ihtiyaci azalir.

## Bu projede kacinilacaklar

- `mempalace mine .` komutunu sik calistirmak
- room filtresi olmadan genis, belirsiz search atmak
- uzun `HISTORY.md` veya buyuk arsiv metinlerini tek basina ana hafiza gibi kullanmak
- ayni gunde birden fazla kez tam wake-up + tam mine yapmak
