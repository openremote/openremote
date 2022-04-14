## Manager customisation
As well as the below information please see the [Manager endpoints and file paths](https://github.com/openremote/openremote/wiki/Architecture:-Manager-endpoints-and-file-paths) wiki.

### Custom provisioning files (`provisioning/`)
As an alternative to writing `java` setup code you can also provide `json` representations of Assets which will be automatically deserialized and added to the system when doing a clean install.

### Console App Configurations (`consoleappconfig/`)
Console app configurations that can be loaded by Android and iOS consoles.

### Custom App Files (`app/`)
This `app` directory is used as the `$CUSTOM_APP_DOCROOT` and can be used to store any custom static content; the Manager UI also checks the `/manager_config.json` path for a custom Manager UI configuration `json` file.

### FCM Configuration (`fcm.json`)
This is where your Firebase cloud messaging config file should be placed to enable push notification for Android/iOS.

### Logging Configuration (`logging.properties`)
Custom `JUL` logging configuration file; default log file can be found [here](https://github.com/openremote/openremote/blob/master/manager/src/main/resources/logging.properties).

### Keycloak Credentials (`keycloak.json`)
This is where custom keycloak credentials are stored/can be supplied; by default the manager will auto generate these during a clean install.

### Custom Java Code (`extensions/`)
Any custom java code should be compiled and made available in this directory; if it is compiled as part of the custom project then only the compiled code should be copied to the `deployment/build/image/manager/extensions` dir and not to this source directory.
