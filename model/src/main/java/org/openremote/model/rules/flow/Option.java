package org.openremote.model.rules.flow;

public class Option {
    private String name;
    private Object value;

    public Option(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public Option() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
