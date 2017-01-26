package org.openremote.android.util;


import org.jboss.aerogear.android.core.RecordId;

public class TestHolder {
    @RecordId
    private String id = null;
    private String value = "Test";

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
