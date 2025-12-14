package org.openremote.model.telematics.teltonika;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.openremote.model.util.ValueUtil;

import java.util.ArrayList;
import java.util.Map;

public class TeltonikaMqttMessage extends TeltonikaMessage {

    public TeltonikaMqttMessage(JsonNode message, Map<String, String> properties) {
        this.payloads = new ArrayList<>(1);
        JsonNode state = message.get("state");
        if (state != null) {
            JsonNode reported = state.get("reported");
            if (reported != null) {
                Map<String, Object> reportedMap = ValueUtil.JSON.convertValue(reported, new TypeReference<Map<String, Object>>() {});
                payloads.add(new TeltonikaDataPayload(reportedMap));
            }
        }else{
            // access RSP field for response payloads
            JsonNode rsp = message.get("RSP");
            if (rsp != null) {
                Map<String, Object> rspMap = ValueUtil.JSON.convertValue(rsp, new TypeReference<Map<String, Object>>() {});
                payloads.add(new TeltonikaResponsePayload((String) rspMap.get("RSP")));
            }
        }
    }
}
