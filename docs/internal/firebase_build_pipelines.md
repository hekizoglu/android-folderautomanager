# Firebase Derleme Hatlari

## Android Code CI — No Firebase

- Otomatik calisir.
- Firebase secret gerektirmez.
- Telefon testi icin APK uretmez.
- Kod ve unit test dogrulamasi yapar.

## Firebase Device Test APK

- Manuel calisir.
- `GOOGLE_SERVICES_JSON` secret gerektirir.
- Telefona kurulacak Firebase etkin APK'yi uretir.
- Artifact adi `app-debug-FIREBASE-ENABLED` olur.

## Android QA — No Firebase

- Manuel QA kontrollerini calistirir.
- Firebase baglantisi test etmez.
- Build, lint, unit test, detekt ve varsa ktlint raporlarini uretir.
