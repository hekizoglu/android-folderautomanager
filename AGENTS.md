# AGENTS.md

## Zorunlu arastirma kurali

- Kullanici tarafindan istenen her isleme baslamadan once online arastirma yap.
- Online arastirmayi mumkun oldugunda resmi veya birincil kaynaklardan dogrula.
- Arastirma icin mevcutsa web arama, dokumantasyon kaynaklari ve alt-agent/subagent araclarini kullan.
- Alt-agent/subagent araclari mevcutsa, uygun ve ayrik bir arastirma gorevini onlara delege et.
- Online arastirma veya alt-agent kullanimi teknik olarak mumkun degilse, bunu acikca belirtmeden uygulamaya gecme.
- Arastirma sonucunda bulunan ilgili bulgulari kisa sekilde ozetle ve ardindan uygulamaya gec.

## MemPalace once

- Bu repoda onceki kararlar, denetim gecmisi, yapilan degisiklikler veya proje hafizasi gerekiyorsa cevaplamadan once MemPalace kullan.
- MCP araci mevcutsa once `mempalace_status`, sonra `mempalace_search`; iliskisel bilgi lazimsa `mempalace_kg_query` kullan.
- MCP araci mevcut degilse CLI fallback olarak `mempalace search "<sorgu>"` calistir.
- MemPalace sonucunda veri cikmazsa tahmin etme; eksigi belirtip sonra kodu ve diger kaynaklari incele.
