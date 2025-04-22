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
package org.openremote.agent.protocol.lorawan.tts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openremote.agent.protocol.lorawan.AbstractLoRaWANProtocol;
import org.openremote.agent.protocol.lorawan.CsvRecord;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.query.filter.NumberPredicate;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.JsonPathFilter;
import org.openremote.model.value.ValueFilter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.lorawan.tts.TheThingsStackAgent.API_KEY;
import static org.openremote.agent.protocol.lorawan.tts.TheThingsStackAgent.TENANT_ID;
import static org.openremote.model.asset.agent.Agent.HOST;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.container.util.MapAccess.getBoolean;

public class TheThingsStackProtocol extends AbstractLoRaWANProtocol<TheThingsStackProtocol, TheThingsStackAgent> {

    public static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, TheThingsStackProtocol.class);

    public static final String PROTOCOL_DISPLAY_NAME = "The Things Stack";

    private Optional<Map<String, String>> devEuiToDeviceIdMap = Optional.empty();

    public TheThingsStackProtocol(TheThingsStackAgent agent) {
        super(agent);
    }

    public static final String THE_THINGS_STACK_TEST =  "THE_THINGS_STACK_TEST";

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getProtocolInstanceUri() {
        return "tts://" + getAgent().getHost().orElse("-") + ":"
                        + getAgent().getMqttPort().map(p -> p.toString()).orElse("-");
    }

    @Override
    protected boolean checkCSVImportPrerequisites() {
        boolean isOk = super.checkCSVImportPrerequisites();

        List<AbstractMap.SimpleEntry<AttributeDescriptor<String>, Optional<String>>> list = new ArrayList<>(3);
        list.add(new AbstractMap.SimpleEntry<>(HOST, getAgent().getHost()));
        list.add(new AbstractMap.SimpleEntry<>(TENANT_ID, getAgent().getTenantId()));
        list.add(new AbstractMap.SimpleEntry<>(API_KEY, getAgent().getApiKey()));

        for (AbstractMap.SimpleEntry<AttributeDescriptor<String>, Optional<String>> item : list) {
            if (!item.getValue().map(attrValue -> !attrValue.trim().isEmpty()).orElse(false)) {
                isOk = false;
                LOG.log(Level.WARNING, "CSV import failed because agent attribute '" + item.getKey().getName() + "'  is missing");
            }
        }

        initializeDevEuiToDeviceIdMap();
        if(devEuiToDeviceIdMap.isEmpty()) {
            isOk = false;
        }

        return isOk;
    }

    @Override
    protected boolean configureMQTTSubscriptionTopic(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }

        boolean isOk = true;
        Optional<String> applicationId = getAgent().getApplicationId().map(id -> id.trim());
        Optional<String> apiKey = getAgent().getApiKey().map(key -> key.trim());
        Optional<String> tenantId = getAgent().getTenantId().map(id -> id.trim());
        Optional<String> devEUI = Optional.ofNullable(csvRecord.getDevEUI()).map(eui -> eui.trim());
        Optional<Integer> uplinkPort = getAgentConfigUplinkPort(attribute);

        if (uplinkPort.isPresent() && applicationId.isPresent() && devEUI.isPresent() && apiKey.isPresent()) {
            if (devEuiToDeviceIdMap.isEmpty()) {
                return false;
            }
            Optional<String> deviceId = devEuiToDeviceIdMap.flatMap(map -> Optional.ofNullable(map.get(devEUI.get())));
            if (deviceId.isPresent()) {
                agentLink.setSubscriptionTopic("v3/" + (tenantId.isPresent() ? (applicationId.get() + "@" + tenantId.get()) : applicationId.get()) + "/devices/" + deviceId.get() + "/up");
            } else {
                LOG.log(
                    Level.WARNING,
                    "CSV import failure because couldn't find device on LoRaWAN network server: " + csvRecord
                );
                isOk = false;
            }
        }
        return isOk;
    }

    @Override
    protected boolean configureMQTTPublishTopic(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }

        boolean isOk = true;
        Optional<String> applicationId = getAgent().getApplicationId().map(id -> id.trim());
        Optional<String> apiKey = getAgent().getApiKey().map(key -> key.trim());
        Optional<String> tenantId = getAgent().getTenantId().map(id -> id.trim());
        Optional<String> devEUI = Optional.ofNullable(csvRecord.getDevEUI()).map(eui -> eui.trim());
        Optional<Integer> downlinkPort = getAgentConfigDownlinkPort(attribute);

        if (downlinkPort.isPresent() && applicationId.isPresent() && devEUI.isPresent() && apiKey.isPresent()) {
            if (devEuiToDeviceIdMap.isEmpty()) {
                return false;
            }
            Optional<String> deviceId = devEuiToDeviceIdMap.flatMap(map -> Optional.ofNullable(map.get(devEUI.get())));
            if (deviceId.isPresent()) {
                agentLink.setPublishTopic("v3/" + (tenantId.isPresent() ? (applicationId.get() + "@" + tenantId.get()) : applicationId.get()) + "/devices/" + deviceId.get() + "/down/push");
            } else {
                LOG.log(
                    Level.WARNING,
                    "CSV import failure because couldn't find device on the LoRaWAN network server: " + csvRecord
                );
                isOk = false;
            }
        }

        return isOk;
    }

    @Override
    protected boolean configureMQTTMessageMatchFilterAndPredicate(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }
        getAgentConfigUplinkPort(attribute).ifPresent(port ->
            agentLink.setMessageMatchFilters(new ValueFilter[] {new JsonPathFilter("$.uplink_message.f_port", true, false)})
                .setMessageMatchPredicate(new NumberPredicate(port))
        );
        return true;
    }

    @Override
    protected boolean configureMQTTWriteValueTemplate(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }

        boolean isOk = true;
        Optional<Integer> downlinkPort = getAgentConfigDownlinkPort(attribute);

        if (downlinkPort.isPresent()) {
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> downlink = new HashMap<>();
            downlink.put("f_port", downlinkPort.get());
            downlink.put("priority", "NORMAL");
            downlink.put("frm_payload", "%VALUE%");

            Map<String, Object> root = new HashMap<>();
            root.put("downlinks", Collections.singletonList(downlink));

            try {
                String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                agentLink.setWriteValue(json);
            } catch (JsonProcessingException e) {
                isOk = false;
                LOG.log(Level.SEVERE, "CSV import failure for CSV record: " + csvRecord, e);
            }
        }
        return isOk;
    }

    private void initializeDevEuiToDeviceIdMap() {
        Optional<String> host = getAgent().getHost().map(h -> h.trim());
        Optional<Integer> port = getAgent().getPort();
        Optional<String> apiKey = getAgent().getApiKey().map(key -> key.trim());
        Optional<String> applicationId = getAgent().getApplicationId().map(key -> key.trim());

        if (host.isEmpty() || apiKey.isEmpty() || applicationId.isEmpty()) {
            return;
        }

        Map<String, String> map = new HashMap<>();

        try {
            String hostWithPort = port
                .map(p -> String.format("%s:%d", host.get(), p))
                .orElse(host.get());
            String url = String.format("%s://%s/api/v3/applications/%s/devices?field_mask=name,ids",
                getTTSApiScheme(), hostWithPort, applicationId.get());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + apiKey.get())
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                JsonNode devices = root.path("end_devices");

                for (JsonNode device : devices) {
                    JsonNode ids = device.path("ids");
                    String devEui = ids.path("dev_eui").asText();
                    String deviceId = ids.path("device_id").asText();

                    if (!devEui.isEmpty()) {
                        map.put(devEui.toLowerCase(), deviceId);
                    }
                }
                this.devEuiToDeviceIdMap =  Optional.of(map);
            } else {
                LOG.log(
                    Level.WARNING,
                    "CSV import failure because couldn't retrieve device id list from the LoRaWAN network server [HTTP status code='" +
                          response.statusCode() + "', error body='" + response.body() +"']"
                );
            }
        } catch (Exception e) {
            LOG.log(
                Level.WARNING,
                "CSV import failure because couldn't retrieve device id list from the LoRaWAN network server", e
            );
        }
    }

    private String getTTSApiScheme() {
        return Optional.ofNullable(container)
            .map(c -> getBoolean(c.getConfig(), THE_THINGS_STACK_TEST, false))
            .map(isTest -> isTest ? "http" : "https")
            .orElse("https");
    }
}








