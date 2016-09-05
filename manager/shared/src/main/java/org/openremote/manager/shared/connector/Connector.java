package org.openremote.manager.shared.connector;

import org.openremote.manager.shared.attribute.Attributes;

public class Connector {

    public static final String ASSET_ATTRIBUTE_CONNECTOR = "connector";

    protected String name;
    protected String type;
    protected Attributes settings;

    public Connector() {
    }

    public Connector(String type) {
        this.type = type;
    }

    public Connector(String name, String type, Attributes settings) {
        this.name = name;
        this.type = type;
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
