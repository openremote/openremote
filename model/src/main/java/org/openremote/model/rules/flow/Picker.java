package org.openremote.model.rules.flow;

public class Picker {
    private PickerType type;
    private Option[] options;

    public Picker(PickerType type, Option[] options) {
        this.type = type;
        this.options = options;
    }

    public Picker(PickerType type) {
        this.type = type;
        this.options = new Option[0];
    }

    public Picker() {
        type = PickerType.NUMBER;
        options = new Option[]{};
    }

    public PickerType getType() {
        return type;
    }

    public void setType(PickerType type) {
        this.type = type;
    }

    public Option[] getOptions() {
        return options;
    }

    public void setOptions(Option[] options) {
        this.options = options;
    }
}
