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
