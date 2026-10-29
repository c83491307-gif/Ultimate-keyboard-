# Ultimate Keyboard — Android IME

Native Android keyboard project using Kotlin and `InputMethodService` (not a WebView-only mockup).

## Source package

`Ultimate-Keyboard-Android-CI-Source.zip` contains the Gradle project, Android manifest, keyboard service, settings activity, clipboard store, tests, verification script, and GitHub Actions APK build workflow.

## Build APK

1. Download and extract the source archive.
2. Open the `ultimate-keyboard` directory in Android Studio, or push the source files to a GitHub repository.
3. Run the **Android APK** workflow under Actions to build a debug APK.
4. Install the APK on an Android device, then enable **Ultimate Keyboard** in Android Settings → Languages & input → On-screen keyboard.

This is a starter implementation; validate build results and test on-device before daily use.