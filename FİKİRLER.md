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

*(Şu an bekleyen yüksek puanlı madde yok — S1/S2/K1 tamamlandı, bkz. HISTORY.md Döngü 207-208.)*

---

## 🟡 Orta Öncelik (12-14p)

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-07-06 | Fable D199 | **Onboarding sonrası "ilk izlenim" emülatör testi** — onboarding sonrası ilk açılış deneyimi (RoleManager dialog + klasör oluşturma süresi + ilk render) test edilmedi. Her 18 döngüde bir tam teste eklensin. (KV:4 · U:5 · BR:2 · EA:3 = **14p**) | Bekliyor |
| 2026-07-07 | Rekabet (Smart Launcher/Niagara) | **Home ekranı/dock'ta kullanım sıklığına göre otomatik sıralama** — Niagara pattern. `lastUsedTimestamp` alanı zaten Room'da mevcut (MIGRATION_3_4), sadece dock/klasör içi sıralama mantığı eksik. (KV:4 · U:3 · BR:3 · EA:3 = **13p**) | Bekliyor |

---

## ⏸ Beklet (≤11p)

| Tarih | Kaynak | Madde | Puan |
|-------|--------|-------|------|
| 2026-06-29 | Rekabet | **Online App Category DB** — Opsiyonel, gizlilik riski yüksek. Lokal → keyword → kullanıcı override → en son online. | 10p |
| 2026-06-29 | Hüseyin | **claude-code-templates mobile-design skill** — Tablet/foldable desteği planlandığında ekle. | 9p |
| 2026-07-07 | Rekabet (Smart Launcher/Niagara) | **Ayarlar'da grid yoğunluğu (satır/sütun) manuel slider'ı** — Smart Launcher'ın Fluid Grid pattern'i; mevcut grid sabit mi kontrol edilmeli, sabitse ayarlanabilir hale getirilsin. (KV:3 · U:3 · BR:2 · EA:3) | 11p |

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
| Smart Launcher | Otomatik kategori atama vaadi (en yakın rakip) | Fluid Grid yoğunluk ayarı, gesture bar |
| Niagara Launcher | Arama-öncelikli minimal ana ekran fikri | Kullanım sıklığına göre otomatik dock sıralama |

**Hedef cümle:** "AppOrganizer, uygulamalarını otomatik klasörleyen; kullanım alışkanlığına göre dock, arama ve önerileri akıllandıran privacy-first Android launcher'dır."

---

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda | Son güncelleme: 2026-07-07*
*Not: 2026-06-29 rekabet analizi [TAMAMLANDI] maddeleri HISTORY.md Tamamlananlar Arşivi'ne taşındı (v1.2.0 döngüsü). S1/S2/K1 (Döngü 207-208) tamamlandı, HISTORY.md'ye taşındı.*
