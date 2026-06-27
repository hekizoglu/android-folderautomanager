# QA Öncelik Matrisi

> Launcher uygulaması için hata yakalama öncelik puanlaması.

| Katman | Puan | Açıklama |
|--------|------|----------|
| Crash / ANR izleme | 10 | Kullanıcıdaki gerçek çökme — en kritik |
| UI ve launcher akış testleri | 9 | Home, drawer, klasör, arama — temel işlev |
| Android Lint + statik analiz | 9 | Derleme öncesi hata — erken yakalama |
| Görsel regresyon testi | 8 | Tasarım bozulması — kullanıcı deneyimi |
| Performans / startup testi | 8 | Launcher açılış hızı — algı kalitesi |
| Güvenlik / izin analizi | 7 | Hassas veri ve izin — Play policy |
| AI kod inceleme | 6 | Yardımcı — otomasyona destek |

---

## Önceliklendirme

Puan 9+: Her değişiklikte çalıştırılmalı.
Puan 8: Günlük/PR bazında.
Puan 7: Haftalık/ön dağıtımda.
Puan 6: İsteğe bağlı.

---

*Oluşturma: 2026-06-27*
