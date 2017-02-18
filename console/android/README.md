# OpenRemote v3 - Android Console

Due to limitations of the Android development environment, this application is a separate, standalone project.

## Building and running the application

```
./gradlew clean installDebug \
    && adb shell am start -n org.openremote.android.v3/org.openremote.android.MainActivity
```