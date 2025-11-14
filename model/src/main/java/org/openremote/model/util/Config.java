package org.openremote.model.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A simple utility for overlaying runtime configuration on top of {@link System#getenv()} env config. The application
 * is responsible for injecting the additional config at the earliest opportunity before it is consumed by other code.
 */
public class Config {

    public static final String OR_DEV_MODE = "OR_DEV_MODE";
    public static final boolean OR_DEV_MODE_DEFAULT = true;
    public static final String OR_METRICS_ENABLED = "OR_METRICS_ENABLED";
    public static final boolean OR_METRICS_ENABLED_DEFAULT = false;
    protected static Map<String, String> config;
    protected static Map<String, Object> parsedConfig;

    static {
        init(null);
    }

    /**
     * Inject additional config that is merged with {@link System#getenv()}
     */
    public static void init(Map<String, String> configOverlay) {
        parsedConfig = new HashMap<>();
        Map<String, String> mergedConfig = new HashMap<>(System.getenv());
        if (configOverlay != null) {
            mergedConfig.putAll(configOverlay);
        }
        mergedConfig.values().removeIf(TextUtil::isNullOrEmpty);
        config = mergedConfig;
    }

    public static Map<String, String> get() {
        return config;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> type, T defaultValue) {
        Object parsedValue = parsedConfig.get(key);
        if (parsedValue != null) {
            try {
                return (T) parsedValue;
            } catch (ClassCastException e) {
                parsedConfig.remove(key);
            }
        }

        String valueStr = config.get(key);
        if (valueStr != null) {
            T result = ValueUtil.getValueCoerced(valueStr, type).orElse(null);
            if (result != null) {
                parsedConfig.put(key, result);
            }
            return result;
        }
        return defaultValue;
    }

    public static boolean containsKey(String key) {
        return config.containsKey(key);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Optional.ofNullable(get(key, Boolean.class, defaultValue)).orElse(defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        return Optional.ofNullable(get(key, Integer.class, defaultValue)).orElse(defaultValue);
    }

    public static String getString(String key, String defaultValue) {
        return MapAccess.getString(config, key, defaultValue);
    }

    public static boolean isDevMode() {
        return getBoolean(OR_DEV_MODE, OR_DEV_MODE_DEFAULT);
    }
}
