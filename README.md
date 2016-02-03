# OpenRemote

## Manager

* Run GWT code server: `./gradlew -p manager gwtSuperDev`
* Run server main class `io.vertx.core.Launcher` with arguments `run org.openremote.manager.server.ManagerVerticle --redeploy=**/*.class --launcher-class=io.vertx.core.Launcher`
* (Alternative when debugger is needed) Run server main class `org.openremote.manager.server.Server`
* Open http://localhost:8080/

