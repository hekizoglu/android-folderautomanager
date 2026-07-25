# Antigravity IDE — Kod Tarama Döngüsü Prompt'u

> Bu dosyayı Antigravity'nin agent/chat penceresine yapıştır. Amaç: AppOrganizer kod tabanının
> modül modül denetimini (ölü kod, kopuk ayar zincirleri, mantık hataları) Antigravity'nin kendi
> ajanına yaptırmak — Claude oturumunun token bütçesi mimari karar ve orkestrasyona ayrılsın.

---

## Görev

Sen bir kod denetim ajanısın. Proje: `c:\Users\hekizoglu\Documents\AppOrganizer` (Android/Kotlin,
Jetpack Compose launcher uygulaması).

**Tek gerçek kaynak:** `MANAGEMENT\KOD_TARAMA_DURUM.md`. Her çalıştırmada:
1. Bu dosyayı oku — üstteki "Görev Tanımı" ve "Kurallar" bölümlerini birebir uygula.
2. "Modül Listesi ve Durum" tablosunda `DEVAM` işaretli satırı bul (yoksa ilk `BEKLEMEDE` satırını
   `DEVAM` yap).
3. O modülü kurallardaki 5 denetim eksenine göre işle:
   - **Ölü kod:** caller'sız fonksiyon/parametre/import/state — SİL, ama önce grep ile 0-caller
     kanıtla (public API ve testte kullanılanlara dokunma).
   - **Hatalı kodlama:** yanlış alan adı, string literal sınıf yolu (her zaman `X::class.java`
     kullan), kopuk yaz→oku→TÜKET zinciri, limit/kabul-kayıt yolu uyuşmazlığı, sihirli sayı
     (mümkünse token/sabit'e bağla), yanlış paket referansı.
   - **Mantık hataları:** ters koşul, unreachable kod, race condition, `remember {}` ile donmuş
     tercih okuması (reaktif `DisposableEffect`+`OnSharedPreferenceChangeListener` pattern'i
     gerekiyorsa uygula), Türkçe locale eksikliği (`Locale("tr")` olmadan `contains(ignoreCase=true)`
     güvenilmez).
   - **Ayarlar zincir testi (Settings modüllerinde):** her satırı sınıflandır — gerçek ayar mı /
     bilgi mi / yönlendirme mi. Her toggle için üç halkayı da kanıtla: **yazılıyor mu → okunuyor mu
     → gerçekten bir davranışı DEĞİŞTİRİYOR mu?** Kopuksa: küçük/güvenliyse bağla; büyük/riskliyse
     kaldır veya "yakında" etiketli kilitli satıra çevir. Kullanıcıyı kandıran işlevsiz toggle
     bırakma.
   - **Geliştirme:** sadece küçük, düşük riskli iyileştirmeler doğrudan kodda yapılır. Büyük/yeni
     özellik fikirleri KODA DÖKÜLMEZ — `FİKİRLER.md`'ye not düşülür, 15+ puan alanlar (Kullanıcı
     Değeri + Uygulanabilirlik + Bağımlılık Riski + Etki Alanı, her biri 1-5) `ROADMAP.md`'ye
     eklenir.
4. Modül bitince: `gradlew :app:compileDebugKotlin -PskipGoogleServices --console=plain -q` ile
   hızlı derleme doğrulaması yap (tam `assembleDebug` + APK gerekmiyor, sadece derleme).
5. `MANAGEMENT\KOD_TARAMA_DURUM.md`'yi güncelle:
   - Modül tablosunda durumu `TAMAM` (veya çözemediysen `BLOKE`) yap, bir sonraki modülü `DEVAM`
     işaretle.
   - "İterasyon Günlüğü" bölümüne yeni bir `### <tarih> — M<n> (Antigravity)` başlığı aç; önceki
     modül girişlerinin formatını birebir kopyala (silinen semboller / bağlanan kopuk halkalar /
     ertelenen bulgular / doğrulanan sağlam desenler / build sonucu / değişen dosya:satır listesi
     / sayısal özet).
   - **Türkçe karakterli MD dosyalarını yazarken PowerShell `Add-Content`/`Set-Content` KULLANMA**
     (CP1254 encoding bozuyor) — Python ile `path.write_text(content, encoding='utf-8')` kullan.
6. `HISTORY.md`'nin en üstüne (ilk `## ` başlığından önce) 3 satırlık bir döngü özeti ekle:
   `**Yapılanlar:**` / `**Bug:**` / `**Sonraki:**` formatında.
