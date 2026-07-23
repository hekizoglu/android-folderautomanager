# AppOrganizer — Dokümantasyon İndeksi

> **Merkezi başvuru sayfası.** Tüm dosyaların rolü ve okuş sırası.  
> **Kural:** İndekse göre oku; dosyalar birbirine bağlıdır.  
> **Son güncelleme:** 2026-07-23 Orkestra Şefi (Professional Restructure)

---

## 🔥 Acil: Şu An Oku

| # | Dosya | Rol | Okuş Sırası | Güncelleme |
|---|-------|-----|------------|-----------|
| 1 | **MANAGEMENT/TASKS.md** | Merkezi görev, kanıt, bağımlılık | ÖNCE | 2026-07-24 |
| 2 | **MANAGEMENT/ROADMAP.md** | Faz, bağımlılık, ilerleme | Sonra | 2026-07-24 |
| 3 | **CLAUDE.md** | Çalışma kuralları, Agent, Telegram | Görevde | 2026-07-23 |

---

## 📋 Yönetim & Raporlama (MANAGEMENT/ klasörü)

| Dosya | Rol | Güncelleme Sırası |
|-------|-----|------------------|
| **MANAGEMENT/HISTORY.md** | Tamamlanan döngüler, kanıtlar, değişik dosyalar | Her P0 görevi sonrası |
| **MANAGEMENT/ROADMAP.md** | Faz durum, bağımlılıklar, kalan efor | TASKS.md bir satırı güncellendikçe |
| **MANAGEMENT/TASKS.md** | Merkezi görev, kanıt, bağımlılık | Her görev tamamlandıkça |
| **MANAGEMENT/LEARNINGS.md** | Tuzak, mimari karar, SOP (rare) | Yeni bulgu = ortak = CLAUDE.md promote |

---

## 🛠️ Referans & Teknik (Görevde İhtiyaç Halinde)

| Dosya | İçerik | Okuş Zamanı |
|-------|--------|-----------|
| **LEARNINGS.md** | Kotlin cast smart, Room migration, emoji/icon cache, Android 15 edge-to-edge | P0.3 (klasör), P0.5 (perf) |
| **CLAUDE.md § 5** | Mimari tuzaklar (Scope snapshot, flow ısı, derivedStateOf) | P0.1 (arama), P0.2 (keyboard) |
| **CLAUDE.md § 4** | Araçlar (Gradle, emulator, Telegram, ADB) | Build/test zamanı |
| **AGENTS.md** | Kurulu agent profilleri | Agent spawn zamanı |

---

## 🎬 Proje Tanıtım (Yerleşik olmayan / Referans)

| Dosya | İçerik | Oku |
|-------|--------|-----|
| **README.md** | Genel tanıtım | Eski (güncelle) |
| **SETUP.md** | IDE/Gradle kurulumu | Yalnız setup zamanı |

---

## 📁 Proje Hiyerarşisi (Professional v2, 2026-07-24)

```
AppOrganizer/
├── 📄 README.md                      ← Başlangıç (proje tanıtım)
├── 📄 INDEX.md                       ← Merkezi indeks (SEÇ BURAYI)
├── 📄 CLAUDE.md                      ← Çalışma kuralları
│
├── 📁 MANAGEMENT/                    ⭐ YÖNETİM BELGELERİ
│   ├── TASKS.md                      ← Görev merkezesi
│   ├── ROADMAP.md                    ← Faz/ilerleme
│   ├── HISTORY.md                    ← Tamamlanan döngüler
│   ├── LEARNINGS.md                  ← Mimari tuzaklar
│   └── DECISIONS.md                  ← Ürün kararları
│
├── 📁 docs/                          ⭐ TEKNIK & RELEASE DOCS
│   ├── setup/                        ← Kurulum rehberi
│   ├── architecture/                 ← Mimari dokümantasyon
│   ├── performance/                  ← Performans ölçümleri
│   ├── release/                      ← Release guide + Checklist
│   ├── qa/                           ← Test & QA belgeleri
│   ├── reports/                      ← Final raporlar
│   └── store_assets/                 ← Play Store görselleri
│
├── 📁 scripts/                       ⭐ ARAÇLAR & OTOMASYON
│   ├── setup/                        ← Initialization scripts
│   ├── build/                        ← Build tools
│   ├── test/                         ← Testing scripts
│   ├── monitor/                      ← Observability & audit
│   ├── maintenance/                  ← Cleanup & maintenance
│   └── README.md                     ← Script katalogu
│
├── 📁 archive/                       ← Eski dökümanlar
│   ├── build_logs/                   ← Build history
│   ├── tablet_tests/                 ← Eski test belgeleri
│   └── ...
│
├── 📄 COZULEMEYEN_SORUNLAR.md       ← Blokeli dış görevler
│
├── 📁 app/                           ← KAYNAK KOD (değişiklik yok)
├── 📁 benchmark/                     ← Baseline Profile tests
├── 📁 gradle/                        ← Gradle wrapper
├── 📁 .github/                       ← CI/CD workflows
└── ...                               (Build dirs, git, vscode, etc)
    └── ...
```

