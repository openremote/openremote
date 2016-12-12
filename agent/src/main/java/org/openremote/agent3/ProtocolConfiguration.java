package org.openremote.agent3;

import elemental.json.Json;
import elemental.json.JsonObject;
import org.openremote.model.Attribute;
import org.openremote.model.AttributeType;

public class ProtocolConfiguration extends Attribute {

    public ProtocolConfiguration(String name, JsonObject jsonObject) {
        super(name, AttributeType.STRING, jsonObject);
    }

    public ProtocolConfiguration(String name, String protocol) {
        super(name, AttributeType.STRING, Json.create(protocol));
    }

}
