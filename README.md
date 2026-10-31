# Quiet Launcher

A minimalist Android home screen: a clock, the date, and the names of a few apps.
No icons, no app drawer. Every app you don't pick is hidden from the home screen.

## Install on your phone (Redmi 14C or any Android 8+)

1. On your phone, open this repo on GitHub → **Releases** → newest **Quiet Launcher build**.
2. Download the `.apk` and open it. Allow "Install unknown apps" for your browser/files app when asked.
3. Open **Quiet**. Tick the apps you want on the home screen, then **Done**.
4. Make it your home screen: long-press the clock → **Set Quiet as default home app**,
   or on Xiaomi: *Settings → Apps → Default apps → Home app → Quiet*.

New builds install over the old one and keep your settings.

## Using it

| Do this | To |
| --- | --- |
| Tap an app name | Open it |
| Long-press an app name | Rename, move up/down, remove, app info |
| Long-press the clock | Choose home apps / set default / phone settings |
| Tap the clock | Alarms |
| Tap the date | Calendar |

To get your old launcher back: Settings → Apps → Default apps → Home app → System launcher.

## Building

Every push builds the APK with GitHub Actions (`.github/workflows/build.yml`) and publishes it as a release.
Locally: open the folder in Android Studio, or run `./gradlew assembleRelease`.

`keystore/sideload.jks` is a throwaway signing key so updates install over each other. It's for personal
sideloading only; don't use it for a Play Store release.
