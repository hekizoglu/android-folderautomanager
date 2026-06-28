# FİKİRLER.md — AppOrganizer Fikir & Görev Havuzu

> Yeni özellik fikirleri, döngüden gelen görevler, backlog adayları buraya eklenir.
> ROADMAP.md artık değiştirilmez — yeni her şey buraya gelir.
> Telegram onayı alındıktan sonra fikir hayata geçirilir.

---

## Nasıl Kullanılır

- Claude her döngü sonunda yeni fikri/görevi buraya ekler
- Her madde: tarih + kaynak + öncelik + kısa açıklama
- Onay gelince: `[ONAYLANDI 2026-xx-xx]` etiketi + uygulama başlar
- Tamamlanınca: `[TAMAMLANDI]` etiketi

**Öncelik:** 🔴 Kritik · 🟡 Orta · 🟢 Düşük · ⚪ Fikir (tartışılacak)

---

## 🔴 Kritik

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-16 | ROADMAP | **QUERY_ALL_PACKAGES Play Store beyan formu** — göndermeden önce zorunlu, aksi halde APK reddedilir | Bekliyor ⚠️ |

---

## 🟡 Orta Öncelik

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-21 | Denetim | **Test altyapısı — Maestro** — 3 flow oluşturuldu: HomeScreen, AllAppsDrawer, Ayarlar navigasyon. `.maestro/` klasöründe. Kurulum: `maestro test .maestro/` (Kullanıcı Değeri:4 · Uygulanabilirlik:3 · Bağımlılık Riski:2 · Etki:3 = 12 puan 🟡) | [TAMAMLANDI D153] |
| 2026-06-28 | Hüseyin | **Onboarding Ayar Sihirbazı** — İlk kurulumda tüm ayarları sırayla sor, her birine açıklama ekle. Mevcut onboarding'e ek bir "Ayar Tur" akışı olarak eklenebilir. (Kullanıcı Değeri:5 · Uygulanabilirlik:4 · Bağımlılık Riski:2 · Etki:4 = 15 puan ⭐) | Bekliyor |
| 2026-06-28 | Hüseyin | **Akşam Önerisi Algoritma Açıklaması** — Ayarlar > Öneriler bölümüne kullanım örüntüsüne göre nasıl öneri verdiği açıklaması ekle | [TAMAMLANDI D146] |
| 2026-06-28 | Hüseyin | **Widget Auto-Resize** — Widget alanı ekran en/boy oranına göre otomatik yükseklik ayarı | [TAMAMLANDI D147] |
| 2026-06-28 | Hüseyin | **claude-code-templates mobile-design skill** — Tablet/foldable desteği planlandığında ekle (şimdi marginal fayda) | Bekliyor |
| 2026-06-28 | Hüseyin | **Tablet Desteği** — Adaptive layout: 2 sütun klasör grid (tablet), side panel AllAppsDrawer, foldable hinge desteği. `WindowSizeClass` API kullan. mobile-design skill ile birlikte ekle. (Kullanıcı Değeri:5 · Uygulanabilirlik:3 · Bağımlılık Riski:3 · Etki:5 = 16 puan ⭐) | Bekliyor |
| 2026-06-28 | Hüseyin | **Yedek Karşılaştırma + Eksik Uygulama Tespiti** — Yedekten geri dönünce eksik uygulamaları listele; liste kopyalanabilir + Play Store derin linkleri göster; "Hepsini Sırayla Aç" butonu ile tek tek yükleme. İki cihaz arası senkron: yedek dosyası paylaş → diğer cihazda eksikler listelenir. Tam otomatik indirme mümkün değil (Android güvenlik kısıtı) ama derin link akışı kullanıcıyı çok hızlandırır. (Kullanıcı Değeri:5 · Uygulanabilirlik:4 · Bağımlılık Riski:1 · Etki:4 = 14 puan 🟡) | Bekliyor |

---

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda | Son güncelleme: 2026-06-28*
