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
| 2026-06-28 | Hüseyin | **Onboarding Ayar Sihirbazı** — İlk kurulumda tüm ayarları sırayla sor, her birine açıklama ekle. QUICK_SETTINGS adımı eklendi (SET_LAUNCHER→DONE arası). Tüm toggle'lar özet kartta gösteriliyor, tek tıkla kaydediliyor. (Kullanıcı Değeri:5 · Uygulanabilirlik:4 · Bağımlılık Riski:2 · Etki:4 = 15 puan ⭐) | [TAMAMLANDI D172] |
| 2026-06-28 | Hüseyin | **Akşam Önerisi Algoritma Açıklaması** — Ayarlar > Öneriler bölümüne kullanım örüntüsüne göre nasıl öneri verdiği açıklaması ekle | [TAMAMLANDI D146] |
| 2026-06-28 | Hüseyin | **Widget Auto-Resize** — Widget alanı ekran en/boy oranına göre otomatik yükseklik ayarı | [TAMAMLANDI D147] |
| 2026-06-28 | Hüseyin | **claude-code-templates mobile-design skill** — Tablet/foldable desteği planlandığında ekle (şimdi marginal fayda) | Bekliyor |
| 2026-06-28 | Hüseyin | **Tablet Desteği** — Adaptive layout: 2 sütun klasör grid (tablet), side panel AllAppsDrawer, foldable hinge desteği. `WindowSizeClass` API kullan. mobile-design skill ile birlikte ekle. (Kullanıcı Değeri:5 · Uygulanabilirlik:3 · Bağımlılık Riski:3 · Etki:5 = 16 puan ⭐) | [TAMAMLANDI D181] |
| 2026-06-28 | Hüseyin | **Yedek Karşılaştırma + Eksik Uygulama Tespiti** — Yedekten geri dönünce eksik uygulamaları listele; liste kopyalanabilir + Play Store derin linkleri göster; "Hepsini Sırayla Aç" butonu ile tek tek yükleme. İki cihaz arası senkron: yedek dosyası paylaş → diğer cihazda eksikler listelenir. Tam otomatik indirme mümkün değil (Android güvenlik kısıtı) ama derin link akışı kullanıcıyı çok hızlandırır. (Kullanıcı Değeri:5 · Uygulanabilirlik:4 · Bağımlılık Riski:1 · Etki:4 = 14 puan 🟡) | Bekliyor |

---

| 2026-06-28 13:03 | Cron #13 | **Compose Compiler Stabilite Raporu** — `build.gradle.kts`'ye `composeCompiler { reportsDestination = ... }` ekle, her build'de hangi composable'ların unstable/skippable olmadığını raporla. compose-expert skill ile entegre, E13/E14 benzeri hataları build aşamasında yakalar. (KV:3 · U:5 · BR:1 · EA:3 = **12 puan 🟡**) | Bekliyor |
| 2026-06-28 14:03 | Meta-audit | **LEARNINGS→audit.ps1 Coverage Matrix** — Her LEARNINGS hatasina (E1-E14) karsi audit.ps1 kurali var mi matrix'i olustur. E9 (@Volatile→AtomicBoolean) eksikti, CE6 eklendi. Sistematik coverage kontrolu her 6 dongude bir otomatik yapilsin. (KV:3 · U:5 · BR:1 · EA:3 = **12 puan 🟡**) | CE6 eklendi, coverage %93 (13/14) |
| 2026-06-28 14:14 | Meta-audit | **E8 Guard Pattern Audit Kurali (CE7)** — `if (map.isNotEmpty())` guard pattern'ini tara. E8'de bildirim badge temizlemede guard kaldirilmisti. Benzer pattern'ler (bos koleksiyon guard'i gereksiz yerde) taranip false-positive olmayanlar isaretlenmeli. (KV:2 · U:4 · BR:2 · EA:2 = **10 puan 🟡**) | Bekliyor |

| 2026-06-28 | Rakip | **Nova Crash Koruması** — Nova 8.2.8'de yeni cihazda crash sorunu var. AppOrganizer'da ilk açılışta crash olursa safe-mode (varsayılan ayarlarla başlat) mekanizması ekle. (KV:4 · U:3 · BR:2 · EA:3 = **12 puan 🟡**) | Bekliyor |
| 2026-06-28 | Rakip | **Gesture/Multitasking Uyumluluğu** — Tüm 3.parti launcher'larda gesture + recents çakışması var. Android 16 iyileştirmelerini takip et, split-screen testleri ekle. (KV:5 · U:3 · BR:3 · EA:5 = **16 puan ⭐**) | [TAMAMLANDI D180] |
| 2026-06-28 | Rakip | **Pixel Launcher Eksikleri Bizde Var** — Icon pack (✅), yedekleme (✅), klasör (✅), gesture özelleştirme (✅), app drawer kategorileri (✅). Pixel'da olmayan 6 özellik bizde var → Play Store listing'te vurgula. (KV:4 · U:5 · BR:1 · EA:4 = **14 puan 🟡**) | Bekliyor |
| 2026-06-28 | Rakip | **İkon Boyutu Özelleştirme** — Nothing Launcher'da en çok şikayet edilen şey. AppOrganizer'a icon scale ayarı ekle (Ayarlar > Görünüm). (KV:4 · U:4 · BR:1 · EA:2 = **11 puan 🟡**) | Bekliyor |
| 2026-06-28 | Rakip | **Safe Mode / Crash Recovery** — Nova'nın yaşadığı crash→downgrade senaryosuna karşı. AppOrganizer'da son 3 build'ten birine otomatik rollback. (KV:5 · U:3 · BR:3 · EA:4 = **15 puan ⭐**) | [TAMAMLANDI D176] |
| 2026-06-28 | Araştırma | **Google Drive Cross-Device Sync (⭐17p)** — WhatsApp benzeri `appDataFolder` API ile otomatik yedekleme. 3 aşama: (1) BackupWorker→Drive upload, (2) Yeni cihazda otomatik restore, (3) FCM push ile canlı sync. Rakiplerin %80'inde yok, en büyük farklılaştırıcı özellik. (KV:5 · U:4 · BR:3 · EA:5 = **17 puan ⭐**)| [TAMAMLANDI D178] |
| 2026-06-28 | Araştırma | **Android 15/16 Edge-to-Edge Tam Uyum** — Android 15'te zorunlu edge-to-edge. Mevcut durum kontrol edilip `WindowInsets` tüm ekranlarda doğru mu taranmalı. Play Store reddi riski. (KV:5 · U:4 · BR:2 · EA:5 = **16 puan ⭐**) | [TAMAMLANDI D177] |
| 2026-06-28 | Araştırma | **Launcher Crash Rate İzleme** — Nova'nın %15 crash rate skandalı. AppOrganizer'a Crashlytics + ANR izleme + otomatik safe mode. Kullanıcı güveni için kritik. (KV:4 · U:4 · BR:2 · EA:4 = **14 puan 🟡**) | Bekliyor |
| 2026-06-28 | Meta #3 | **Android 16 File Permission Audit Kurali (CE7)** — MojoLauncher'da Android 16'da app data folder'a yazma izni kirildi. `getExternalFilesDir` / `getFilesDir` kullanimini tara, Android 16 breaking change'lerine karsi kural ekle. (KV:3 · U:4 · BR:1 · EA:3 = **11 puan 🟡**) | Bekliyor |

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda | Son güncelleme: 2026-06-28 14:30*
*🏆 Piyasa Puani: 7.8/10 | Tahmini 1 yillik indirme: 5K-15K (organik, niche launcher)*
