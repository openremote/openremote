package org.openremote.controller.event;

public class RangeEvent extends Event<Integer> {

    private Integer rangeValue;
    private Integer min;
    private Integer max;

    public RangeEvent(int sourceID, String sourceName, Integer value, Integer min, Integer max) {
        super(sourceID, sourceName);
        this.min = min;
        this.max = max;
        setValue(value);
    }

    public Integer getMinValue() {
        return min;
    }

    public Integer getMaxValue() {
        return max;
    }

    @Override
    public Integer getValue() {
        return rangeValue;
    }

    @Override
    public void setValue(Integer value) {
        // TODO: The LevelEvent logs when boundaries are breached, why don't we log here?
        if (value > max) {
            this.rangeValue = max;
        } else if (value < min) {
            this.rangeValue = min;
        } else {
            this.rangeValue = value;
        }
    }

    @Override
    public RangeEvent clone(Integer newValue) {
        if (newValue < getMinValue()) {
            newValue = getMinValue();
        } else if (newValue > getMaxValue()) {
            newValue = getMaxValue();
        }

        return new RangeEvent(this.getSourceID(), this.getSource(), newValue, getMinValue(), getMaxValue());
    }

    @Override
    public String serialize() {
        return Integer.toString(rangeValue);
    }

    @Override
    public String toString() {
        return "RangeEvent{" +
            "sourceId=" + getSourceID() +
            ", source='" + getSource() + "'" +
            ", rangeValue=" + rangeValue +
            ", min=" + min +
            ", max=" + max +
            '}';
    }
}

