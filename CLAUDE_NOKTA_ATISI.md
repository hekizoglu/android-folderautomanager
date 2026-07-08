# Nokta Atışı Görev Şablonu

> Bu dosya, Claude'a geniş repo taraması yaptırmadan dar kapsamlı, tek-oturumluk görevler vermek için şablon içerir.
> Amaç: token kullanımını düşürmek — Claude sadece belirtilen dosyalara bakar, belirtilen komutu önce çalıştırır, kapsam dışına çıkmaz.

---

## Format

```
GÖREV [N] — [kısa başlık]

Amaç: [1 cümlede ne bulunacak/düzeltilecek]

SADECE ŞUNLARA BAK:
- [dosya/klasör 1]
- [dosya/klasör 2]
- ...

ÖNCE ÇALIŞTIR:
- rg -n "[anahtar kelimeler]" [dosyalar]

YAPMA:
- [açıkça yasaklanan aksiyonlar — ör. "kod değiştirme", "build çalıştırma"]

ÇIKTI:
- [beklenen rapor formatı — ör. "sorun hâlâ geçerli mi", "hangi dosyada ne değişmeli", "minimum çözüm önerisi"]
```

## Kurallar

1. **SADECE ŞUNLARA BAK** listesi dar tutulmalı (ideal: 2-6 dosya/klasör) — Claude bu listenin dışına çıkmamalı, kapsam dışı bir bulgu varsa bunu raporda ayrıca belirtir ama otomatik genişletmez.
2. **ÖNCE ÇALIŞTIR** bir `rg -n` komutu olmalı — Claude'un context'i doğrudan doğru satırlarla doldurur, kör tarama yapmasını engeller.
3. **YAPMA** listesi net olmalı. "Sadece raporla" isteniyorsa Claude kod değiştirmez, sadece bulgu+öneri sunar. Uygulanmasını istiyorsan ayrı bir "uygula" mesajı gönder.
4. **ÇIKTI** formatı 3-5 maddelik kısa bir liste olmalı — uzun rapor dosyası istenmedikçe Claude yeni `.md` dosyası oluşturmaz.
5. Görevler birbirine bağımlıysa (ör. GÖREV 2'nin sonucu GÖREV 5'i etkiliyorsa) bunu görev metninde belirt; Claude aksi belirtilmedikçe her GÖREV'i bağımsız ele alır.
6. `.claude/rules/` altındaki dosyalar **protected path** — Claude bu dosyaları otomatik düzenleyemez, açık ve o göreve özel bir talimat gerekir (ör. "build.md'yi güncelle" açıkça yazılmalı, "tüm sorunları çöz" gibi genel talimatlar yetmez).

## Örnek (bu oturumdan)

```
GÖREV 1 — COZULEMEYEN_SORUNLAR doğrulama

Amaç: COZULEMEYEN_SORUNLAR.md içindeki add_defender_exclusion maddesini gerçek repo durumuna göre güncelle.

SADECE ŞUNLARA BAK:
- COZULEMEYEN_SORUNLAR.md
- scripts/
- gradle.properties
- gradlew.bat

ÖNCE ÇALIŞTIR:
- rg -n "add_defender_exclusion|Defender|Windows Defender|exclusion|gradle" COZULEMEYEN_SORUNLAR.md scripts gradle.properties gradlew.bat

YAPMA:
- Kod refactor etme.
- Build çalıştırma.
- İlgisiz dosya okuma.

ÇIKTI:
- Sorun hala geçerli mi?
- Hangi dosyada ne değişmeli?
- Minimum çözüm önerisi.
```

## Bilinen Tuzaklar (bu oturumdan öğrenilenler)

- **Otomatik-üretilen bloklar:** `ROADMAP.md` içindeki `<!-- DOCS_SCORE_HIGH_START/END -->` gibi işaretli bloklar bir script tarafından yeniden üretilebilir. Böyle bir bloğu elle düzeltmeden önce, gerçekten o script tarafından üretilip üretilmediğini doğrula (`scripts/score_docs_backlog.ps1` içinde uzun süre `R1-R7` hardcoded değildi, sadece formatı taklit eden elle eklenmiş satırlardı — script çalıştırılsaydı hepsini silip boş tabloyla değiştirecekti).
- **Phantom kaynak dosyalar:** Bir denetim raporunun "Kaynak" sütununda adı geçen dosya (`play-store-hazirlik-risk-raporu.md` gibi) repoda hiç var olmayabilir — büyük ihtimalle başka bir agent oturumunda (cloud/worktree) üretilip commit edilmemiştir. Kaynak dosya adını referans olarak kullanmadan önce `test -f` ile var olduğunu doğrula.
- **Protected agent-config path:** `.claude/rules/*.md` gibi dosyalara yapılan Edit çağrıları, kullanıcının o spesifik değişikliği o oturumda açıkça istemediği sürece auto-mode classifier tarafından reddedilir — bir önceki oturumda bulunan bir öneri bile "açık istek" sayılmaz.

---

*Oluşturulma: 2026-07-08 (Döngü 215) — "güncel-todo-raporu" audit turunun bir çıktısı olarak.*
