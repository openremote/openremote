package org.openremote.manager.shared.connector;

import org.openremote.model.Attributes;

public class Connector {

    public static final String ASSET_ATTRIBUTE_CONNECTOR = "connector";

    protected String name;
    protected String type;
    protected boolean supportsInventoryRefresh;
    protected Attributes settings;

    public Connector() {
    }

    public Connector(String type) {
        this.type = type;
    }

    public Connector(String name, String type, boolean supportsInventoryRefresh, Attributes settings) {
        this.name = name;
        this.type = type;
        this.supportsInventoryRefresh = supportsInventoryRefresh;
        this.settings = settings;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSupportsInventoryRefresh() {
        return supportsInventoryRefresh;
    }

    public void setSupportsInventoryRefresh(boolean supportsInventoryRefresh) {
        this.supportsInventoryRefresh = supportsInventoryRefresh;
    }

    public Attributes getSettings() {
        return settings;
    }

    public void setSettings(Attributes settings) {
        this.settings = settings;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "name='" + name + '\'' +
            ", type='" + type + '\'' +
            ", settings=" + settings +
            '}';
    }
}