7. Git commit at:
   - Mesajda **çift tırnak kullanma** (Windows/PowerShell commit'i sessizce bozabiliyor).
   - Commit mesajı sonu: `Co-Authored-By: Antigravity <noreply@antigravity.google>`
   - `git push origin main` dene; 2 dakikada tamamlanmazsa günlüğe "push denendi, timeout" notu
     düş ve bloklanma, bir sonraki adıma geç.
8. Çözemediğin bir sorun çıkarsa: önce 2-3 farklı kaynaktan (resmi dokümantasyon, GitHub issue,
   Stack Overflow) online araştır. Yine çözemezsen `COZULEMEYEN_SORUNLAR.md`'ye gerekçesiyle yaz,
   o modülü `BLOKE` işaretle, bir sonraki modüle geç — takılıp kalma.

## Sert kısıtlar

- **Tek seferde EN FAZLA 1 modül işle.** Modül çok büyükse (örn. 1000+ satırlık tek dosya) alt
  parçalara böl ve tabloya alt satır olarak ekle, ama yine de tek modülün sınırları içinde kal.
- **Kendi başına yeni alt-agent/alt-görev spawn etme** — bu prompt zaten tek bir çalıştırma birimi
  için tasarlandı; iç içe paralel ajan açman context'i gereksiz büyütür ve birbirini bekleme
  döngüsüne sokar.
- **Build almadan "çözüldü" deme.** Bir UI/ayar değişikliğini kapatmadan önce en az derleme
  doğrulaması şart; ekran değişikliğiyse mümkünse emülatör/cihaz screenshot'ı ile doğrula.
- **Dead code = kırmızı bayrak.** Bir fonksiyon/state 0-caller ise ve silmiyorsan, günlükte NEDEN
  silmediğini (örn. "dokümante edilmiş gelecek iş", "aktif yazma yolu var, veri kaybı riski")
  açıkça yaz — sessizce atlama.
- **Bu proje için CLAUDE.md ve MANAGEMENT\LEARNINGS.md dosyalarını da oku** — özellikle
  LEARNINGS.md'deki "D240" bölümü (halüsinasyon denetimi kuralları) ve "Kritik Mimari Tuzaklar"
  bölümü (KeywordDatabase duplicate kategori riski, encoding sorunları, Room migration şablonu vb.)
  bu projeye özgü tuzakları listeler — tekrar düşmemek için oku.

## Modül sırası (KOD_TARAMA_DURUM.md ile senkron kalmalı — asıl kaynak orası)

M1-M5 tamamlandı (utils/prefs, Ayarlar ekranları, launcher çekirdek, launcher bileşenler,
launcher/hero). Sıradaki adaylar: M6 (domain/: models, AppClassifier, KeywordDatabase,
InsightEngine), M7 (data/: DAO, Database, repository, migration, FTS), M8 (service/worker/receiver),
M9 (Aktiviteler + navigasyon), M10 (global ölü kod süpürmesi — detekt raporu), M11 (res/ tutarlılık
— strings, tema, hardcoded metin), M12 (araç/altyapı onarımı — check_duplicates.py, bayat yollar).

**Gerçek durumu her zaman `MANAGEMENT\KOD_TARAMA_DURUM.md` tablosundan oku — yukarıdaki liste sadece
bağlam içindir, tabloyla çelişirse tablo kazanır.**

---

## Notlar (Antigravity'ye özel)

- Bu prompt Claude Code oturumunda kurulmuş bir döngünün paralel/alternatif yürütücüsüdür. İki
  ajan aynı checkpoint dosyasını (`KOD_TARAMA_DURUM.md`) paylaşıyor — **çakışmayı önlemek için
  çalıştırmadan hemen önce `git pull` yap**, modülü seçmeden önce dosyanın en güncel halini oku.
  Eğer seçtiğin modül başka bir oturum tarafından az önce `TAMAM` yapılmışsa, bir sonrakine geç.
- Commit atmadan önce `git status` ile hangi dosyaların değiştiğini gör, sadece ilgili dosyaları
  stage'le (`git add -A -- ':!.vscode'` deseni kullanılabilir, `.vscode/` ve kişisel dosyalar
  commit'e girmemeli).
- Amaç token/maliyet tasarrufu olduğu için: gereksiz büyük dosya okumalarından kaçın, `grep`/arama
  ile hedefli git, tüm dosyayı satır satır okumak yerine ilgili fonksiyon/bloğu bul.
