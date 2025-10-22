/*
 * Copyright 2025, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.agent.protocol.lorawan.chirpstack;

import org.openremote.agent.protocol.lorawan.AbstractLoRaWANProtocol;
import org.openremote.agent.protocol.lorawan.CsvRecord;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.query.filter.NumberPredicate;
import org.openremote.model.value.JsonPathFilter;
import org.openremote.model.value.ValueFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ChirpStackProtocol extends AbstractLoRaWANProtocol<ChirpStackProtocol, ChirpStackAgent> {

    public static final String PROTOCOL_DISPLAY_NAME = "ChirpStack";

    public ChirpStackProtocol(ChirpStackAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "chirpstack://" + getAgent().getMqttHost().orElse("-") + ":"
                               + getAgent().getMqttPort().map(p -> p.toString()).orElse("-");
    }

    @Override
    protected List<String> createWildcardSubscriptionTopicList() {
        return getAgent().getApplicationId()
            .map(applicationId -> Collections.singletonList("application/" + applicationId + "/device/+/event/up"))
            .orElse(new ArrayList<>());
    }

    @Override
    protected boolean configureMQTTSubscriptionTopic(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }

        Optional<String> applicationId = getAgent().getApplicationId().map(id -> id.trim());
        Optional<String> devEUI = Optional.ofNullable(csvRecord.getDevEUI()).map(eui -> eui.trim());

        if (applicationId.isPresent() && devEUI.isPresent()) {
            agentLink.setSubscriptionTopic("application/" + applicationId.get() + "/device/" + devEUI.get().toLowerCase() + "/event/up");
        }
        return true;
    }

    @Override
    protected boolean configureMQTTPublishTopic(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }

        Optional<String> applicationId = getAgent().getApplicationId().map(id -> id.trim());
        Optional<String> devEUI = Optional.ofNullable(csvRecord.getDevEUI()).map(eui -> eui.trim());

        if (applicationId.isPresent() && devEUI.isPresent()) {
            agentLink.setPublishTopic("application/" + applicationId.get() + "/device/" + devEUI.get().toLowerCase() + "/command/down");
        }
        return true;
    }

    @Override
    protected boolean configureMQTTMessageMatchFilterAndPredicate(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }
        getAgentConfigUplinkPort(attribute).ifPresent(port ->
            agentLink.setMessageMatchFilters(new ValueFilter[] {new JsonPathFilter("$.fPort", true, false)})
                     .setMessageMatchPredicate(new NumberPredicate(port))
        );
        return true;
    }

    @Override
    protected boolean configureMQTTWriteValueTemplate(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }

        Optional<Integer> downlinkPort = getAgentConfigDownlinkPort(attribute);
        Optional<String> devEUI = Optional.ofNullable(csvRecord.getDevEUI()).map(eui -> eui.trim());
        Optional<String> objectTemplate = getAgentConfigWriteObjectValueTemplate(attribute);

        if (downlinkPort.isPresent() && devEUI.isPresent()) {
            String writeValue = "{" +
                "\n  \"devEui\": \"" + devEUI.get() + "\"," +
                "\n  \"confirmed\": true,";
            if (objectTemplate.isPresent()) {
                writeValue += "\n  \"object\": {\n    " + objectTemplate.get() + "\n  },";
            } else {
                writeValue += "\n  \"data\": \"%VALUE%\",";
            }
            writeValue += "\n  \"fPort\": " + downlinkPort.get() + "\n}";

            agentLink.setWriteValue(writeValue);
        }
        return true;
    }
}
