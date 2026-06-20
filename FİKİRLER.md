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
| 2026-06-20 | Denetim #13 | **FCM push mimari kararı LEARNINGS.md'ye eklenmeli** — `AppFirebaseMessagingService.kt` + `AppOrganizerApp.kt` FCM init belgelenmedi | [TAMAMLANDI] |
| 2026-06-16 | ROADMAP | **QUERY_ALL_PACKAGES Play Store beyan formu** — göndermeden önce zorunlu, aksi halde APK reddedilir | Bekliyor ⚠️ |

---

## 🟡 Orta Öncelik

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-20 | Denetim #11 | **Onboarding adım sırası güncellenmeli** — CLASSIFY_MODE adımı eksik, CLAUDE.md "14+2" diyor ama LEARNINGS.md 14 adım | Bekliyor |
| 2026-06-20 | Denetim #14 | **Merge conflict AppClassifier kural CLAUDE.md §5'e promote** — 4+ tekrar, eşik geçildi | [TAMAMLANDI] |
| 2026-06-16 | Döngü 86 | **AllAppsDrawer/FolderSheet dark mode regresyon kontrolü** — D91 fix sonrası FCM commit tekrar değiştirdi | Bekliyor |
| 2026-06-15 | ROADMAP | **Android 14 NotificationListenerService gerçek cihaz testi** | Bekliyor |
| 2026-06-15 | Döngü 43 | **AppNotificationListenerService ilk açılışta restart** — gerçek cihaz test gerekli | Bekliyor |
| 2026-06-15 | ROADMAP | **Firebase Crashlytics API kurulumu** — `google-services.json` + service account | Bekliyor |
| 2026-06-15 | ROADMAP | **Privacy Policy sayfası** — GitHub Pages tek HTML, içerik onayı gerekli ⚠️ | Bekliyor |
| 2026-06-15 | ROADMAP | **Screenshots** — Pixel 6 emülatörü, light + dark mode | Bekliyor |
| 2026-06-15 | ROADMAP | **Content rating anketi** — Play Store ⚠️ | Bekliyor |
| 2026-06-16 | ROADMAP | **BLUR-4: Gerçek cihaz testi** — blur performansı + API 26 uyumu | Bekliyor |
| 2026-06-16 | ROADMAP | **`cycle.ps1` uçtan uca test** — build → push → Telegram | Bekliyor |

---

## 🟢 Düşük Öncelik

| Tarih | Kaynak | Madde | Durum |
|-------|--------|-------|-------|
| 2026-06-15 | Döngü 43 | **AllApps double-tap emülatörde doğrulanamadı** — gerçek cihaz testi | Bekliyor |
| 2026-06-15 | Döngü 41 | **Üretici kategorileri gerçek cihaz testi** — 9 yeni kategori, onboarding'den "üreticiye göre" | Bekliyor |
| 2026-06-16 | ROADMAP | **Hilt DI kurulumu** — manuel `new()` çağrılarını temizle | Bekliyor |
| 2026-06-16 | ROADMAP | **Unit test coverage** — ViewModel'ler MockK testleri | Bekliyor |
| 2026-06-16 | ROADMAP | **Dark mode tam uyum audit** | Bekliyor |
| 2026-06-16 | ROADMAP | **Multi-language support (TR/EN)** | Bekliyor |

---

## ⚪ Fikirler (Tartışılacak)

| Tarih | Kaynak | Fikir | Not |
|-------|--------|-------|-----|
| 2026-06-16 | ROADMAP | **Ayarlar — Kullanıcı Talep/Öneri Formu** — SettingsScreen'e "Talep Gönder" butonu | Tartışılacak |
| 2026-06-15 | ROADMAP | **AppClassifier'ı JSON asset'e böl** — 3717 paketlik mapOf şişiyor; `assets/app_categories.json` + runtime parse | Tartışılacak |
| 2026-06-15 | ROADMAP | **Kendi sunucu API'si** — `packageName → category` endpoint, APK güncellemeden DB güncellenebilir | Tartışılacak |
| 2026-06-16 | ROADMAP | **Akıllı Uygulama Önerileri (30dk)** — kullanım alışkanlığına göre değişen öneri satırı | Tartışılacak |
| 2026-06-16 | ROADMAP | **Wear OS companion app** | Uzun vade |
| 2026-06-16 | ROADMAP | **Tablet layout (large screen)** | Uzun vade |
| 2026-06-16 | ROADMAP | **Widget ekranı genişletme** — resize, çoklu sayfa | Uzun vade |
| 2026-06-16 | ROADMAP | **Aider repo-map CBM entegrasyon testi** | Tartışılacak |
| 2026-06-16 | ROADMAP | **Greptile API PR review otomasyonu** | Tartışılacak |

---

## 📋 Son Eklenenler (Bu Konuşmadan)

| Tarih | Madde |
|-------|-------|
| 2026-06-20 | FİKİRLER.md sistemi oluşturuldu — artık tüm yeni görevler/fikirler buraya |
| 2026-06-20 | ROADMAP.md donduruldu — yeni ekleme yapılmayacak |

---

*Oluşturulma: 2026-06-20 | Güncelleyen: Claude her döngü sonunda*
