# Logic Audit Rapor Semasi

Her mantik denetimi raporu asagidaki duzende uretilir:

```text
P1 | RULE_ID | relative/path/File.kt:123 | kisa aciklama
```

## Alanlar

- `Oncelik`: `P1`, `P2`, `P3`
- `Rule Id`: sabit ve takip edilebilir kural anahtari
- `Konum`: repo goreli dosya ve satir
- `Aciklama`: davranissal sorunun net ozet cümlesi

## Ornek

```text
P1 | LS006 | app/src/main/.../SmartInsightWorker.kt:145 | Bildirim extra'si veriliyor ama hedef route tuketilmiyor
```

## Deep Tarama Klasoru

`logicAuditDeep` calistiginda tek satirlik rapor yerine klasor tabanli cikti uretilir:

```text
qa/reports/logic-audit-deep-YYYYMMDD-HHMMSS/
  summary.md
  findings.json
  sections/
    settings.md
    stats_reports.md
    launcher_home.md
    data_workers.md
  tool-outputs/
    detekt.txt
    lintDebug.txt
    testDebugUnitTest.txt
```

Her `findings.json` kaydi su alanlari icerir:

- `section`: bulgunun ait oldugu bolum
- `severity`: `P1`, `P2`, `P3`
- `id`: sabit kural anahtari
- `title`: kisa bulgu basligi
- `description`: davranissal hata ozeti
- `why_it_matters`: kullaniciya veya sisteme etkisi
- `path`: repo goreli dosya
- `line`: satir numarasi
- `snippet`: AI'nin konumu hizli anlamasi icin ilgili satir
- `recommendation`: cozum yonu
