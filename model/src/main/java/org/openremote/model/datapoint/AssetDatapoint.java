package org.openremote.model.datapoint;

import elemental.json.JsonValue;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeState;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "ASSET_DATAPOINT")
public class AssetDatapoint extends Datapoint {

    public AssetDatapoint() {
    }

    public AssetDatapoint(AttributeState attributeState) {
        super(attributeState);
    }

    public AssetDatapoint(AttributeRef attributeRef, JsonValue value) {
        super(attributeRef, value);
    }

    public AssetDatapoint(String entityId, String attributeName, JsonValue value) {
        super(entityId, attributeName, value);
    }

    public AssetDatapoint(AttributeState attributeState, long timestamp) {
        super(attributeState, timestamp);
    }

    public AssetDatapoint(AttributeRef attributeRef, JsonValue value, long timestamp) {
        super(attributeRef, value, timestamp);
    }

    public AssetDatapoint(String entityId, String attributeName, JsonValue value, long timestamp) {
        super(entityId, attributeName, value, timestamp);
    }

}
