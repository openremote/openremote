package org.openremote.model.datapoint;

import elemental.json.JsonValue;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeValueChange;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "ASSET_DATAPOINT")
public class AssetDatapoint extends Datapoint {

    public AssetDatapoint() {
    }

    public AssetDatapoint(AttributeValueChange attributeValueChange) {
        super(attributeValueChange);
    }

    public AssetDatapoint(AttributeRef attributeRef, JsonValue value) {
        super(attributeRef, value);
    }

    public AssetDatapoint(String entityId, String attributeName, long timestamp, JsonValue value) {
        super(entityId, attributeName, timestamp, value);
    }

}
