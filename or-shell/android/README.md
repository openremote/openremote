# OpenRemote Milestone 1 Designer Prototype - Android Console

To be removed when the prototype is removed.

Development
---

* Install JDK 1.7

* Install Android SDK and API level 22

* Create file `local.properties` in the root of the project with content `sdk.dir=/path/to/android/sdk`

* Import project in Android Studio/IntelliJ

Installing the application (debug build)
---

    ./gradlew clean check build

* Install with `adb install -r build/outputs/apk/or-android-debug.apk`