---

## 🔄 Güncelleme Sırası (Görev İçin)

### Görev Başında
1. TASKS.md açılacak görev kısmını oku
2. ROADMAP.md ilgili faz bölümünü oku (bağımlılıklar)
3. Dosya referansı okunmalıysa LEARNINGS.md sembol ara

### Görev Sırasında
- Kod yazıyorsan: CLAUDE.md § 3 (temel kurallar)
- Hata alırsan: LEARNINGS.md § 5 (tuzaklar)
- Agent şefi iseniz: AGENTS.md (profiller)

### Görev Sonunda
1. Kanıt test etme: TASKS.md görev sırasını kontrol et
2. Commit + git push
3. TASKS.md satırını `[x]` işaretle
4. HISTORY.md güncelle (3 satır özet)
5. ROADMAP.md ilgili satırı güncelle (durum)

---

## 📊 Dosya Yönetim Kuralları

### ✅ Hangi Dosya Neyi Takip Ediyor?

| Soru | Cevap | Dosya |
|------|-------|-------|
| "Bu hafta ne yapılıyor?" | Görevler + kanıt | **TASKS.md** |
| "Bağımlılıklar neler?" | Faz zinciri | **ROADMAP.md** |
| "Bu tuzak daha önce çözüldü mü?" | Mimari karar + SOP | **LEARNINGS.md** |
| "Geçen hafta ne yapıldı?" | Tamamlanan + dosya değişiklikleri | **HISTORY.md** |
| "Agent seçimi nasıl yapılır?" | Agent profilleri | **AGENTS.md** |
| "Cihaza ihtiyaç var ama bağlantı yok?" | Engel neden/cihaz nedir | **COZULEMEYEN_SORUNLAR.md** |
| "Tablet test adımları?" | Test sahneleri + fail case'ler | **docs/testing/tablet/** |

---

## 🚀 Orkestra Şefi Kontrol Listesi

Her görev başında:

- [ ] TASKS.md P0/P1/P2 görevlerini oku
- [ ] Bağımlılıklar tamamlandı mı? (ROADMAP.md bağımlılık zinciri)
- [ ] Kararlar var mı? (TASKS.md "Karar bekleniyor")
- [ ] Dosya referansları doğru? (INDEX.md → LEARNINGS/CLAUDE)
- [ ] Test yapılacak? (TASKS.md "Kanıt Gereksinimi")
- [ ] Commit mesajı şablonu? (TASKS.md "Commit mesajı:")

Görev sonunda:
- [ ] `[x]` işareti + mesaj kopyala
- [ ] HISTORY.md > ROADMAP.md sırasını tut
- [ ] Eski dosyaları archive'a at
- [ ] İndeksi güncelle

---

## 🔗 Hızlı Erişim (Tarayıcı Bookmark)

```
📑 TASKS.md       P0 görev + kanıt
📑 ROADMAP.md     Faz zinciri
📑 LEARNINGS.md   Tuzak referans
📑 CLAUDE.md      Kurallar
📑 HISTORY.md     Tamamlanan
📑 AGENTS.md      Agent seçimi
```

---

## 🎯 Son Durum (2026-07-23 23:00 UTC)

- **Kullanım sürüsü:** Orkestra Şefi moduna geçiş
- **Görevler:** TASKS.md'de merkezi olarak yönetiliyor (P0: 6 + karar 2, P1: 6, P2: 5)
- **Engeller:** Hiçbiri (Widget/Dock seçimi bekleniyor)
- **Sonraki:** P0.1 başlat (sabit global arama)

---

**İndeks kuralı:** Bu dosya yalnızca hafifçe güncellenir; ağır değişiklikler bileşen dosyalarında (TASKS.md, ROADMAP.md) yapılır.

