package org.openremote.manager.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class RulePersistence {

    private static final Logger LOG = Logger.getLogger(RulePersistence.class.getName());

    // TODO: This should be persistent on disk
    final protected Map<String, String> data = new HashMap<>();

    public void writeData(String key, Object value) {
        LOG.fine("Writing '" + key + "': " + value);
        data.put(key, value != null ? value.toString() : null);
    }

    public String readData(String key) {
        LOG.fine("Reading '" + key + "'");
        return data.get(key);
    }

    public String readData(String key, String defaultValue) {
        LOG.fine("Reading '" + key + "', default: " + defaultValue);
        return data.getOrDefault(key, defaultValue);
    }

    public void deleteData(String key) {
        LOG.fine("Deleting '" + key + "'");
        data.remove(key);
    }

}
