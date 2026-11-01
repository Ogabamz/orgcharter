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
| Long-press the clock | Settings |
| Tap the line at the top | Set today's one thing |
| Tap **focus** | Start a focus session (long-press to change its length) |
| Swipe down, or tap **notes** | Quick notes |
| Tap **mono** | Grayscale on/off |
| Tap the verse | Open your Bible app |
| Tap the clock / date | Alarms / calendar |

YouTube and Chrome are never offered for the home screen.

### One-time setup for the extras (Settings → long-press the clock)

- **Pause & website blocking** use an accessibility service. On Xiaomi/Redmi: Settings → Additional settings →
  Accessibility → Downloaded apps → Quiet → on. If it's greyed out, first go to App info → ⋮ → *Allow restricted
  settings*. Also turn on *Autostart* for Quiet.
- **Screen time** needs *Usage access* for Quiet.
- **Grayscale** needs one permission granted from a computer:
  `adb shell pm grant com.quiet.launcher android.permission.WRITE_SECURE_SETTINGS`
  (on Xiaomi also enable *USB debugging (Security settings)* in Developer options).

To get your old launcher back: Settings → Apps → Default apps → Home app → System launcher.

## Editions

- **lite** (build 5 onwards, what GitHub publishes): everything except the pause screen and website
  blocking. It has no accessibility service, so Play Protect doesn't block the install.
- **full** (build 4): adds the pause screen and website blocking through an accessibility service.
  Play Protect often blocks installing this from a browser; install it from a computer with
  `adb install` instead. Build it with `./gradlew assembleFullRelease`.

## Building

Every push builds the APK with GitHub Actions (`.github/workflows/build.yml`) and publishes it as a release.
Locally: open the folder in Android Studio, or run `./gradlew assembleLiteRelease`.

`keystore/sideload.jks` is a throwaway signing key so updates install over each other. It's for personal
sideloading only; don't use it for a Play Store release.

The text uses [Source Serif 4](https://github.com/adobe-fonts/source-serif) (SIL Open Font License, see `FONT_LICENSE.txt`).
