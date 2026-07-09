# Home Revizyon Karar Listesi

> Tarih: 2026-07-09
> Kapsam: ROADMAP U10 - acik kaynak launcher referanslariyla Home revizyonunu dar uygulama parcalarina bolmek.
> Dogrulama: Kod okuma + resmi/birincil kaynak incelemesi. Build veya cihaz testi calistirilmadi.

## Referans Ozeti

- Lawnchair: Pixel/Launcher3 cizgisini temel alir; ana gucu tanidik home deneyimi ve ozellestirme yuzeyidir. Kaynak: https://github.com/LawnchairLauncher/lawnchair
- Kvaesitso: search-focused launcher olarak konumlanir; global search ana navigasyon yuzeyidir. Kaynak: https://github.com/MM2-0/Kvaesitso
- AppOrganizer mevcut farki: otomatik klasorleme, privacy-first yerel arama ve akilli oneriler zaten urun vaadinin merkezinde.

## Mevcut Kod Durumu

- `HomeScreen.kt`: Home yuzeyinde klasor grid/pager, widget alani, favorites, recent apps, oneriler, ticker, assistant cards ve search bar konum ayarlari ayni composable akista toplanmis.
- `AllAppsDrawer.kt`: All Apps icinde arama, gecmis, kaynak filtreleri, quick filter ve siralama chipleri mevcut.
- `SettingsHomeScreenSection.kt`: Home yuzeylerinin buyuk kismi toggle ile acilip kapatilabiliyor.
- `AppPrefs.kt`: Home davranislari cok sayida preference ile yonetiliyor; revizyon tek buyuk rewrite olarak degil, yuzey bazli kucuk degisikliklerle ilerlemeli.

## Kalacaklar

1. Otomatik klasorleme ana Home vaadi olarak kalacak.
2. All Apps aramasi ve kaynak filtreleri ana arama deneyiminin guvenli yolu olarak kalacak.
3. Favorites ve recent apps yuzeyleri opsiyonel kalacak; varsayilan kalabaligi artirmayacak sekilde toggle arkasinda tutulacak.
4. Widget alani opsiyonel kalacak; ilk kurulumda kullaniciya kisa aciklama ile sunulacak.
5. Privacy-first metinleri ve permission center ayarlarda kalacak; Home ilk ekranina surekli izin baskisi eklenmeyecek.

## Yeniden Gruplanacaklar

1. Search-first mod: klasor istemeyen kullanici icin Home ustunde sade search bar + favorites/recent akisi ayri mod olarak tasarlanacak.
2. Klasor-first mod: mevcut otomatik klasor grid/pager korunacak; search bar konumu TOP/BOTTOM ayariyla kalacak.
3. Oneriler: Smart/Niagara referansi dogrultusunda kullanim sikligina dayali oneriler Home icinde tek yuzeyde toplanacak; dock/favorites/recent ile ayni anda tekrar eden liste hissi azaltacak.
4. Settings: Home yuzey toggle'lari "Temel", "Arama", "Oneriler", "Gorsel" alt gruplarina ayrilacak.

## Gidip Ayrilacaklar

1. Home uzerinde ayni anda cok fazla bilgi yuzeyi acik varsayilmamali.
2. Ilk acilista launcher secimini tekrar tekrar zorlayan akislardan kacinilmali; kullanici "simdi degil" dediyse Settings'ten manuel devam etmeli.
3. Search bar, Google web arama ve yerel app search ayni anlamsal yuzey gibi gosterilmemeli; yerel arama daha belirgin adlandirilmali.

## Uygulama Parcalari

1. **Setup friction kapisi** - `MainActivity.kt` otomatik launcher picker tekrarini kaldir. Durum: tamamlandi, statik dogrulandi.
2. **Home mod karari** - Yeni `home_mode` preference eklemek yerine mevcut `KEY_FOCUS_MODE` search-first davranisa genisletildi. Durum: tamamlandi, statik dogrulandi.
3. **Search-first UI** - `HomeScreen.kt` icinde focus/search-first modda klasor pager gizlenir; search bar + dock + favorites + suggestions + recent akisi kalir. `HomeFavoritesSection.kt` bu modda kucuk ekranda ikincil satirlari saklamaz. Durum: tamamlandi, statik dogrulandi.
4. **Oneri tekrarlarini azaltma** - favorites/recent/suggested yuzeyleri ayni anda acikken oncelik sirasi belirlendi. Durum: tamamlandi, statik dogrulandi.
5. **Settings bilgi mimarisi** - `SettingsHomeScreenSection.kt` toggle'lari alt gruplara ayrildi. Durum: tamamlandi, statik dogrulandi.
6. **Smoke kaniti** - cihaz/emulator olmadan kapanmaz; `Settings hiyerarsi smoke` ve `Search/launcher regression smoke` maddeleri ayri kalacak.

## Kapanis Karari

ROADMAP U10'un minimum ciktisi olan referans karsilastirma, search-first karar listesi ve dar uygulama parcalari tamamlandi. Bundan sonraki isler U10 altindan degil, yukaridaki uygulama parcalari veya mevcut smoke/cihaz maddeleri altindan takip edilmeli.
