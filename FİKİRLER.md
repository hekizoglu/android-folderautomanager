# FİKİRLER.md — AppOrganizer Fikir & Görev Havuzu

> Yeni özellik fikirleri, döngüden gelen görevler, backlog adayları buraya eklenir.
> 15+ puan alanlar ROADMAP.md'ye de taşınır. Tamamlananlar → HISTORY.md arşiv.
> Telegram onayı alındıktan sonra fikir hayata geçirilir.

---

## Nasıl Kullanılır

- Claude her döngü sonunda yeni fikri/görevi buraya ekler
- Her madde: tarih + kaynak + öncelik + kısa açıklama
- Onay gelince: `[ONAYLANDI 2026-xx-xx]` etiketi + uygulama başlar
- Tamamlanınca: `[TAMAMLANDI]` etiketi → HISTORY.md'ye taşınır

**Öncelik:** 🔴 Kritik · ⭐ Yüksek Puanlı (15+) · 🟡 Orta (12-14) · 🟢 Düşük · ⚪ Fikir

---

## 🔴 Kritik

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-16 | ROADMAP | **QUERY_ALL_PACKAGES Play Store beyan formu** — göndermeden önce zorunlu, aksi halde APK reddedilir | Bekliyor ⚠️ |

---

## ⭐ Yüksek Puanlı (≥15p)

| Tarih | Puan | Madde | Durum |
|-------|------|-------|-------|
| 2026-07-07 | **18 ⭐** | **Birleşik "her şeyi ara" ana ekran araması (S1)** — tek çubuk: uygulama + kategori + klasör + rehber kişisi + dosya; "Uygulama/Klasör" sekmesi ana ekrandan kaldırılır. (KV:5 · U:4 · BR:4 · EA:5) | ROADMAP S1 — Bekliyor |
| 2026-07-07 | **16 ⭐** | **Rehber kişisi araması default etkin (S2)** — ana ekran aramasında kişiler de çıksın; izin yoksa sonuç grubunda "izin ver" kısayolu. (KV:4 · U:4 · BR:4 · EA:4) | ROADMAP S2 — Bekliyor |
| 2026-07-07 | **17 ⭐** | **KAPT → KSP geçişi (K1)** — Windows build kilitlerinin ana kaynağı KAPT; KSP ile kilitler biter, build ~%30 hızlanır, configuration cache açılabilir. (KV:4 · U:5 · BR:4 · EA:4) | ROADMAP K1 — Bekliyor |

---

## 🟡 Orta Öncelik (12-14p)

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-07-06 | Fable D199 | **Onboarding sonrası "ilk izlenim" emülatör testi** — onboarding sonrası ilk açılış deneyimi (RoleManager dialog + klasör oluşturma süresi + ilk render) test edilmedi. Her 18 döngüde bir tam teste eklensin. (KV:4 · U:5 · BR:2 · EA:3 = **14p**) | Bekliyor |
| 2026-07-07 | Fable | **Ölü kod: Room `search_history` tablosu + SearchHistoryDao kaldırma** — UI kullanmıyor, arama geçmişi SharedPreferences'ta (2 saat TTL). v13 migration gerekir. (KV:2 · U:5 · BR:3 · EA:4 = **14p**) | Bekliyor |
| 2026-07-07 | Fable | **CLAUDE.md sadeleştirme** — ~390 satır → ~250; rutinleşen bölümler LEARNINGS'e taşınır, her oturumda token tasarrufu. (KV:2 · U:5 · BR:2 · EA:5 = **14p**) | Bekliyor |

---

## ⏸ Beklet (≤11p)

| Tarih | Kaynak | Madde | Puan |
|-------|--------|-------|------|
| 2026-06-29 | Rekabet | **Online App Category DB** — Opsiyonel, gizlilik riski yüksek. Lokal → keyword → kullanıcı override → en son online. | 10p |
| 2026-06-29 | Hüseyin | **claude-code-templates mobile-design skill** — Tablet/foldable desteği planlandığında ekle. | 9p |

---

## 📊 Rekabet Pozisyonlama Özeti

| Rakip | Bizim aldığımız | Kalan fark |
|-------|----------------|------------|
| KISS | UsageScore + Smart Search fikri | KISS'in arama hızı (native C) |
| Lawnchair | Pixel hissi korunuyor | Icon pack, global search |
| mLauncher | Gesture fikri | Biometric lock |
| Neo | Backup/kategori fikri | Icon shape, drawer özelleştirme |
| Fossify | Privacy Center fikri | Tam offline mimari |
| Kvaesitso | Search provider mimarisi | Plugin sistemi |
| Pie Launcher | Quick Wheel fikri | Tam radyal nav |

**Hedef cümle:** "AppOrganizer, uygulamalarını otomatik klasörleyen; kullanım alışkanlığına göre dock, arama ve önerileri akıllandıran privacy-first Android launcher'dır."

---

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda | Son güncelleme: 2026-07-07*
*Not: 2026-06-29 rekabet analizi [TAMAMLANDI] maddeleri HISTORY.md Tamamlananlar Arşivi'ne taşındı (v1.2.0 döngüsü).*
