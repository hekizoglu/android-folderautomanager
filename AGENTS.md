# AGENTS.md — AppOrganizer Agent Strategy

## Online Research Rule (ZORUNLU — tüm agent görevlerinde)

- **Tetikleyen:** Yeni API, versiyon uyumluluk, derleme hatası, daha önce çözülmemiş teknik sorun
- **Kaynaklar:** Official docs > primary sources (GitHub, developer.android.com) > StackOverflow
- **Subagent:** Bağımsız araştırma görevleri → `Explore` agent ile paralel dele
- **Fallback:** Araştırma yapılamazsa açık belirt; koddan önce plan ver

## Memory First (Proje Hafızası)

- `CLAUDE.md`, `LEARNINGS.md`, `DECISIONS.md` projeye özgü kurallar
- Karar tersine çevrilmişse (eski → yeni ROADMAP) — yeni karar geçerli
- Mimari tuzaklar (LEARNINGS.md §5) — kod yazılmadan kontrol et

## Parallelization (Bağımsız işleri paralel çalıştır)

- 2+ araştırma görevleri → aynı anda agent çağrı
- 1 agent sonucu → sonraki adımın inputu ise sıralı tutmalı
- Context düşük tutmak için ağır işleri Sonnet agent'a (hafif iterasyon Haiku'da)

## Report Format

Her R item bitince:
- **Item:** R# ile başlık
- **Dosyalar:** changed + lines (örn: `AppIconView.kt:56-60`)
- **Tests:** pass/fail, komut
- **Build:** duration + result
- **Commit:** hash (local, push yok)
- **Risk:** Bilinen engeller, sonraki bağımlılık
