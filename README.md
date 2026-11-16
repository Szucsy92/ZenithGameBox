# Zenith GameBox

Retro játékkonzol hét játékkal (Bird, Dino, Pong, Space, Jump, Snake, Memory), érintéssel és húzással. Teljesen natív, internet nélkül is játszható.

## Hivatalos alkalmazás

Az Android és iOS kiadások kizárólag a natív forrásokból készülnek:
- Android: `native/android` — Kotlin + Jetpack Compose
- iOS: `native/ios` — SwiftUI

A `.github/workflows/native-build.yml` az egyetlen hivatalos build- és release-folyamat. Minden kiadás Release konfigurációval készül; debug APK nem készül. Androidon a GitHub Actions az `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` és `ANDROID_KEY_PASSWORD` titkokkal aláírt APK-t állít elő; kulcs nélkül aláíratlan Release APK készül. Az iOS IPA aláíratlan; telepítéshez Apple fejlesztői aláírás és provisioning szükséges.

Egyszeri aláírókulcs-készítéshez: **Actions → Aláírókulcs készítése (egyszeri)** (lásd a fájl elején a szükséges titkokat). Ha a korábbi (WebView-s) APK kulcsa megvan, a meglévő titkokat használd tovább: az alkalmazásazonosító (`hu.zenith.gamebox`) és a verziókód-séma változatlan, így az új APK a régire frissítésként települ.

## Offline működés

Minden játék, kép és beállítás az alkalmazásban van. A hálózat kizárólag az opcionális frissítés-ellenőrzéshez kell (Android); internet nélkül ez csendben kimarad.

## Frissítés az alkalmazásból (OTA, Android)

Indításkor az app megnézi a GitHub legfrissebb kiadását (`releases/latest`). A kiadás címkéje `native-<dátum>-<verziókód>`, a végén lévő szám az APK `versionCode`-ja (a 2020-01-01 óta eltelt másodpercek, mint a korábbi kiadásoknál). Ha újabb, megjelenik az „Új frissítés elérhető” ablak: **Később** vagy **Letöltés**. A Letöltés az APK-t az appon belül letölti, majd elindítja a telepítést (a rendszer egyszer az „ismeretlen forrásból telepítés” engedélyt és a telepítés jóváhagyását kéri). Feltételek: a repó nyilvános legyen, és a kiadások ugyanazzal a kulccsal legyenek aláírva. A repó nevét a CI a `GITHUB_REPOSITORY` változóból adja a buildnek. iOS-en az app nem tud önmagát telepíteni, ott nincs ilyen ablak.

## Fejlesztés

- Android Studio: nyisd meg a `native/android` mappát.
- Xcode 16+: nyisd meg a `native/ios/ZenithGameBox.xcodeproj` projektet (iOS 17+).
