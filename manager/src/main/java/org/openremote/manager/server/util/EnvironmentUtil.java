package org.openremote.manager.server.util;

import io.vertx.core.json.JsonObject;

import java.util.Locale;
import java.util.Map;

public class EnvironmentUtil {

    public static JsonObject getEnvironment() {
        JsonObject env = new JsonObject();
        Map<String, String> environment = System.getenv();
        for (Map.Entry<String, String> entry : environment.entrySet()) {

            // Integer
            try {
                Integer intValue = Integer.valueOf(entry.getValue());
                env.put(entry.getKey(), intValue);
            } catch (NumberFormatException ex) {
                // Ignore
            }

            // Boolean
            if (entry.getValue().toLowerCase(Locale.ROOT).equals("true"))
                env.put(entry.getKey(), true);
            if (entry.getValue().toLowerCase(Locale.ROOT).equals("false"))
                env.put(entry.getKey(), false);

            // or String
            if (env.getValue(entry.getKey()) == null)
                env.put(entry.getKey(), entry.getValue());

        }
        return env;
    }
}
