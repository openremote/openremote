package org.openremote.model.rules.flow;

public class NodeInternal {
    private String name;
    private Picker picker;
    private Object value;
    private BreakType breakType;

    public enum BreakType{
        NEW_LINE,
        SPACER
    }

    public NodeInternal(String name, Picker picker, Object value) {
        this.name = name;
        this.picker = picker;
        this.value = value;
        this.breakType = BreakType.NEW_LINE;
    }

    public NodeInternal(String name, Picker picker) {
        this.name = name;
        this.picker = picker;
        this.value = picker.getOptions().length == 0 ? null : picker.getOptions()[0].getValue();
        this.breakType = BreakType.NEW_LINE;

    }
    public NodeInternal(String name, Picker picker, BreakType breakType) {
        this.name = name;
        this.picker = picker;
        this.value = picker.getOptions().length == 0 ? null : picker.getOptions()[0].getValue();
        this.breakType = breakType;
    }


    public NodeInternal() {
        name = null;
        picker = new Picker();
        value = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Picker getPicker() {
        return picker;
    }

    public void setPicker(Picker picker) {
        this.picker = picker;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
