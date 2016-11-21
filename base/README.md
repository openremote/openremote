# OpenRemote Base Images

Build Docker base images before you build any other projects, in the main project root run:

```
./gradlew clean buildBaseImage
```

To publish base images, execute:

```
./gradlew clean pushImage -PdockerHubUsername=username -PdockerHubPassword=secret
```
