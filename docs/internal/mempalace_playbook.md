# MemPalace Playbook

Tarih: 2026-07-13

## Amac

AppOrganizer icin bu makinadaki Memory Palace CLI ile calisan akislari belgelemek.

Not: Bu ortamda CLI adi `memory-palace`. Eski `mempalace status/search/mine/wake-up/sync` komutlari yok.

Kaynaklar:

- Local CLI help: `memory-palace --help`
- GitHub repo: https://github.com/mempalace/mempalace

## Bu projede calisma kurali

1. Once `memory-palace list`
2. Gerekirse `memory-palace recover <short_id>`
3. Yeni kayit icin `memory-palace save <json_file>`
4. MCP gerekiyorsa `memory-palace mcp`
5. Ilk kurulum veya yenileme icin `memory-palace init`

## Tercih edilen komutlar

### 1. Son kayitlar

```powershell
memory-palace list
```

### 2. Kayit geri getirme

```powershell
memory-palace recover <short_id>
```

### 3. JSON kaydetme

```powershell
memory-palace save path\to\memory.json
```

### 4. MCP sunucusu

```powershell
memory-palace mcp
```

## Token verimi icin kural listesi

- Genis ve gereksiz tekrar eden kayitlar yerine kisa ve baglantili notlar tut.
- Uzun arsiv dokumanlari yerine `NOW.md` ve `DECISIONS.md` gibi kisa kaynaklari guncel tut.
- Yeni karar ciktiysa once bu dosyalari guncelle, sonra gerekiyorsa yeni kayit ekle.
- Buyuk build loglarini ve gecici raporlari surekli hafizaya alma.
- Kayit eklerken tek bir iyi tanimlanmis JSON kullan.

## Is verimi icin oneriler

- Kod gorevlerinde once repo aramasi ve ilgili notlari incele.
- Test/QA icin `docs/qa` ve `docs/internal` ayri parcalarda tutulmali.
- Mimari kararlar `DECISIONS.md`'de tutulursa ayni tartismalar tekrar acilmaz.
- Gecici calisma durumu `NOW.md`'de tutulursa oturum basinda uzun anlatim ihtiyaci azalir.

## Bu projede kacinilacaklar

- Olmayan `search`, `mine`, `wake-up`, `sync` akislari icin eski notlara guvenmek
- Uzun ve belirsiz arama benzeri surecleri belgeyi sisirmek icin kullanmak
- Uzun `HISTORY.md` veya buyuk arsiv metinlerini tek basina ana hafiza gibi kullanmak
- Ayni gunde birden fazla kez gereksiz toplu kayit islemi yapmak
