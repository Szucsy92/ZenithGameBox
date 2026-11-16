# Natív alkalmazás (Android + iOS)

## Android
Nyisd meg az `android/` mappát Android Studio-ban.
- Kotlin + Jetpack Compose, minSdk 24
- `game/`: a hét játék és a terminál-cellás rács (`Term.kt`), az eredeti mechanikával
- `ui/`: kézi konzol, menü, játékfelület, jelly érintőgombok, a rács Canvas-rajzolója (`GridCanvas.kt`)
- `app/`: helyi mentés (legjobb pontszámok, játékbeállítások), OTA frissítés (`UpdateManager.kt`)
- Nincs kiosk / képernyőrögzítés

## iOS
Nyisd meg az `ios/ZenithGameBox.xcodeproj` projektet Xcode 16+-ban; iOS 17+.
- SwiftUI Canvas rajzolás, `UserDefaults` mentés
- Ugyanaz a játéklogika és felület, mint Androidon (frissítés-ablak nélkül)

## Build és release
A `.github/workflows/native-build.yml` építi az Android Release APK-t és az iOS IPA-t. A kiadás az Android-build sikerén múlik; az iOS IPA akkor kerül mellé, ha az is elkészült.
