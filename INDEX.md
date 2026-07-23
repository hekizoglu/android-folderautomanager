# AppOrganizer — Dokümantasyon İndeksi

> **Merkezi başvuru sayfası.** Tüm dosyaların rolü ve okuş sırası.  
> **Kural:** İndekse göre oku; dosyalar birbirine bağlıdır.  
> **Son güncelleme:** 2026-07-23 Orkestra Şefi (Professional Restructure)

---

## 🔥 Acil: Şu An Oku

| # | Dosya | Rol | Okuş Sırası | Güncelleme |
|---|-------|-----|------------|-----------|
| 1 | **TASKS.md** | Merkezi görev, kanıt, bağımlılık | ÖNCE | 2026-07-23 |
| 2 | **ROADMAP.md** | Faz, bağımlılık, ilerleme | Sonra | 2026-07-21 |
| 3 | **CLAUDE.md** | Çalışma kuralları, Agent, Telegram | Görevde | 2026-07-23 |

---

## 📋 Yönetim & Raporlama

| Dosya | Rol | Güncelleme Sırası |
|-------|-----|------------------|
| **HISTORY.md** | Tamamlanan döngüler, kanıtlar, değişik dosyalar | Her P0 görevi sonrası |
| **ROADMAP.md** | Faz durum, bağımlılıklar, kalan efor | TASKS.md bir satırı güncellendikçe |
| **LEARNINGS.md** | Tuzak, mimari karar, SOP (rare) | Yeni bulgu = ortak = CLAUDE.md promote |
| **FİKİRLER.md** | Puanlı fikirler, backlog | Arşiv (P0 bittikçe TASKS → HISTORY → temizle) |

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

## 📁 Dosya Hiyerarşisi (Fiziksel)

```
AppOrganizer/
├── INDEX.md                          ← Başlangıç (buradan başla)
├── TASKS.md                          ← Görev merkezesi
├── ROADMAP.md                        ← Faz/ilerleme
├── HISTORY.md                        ← Tamamlanan
├── CLAUDE.md                         ← Kurallar
│
├── LEARNINGS.md                      ← Mimari referans (ender güncellenme)
├── AGENTS.md                         ← Agent profilleri
├── FİKİRLER.md                       ← Backlog (arşiv)
│
├── docs/
│   ├── testing/tablet/               ← Tablet test belgeleri (NEW)
│   │   ├── R1_TEST_READINESS.md
│   │   ├── TABLET_TEST_GUIDE.md
│   │   └── TABLET_TROUBLESHOOTING.md
│   ├── release/                      ← Release guide (NEW)
│   │   └── RELEASE_BUILD_GUIDE.md
│   └── architecture/                 ← Mimari (NEW)
│       └── RESPONSIVE_LAYOUT_INTEGRATION.md
│
├── COZULEMEYEN_SORUNLAR.md          ← Cihaz/hesap blokeli işler
├── harcananvakit.md                  ← Döngü zaman logu
│
└── archive/                          ← Eski (temizlik)
    ├── README.md.bak
    ├── NOW.md.bak
    ├── DECISIONS.md.bak
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

