# OpenRemote Milestone 1 Designer Prototype - Android Console

To be removed when the prototype is removed.

Development
---

* Install JDK 1.8

* Install Android SDK and API level 25

* Create file `local.properties` in the root of the project with content `sdk.dir=/path/to/android/sdk` or set ANDROID_HOME system variable to the same path

* Import project in Android Studio/IntelliJ

Installing the application (debug build)
---

    ./gradlew clean check build

* Install with `adb install -r build/outputs/apk/or-android-debug.apk`
