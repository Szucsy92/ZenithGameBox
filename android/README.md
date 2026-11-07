# Zenith GameBox – Android

Ez a csomag a közzétett webes konzolt nyitja meg: https://window-wiggle-arcade.lovable.app/. Internetkapcsolat és naprakész Android System WebView szükséges. Az APK dátuma a telefonos csomag készítésének ideje, nem a webes játék változatának rögzítése. A játékok változatlanok.

## Egyszeri aláírási beállítás

A GitHubon: **Settings → Secrets and variables → Actions → New repository secret**. Ezeket kell megadni:

| Titok neve | Tartalom |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | A meglévő kiadási kulcstár Base64-kódolt tartalma |
| `ANDROID_KEYSTORE_PASSWORD` | A kulcstár jelszava |
| `ANDROID_KEY_ALIAS` | Az aláíró kulcs neve a kulcstárban |
| `ANDROID_KEY_PASSWORD` | Az aláíró kulcs jelszava |
| `ANDROID_SIGNING_CERT_SHA256` | Az aláíró tanúsítvány SHA-256 ujjlenyomata; kettősponttal vagy anélkül |

**Ha korábbi APK-ra frissíteni szeretnétek:** ugyanaz az alkalmazásazonosító és ugyanaz a kulcstár kell. A korábbi APK és kulcstár ebben a projektben nem érhető el, ezért a korábbi telepítésre frissítés még nincs ellenőrizve. Az új csomag azonosítója `hu.zenith.gamebox`; első kiadás előtt egyeztesd a korábbi azonosítóval, ha szükséges.

Ha nincs korábbi aláírókulcs, helyben, egy Java JDK-val készíts egyszer egyet (a jelszavakat a parancs bekéri, nem kerülnek a parancssorba):

```sh
keytool -genkeypair -keystore zenith-release.jks -alias zenith-release -keyalg RSA -keysize 3072 -validity 10000
keytool -list -v -keystore zenith-release.jks -alias zenith-release
```

A második parancs SHA256 sorát használd az ujjlenyomathoz. A kulcstár Base64-változatát saját gépen készítsd el (`base64 < zenith-release.jks`), és kizárólag a GitHub titkos mezőjébe másold. **A kulcstárat, jelszavakat és Base64-tartalmat soha ne töltsd fel a repóba, ne küldd chatbe.** Őrizd meg őket biztonságos, külön mentésben; elvesztésük esetén a már telepített app frissíthetősége elveszhet. Nem készül új kulcs minden indításkor, és nincs helyettesítő debug-aláírás.

## APK készítése és letöltése

1. A módosítások kerüljenek a GitHub alapértelmezett ágára. A konzol legfrissebb webes változatát tedd közzé Lovable-ből is.
2. **Actions → Aláírt Android APK → Run workflow**, az alapértelmezett ágat választva.
3. A sikeres futás után a **Releases** oldalon megjelenik a dátumozott APK és az ellenőrző összege. A legújabb sikeres kiadás kapja a **Latest** jelölést.
4. Példa: `ZenithGameBox-2026-11-07_05-24-00-r12-a1.apk`. A dátum **UTC**, a futási szám és újrafuttatási szám megkülönbözteti az ismételt készítéseket.

A készítés hiányzó titkok, hibás aláírás vagy eltérő tanúsítvány esetén megáll; ilyenkor nem publikál aláíratlan csomagot. A frissítéshez növekvő verziószámot az UTC-készítési idő adja. A kiadás a ténylegesen csomagolt forrásra hivatkozik. A munkafolyamat kizárólag az alapértelmezett ágról indulhat.

Az Android az ismeretlen forrásból telepített APK engedélyezését kérheti, és Play Protect figyelmeztetést is mutathat: az aláírás ezt nem kerüli meg. A GitHub Releases a repó hozzáférési szabályait követi; privát repóból csak jogosult felhasználók tölthetnek le.

## Megjelenés és ellenőrzés

- A felső és alsó sáv átlátszó, világos rendszerikonokkal a konzol háttérszíne felett.
- A háttér a képernyő széléig ér, a játék és gombok a kamerakivágás és rendszervezérlők biztonságos területén maradnak.
- Az indítójel a meglévő konzolikonból származik, valódi átlátszó háttérrel; az Android rendszer saját ikonmaszkját továbbra is alkalmazhatja.
- A `scripts/android-branding.py` a webes háttérszínből újragenerálja a telefonos háttérszínt, és a meglévő képből az indítójelet. A GitHub-futás ezt automatikusan elvégzi.
- API 24+; Gradle 8.13, AGP 8.13.2, JDK 17, compile/target SDK 36. A rögzített Gradle-t a munkafolyamat telepíti, nincs szükség hiányzó wrapper fájlokra.

Helyi ellenőrzés: `python3 scripts/test_android_release.py`. Teljes Android-csomagolás JDK és Android SDK mellett: `gradle --no-daemon :app:assembleRelease` az `android` könyvtárban, a fenti titkokkal környezeti változóként és `ANDROID_KEYSTORE_PATH`-ként a kulcstár helyével. A titkokat ne tárold a projektben.

Az első sikeres GitHub-futás után valódi telefonon ellenőrizendő: gesztusos és háromgombos navigáció, kamerakivágás, indítás, mindkét joystick, hálózati hiba/újrapróbálás, majd azonos aláírókulcsú újabb APK telepítése frissítésként.