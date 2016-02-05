package org.openremote.manager.server.util;

import io.vertx.core.json.JsonObject;

import java.util.Map;

public class EnvironmentUtil {

    public static JsonObject getEnvironment() {
        JsonObject env = new JsonObject();
        Map<String, String> environment = System.getenv();
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            env.put(entry.getKey(), entry.getValue());
        }
        return env;
    }
}
