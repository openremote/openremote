package org.openremote.model.attribute;

public class AttributeStateWithTimestamp extends AttributeState {
    protected Long timestamp;

    AttributeStateWithTimestamp(){}

    public AttributeStateWithTimestamp(AttributeState attributeState) {
        this.ref = attributeState.ref;
        this.value = attributeState.value;

        this.timestamp = 0L;
    }
    public AttributeStateWithTimestamp(AttributeState attributeState, Long timestamp) {
        this.ref = attributeState.ref;
        this.value = attributeState.value;

        this.timestamp = timestamp;
    }
}
