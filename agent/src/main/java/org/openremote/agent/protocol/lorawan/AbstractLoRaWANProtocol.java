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
package org.openremote.agent.protocol.lorawan;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.openremote.agent.protocol.mqtt.MQTTAgent;
import org.openremote.agent.protocol.mqtt.MQTTAgentLink;
import org.openremote.agent.protocol.mqtt.MQTTProtocol;
import org.openremote.model.Container;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.Protocol;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.protocol.ProtocolAssetDiscovery;
import org.openremote.model.protocol.ProtocolAssetImport;
import org.openremote.model.protocol.ProtocolAssetService;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.JsonPathFilter;
import org.openremote.model.value.RegexValueFilter;
import org.openremote.model.value.ValueFilter;
import org.openremote.model.value.ValueType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.lorawan.LoRaWANAgent.APPLICATION_ID;
import static org.openremote.agent.protocol.lorawan.LoRaWANAgent.MQTT_HOST;
import static org.openremote.agent.protocol.lorawan.LoRaWANAgent.MQTT_PORT;
import static org.openremote.agent.protocol.lorawan.LoRaWANConstants.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.value.MetaItemType.AGENT_LINK;
import static org.openremote.model.value.MetaItemType.AGENT_LINK_CONFIG;


public abstract class AbstractLoRaWANProtocol<S extends AbstractLoRaWANProtocol<S,T>, T extends LoRaWANAgent<T, S>> implements Protocol<T>, ProtocolAssetImport, ProtocolAssetDiscovery {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, AbstractLoRaWANProtocol.class);

    private ExecutorService executorService;
    private ProtocolAssetService assetService;
    private MQTTProtocol mqttProtocol;
    private MQTTAgent mqttAgent;
    private T agent;
    private Set<String> devEuiSet = new CopyOnWriteArraySet<>();
    protected Container container;

    public AbstractLoRaWANProtocol(T agent) {
        this.agent = agent;
        this.mqttAgent = new MQTTAgent(agent.getName());

        mqttAgent.setId(agent.getId());
        agent.getMqttHost().ifPresent(host -> mqttAgent.setHost(host));
        agent.getMqttPort().ifPresent(port -> mqttAgent.setPort(port));
        agent.getClientId().ifPresent(clientId -> mqttAgent.setClientId(clientId));
        agent.isSecureMode().ifPresent(secureMode -> mqttAgent.setSecureMode(secureMode));
        agent.getPublishQoS().ifPresent(publishQos -> mqttAgent.setPublishQos(publishQos));
        agent.getSubscribeQoS().ifPresent(subscribeQos -> mqttAgent.setSubscribeQos(subscribeQos));
        agent.getCertificateAlias().ifPresent(certificateAlias -> mqttAgent.setCertificateAlias(certificateAlias));
        agent.isResumeSession().ifPresent(resumeSession -> mqttAgent.setResumeSession(resumeSession));
        agent.isWebsocketMode().ifPresent(websocketMode -> mqttAgent.setWebsocketMode(websocketMode));
        agent.getWebsocketPath().ifPresent(websocketPath -> mqttAgent.setWebsocketPath(websocketPath));
        agent.getWebsocketQuery().ifPresent(websocketQuery -> mqttAgent.setWebsocketQuery(websocketQuery));
        agent.getLastWillTopic().ifPresent(lastWillTopic -> mqttAgent.setLastWillTopic(lastWillTopic));
        agent.getLastWillPayload().ifPresent(lastWillPayload -> mqttAgent.setLastWillPayload(lastWillPayload));
        agent.isLastWillRetain().ifPresent(lastWillRetain -> mqttAgent.setLastWillRetain(lastWillRetain));
        agent.getUsernamePassword().ifPresent(usernamePassword -> mqttAgent.setUsernamePassword(usernamePassword));
        mqttAgent.setWildcardSubscriptionTopics(createWildcardSubscriptionTopicList().toArray(new String[0]));

        this.mqttProtocol = new LoRaWANMQTTProtocol(mqttAgent);
    }

    @Override
    public Map<AttributeRef, Attribute<?>> getLinkedAttributes() {
        return mqttProtocol.getLinkedAttributes();
    }

    @Override
    public void linkAttribute(String assetId, Attribute<?> attribute) throws Exception {
        mqttProtocol.linkAttribute(assetId, attribute);

        if (ATTRIBUTE_NAME_DEV_EUI.equals(attribute.getName())) {
            attribute.getValue(String.class)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .ifPresent(devEUI -> devEuiSet.add(devEUI));
        }
    }

    @Override
    public void unlinkAttribute(String assetId, Attribute<?> attribute) throws Exception {
        mqttProtocol.unlinkAttribute(assetId, attribute);

        if (ATTRIBUTE_NAME_DEV_EUI.equals(attribute.getName())) {
            attribute.getValue(String.class)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .ifPresent(devEUI -> devEuiSet.remove(devEUI));
        }
    }

    @Override
    public void start(Container container) throws Exception {
        this.container = container;
        executorService = container.getExecutor();
        mqttProtocol.start(container);
    }

    @Override
    public void stop(Container container) throws Exception {
        mqttProtocol.stop(container);
    }

    @Override
    public T getAgent() {
        return agent;
    }

    @Override
    public void updateLinkedAttribute(AttributeRef attributeRef, Object value, long timestamp) {
        mqttProtocol.updateLinkedAttribute(attributeRef, value, timestamp);
    }

    @Override
    public void updateLinkedAttribute(AttributeRef attributeRef, Object value) {
        mqttProtocol.updateLinkedAttribute(attributeRef, value);
    }

    @Override
    public void setAssetService(ProtocolAssetService assetService) {
        this.assetService = assetService;
        mqttProtocol.setAssetService(assetService);
    }

    @Override
    public void processLinkedAttributeWrite(AttributeEvent event) {
        mqttProtocol.processLinkedAttributeWrite(event);
    }

    @Override
    public boolean onAgentAttributeChanged(AttributeEvent event) {
        if (MQTT_HOST.getName().equals(event.getName()) ||
            MQTT_PORT.getName().equals(event.getName()) ||
            APPLICATION_ID.getName().equals(event.getName()))
        {
            return true;
        }

        return mqttProtocol.onAgentAttributeChanged(event);
    }

    @Override
    public Future<Void> startAssetImport(byte[] fileData, Consumer<AssetTreeNode[]> assetConsumer) {
        return executorService.submit(() -> {
            if (!checkCSVImportPrerequisites()) {
                assetConsumer.accept(new AssetTreeNode[0]);
                return;
            }

            CsvMapper csvMapper = new CsvMapper();
            CsvSchema schema = CsvSchema.builder()
                .addColumn("devEUI")
                .addColumn("name")
                .addColumn("assetTypeName")
                .addColumn("vendor_id")
                .addColumn("model_id")
                .addColumn("firmwareVersion")
                .setUseHeader(false)
                .build();

            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileData)) {
                List<CsvRecord> csvEntries = csvMapper.readerFor(CsvRecord.class).with(schema).<CsvRecord>readValues(inputStream).readAll();

                AssetTreeNode[] assetTreeNodes = csvEntries.stream()
                    .filter(record -> validateCsvRecord(record))
                    .filter(record -> duplicateAssetCheck(record))
                    .map(this::createAsset)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(AssetTreeNode::new)
                    .toArray(AssetTreeNode[]::new);

                assetConsumer.accept(assetTreeNodes);
            } catch (IOException e) {
                LOG.log(Level.WARNING, "CSV import failed", e);
            }
        },  null);
    }

    @Override
    public Future<Void> startAssetDiscovery(Consumer<AssetTreeNode[]> assetConsumer) {
        return executorService.submit(() -> {
            if (!checkAutoDiscoveryPrerequisites()) {
                assetConsumer.accept(new AssetTreeNode[0]);
                return;
            }

            try {
                AssetTreeNode[] assetTreeNodes = discoverDevices();
                assetConsumer.accept(assetTreeNodes);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Auto discovery failed", e);
            }
        },  null);
    }

    protected boolean checkCSVImportPrerequisites() {
        Optional<String> applicationId = getAgent().getApplicationId();
        boolean isOk = applicationId.map(id -> !id.trim().isEmpty()).orElse(false);
        if (!isOk) {
            LOG.log(Level.WARNING, "CSV import failed because agent attribute '" + APPLICATION_ID.getName() + "'  is missing");
        }
        return isOk;
    }

    protected boolean checkAutoDiscoveryPrerequisites() {
        Optional<String> applicationId = getAgent().getApplicationId();
        boolean isOk = applicationId.map(id -> !id.trim().isEmpty()).orElse(false);
        if (!isOk) {
            LOG.log(Level.WARNING, "Device auto discovery failed because agent attribute '" + APPLICATION_ID.getName() + "' is missing");
        }
        return isOk;
    }

    protected abstract AssetTreeNode[] discoverDevices();
    protected abstract List<String> createWildcardSubscriptionTopicList();
    protected abstract boolean configureMQTTSubscriptionTopic(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord);
    protected abstract boolean configureMQTTPublishTopic(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord);
    protected abstract boolean configureMQTTMessageMatchFilterAndPredicate(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord);
    protected abstract boolean configureMQTTWriteValueTemplate(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord);

    protected boolean configureMQTTValueFilter(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }
        getAgentConfigJsonPath(attribute).ifPresent(
            jsonPath -> agentLink.setValueFilters(new ValueFilter[] {new JsonPathFilter(jsonPath, true, false)})
        );
        getAgentConfigRegex(attribute).ifPresent(
            pattern -> agentLink.setValueFilters(new ValueFilter[] {new RegexValueFilter(pattern, false, false, 1, 0)})
        );
        return true;
    }

    protected boolean configureMQTTValueConverter(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }
        getAgentConfigValueConverter(attribute).ifPresent(converter -> agentLink.setValueConverter(converter));
        return true;
    }

    protected boolean configureMQTTWriteValueConverter(Attribute<?> attribute, MQTTAgentLink agentLink, CsvRecord csvRecord) {
        if (attribute == null || agentLink == null || csvRecord == null) {
            return false;
        }
        getAgentConfigWriteValueConverter(attribute).ifPresent(converter -> agentLink.setWriteValueConverter(converter));
        return true;
    }

    protected Optional<ValueType.ObjectMap> getAgentConfig(Attribute<?> attribute) {
        return Optional.ofNullable(attribute)
            .map(attr -> attr.getMeta())
            .flatMap(metaMap -> metaMap.get(AGENT_LINK_CONFIG))
            .flatMap(metItem -> metItem.getValue());
    }

    protected Optional<Integer> getAgentConfigUplinkPort(Attribute<?> attribute) {
        return getAgentConfig(attribute)
            .map(map -> map.get(AGENT_LINK_CONFIG_UPLINK_PORT))
            .filter(port -> port instanceof Integer)
            .map(port -> (Integer)port);
    }

    protected Optional<Integer> getAgentConfigDownlinkPort(Attribute<?> attribute) {
        return getAgentConfig(attribute)
            .map(map -> map.get(AGENT_LINK_CONFIG_DOWNLINK_PORT))
            .filter(port -> port instanceof Integer)
            .map(port -> (Integer)port);
    }

    protected Optional<String> getAgentConfigJsonPath(Attribute<?> attribute) {
        return getAgentConfig(attribute)
            .map(map -> map.get(AGENT_LINK_CONFIG_VALUE_FILTER_JSON_PATH))
            .filter(jsonPath -> jsonPath instanceof String)
            .map(jsonPath -> ((String) jsonPath).trim())
            .filter(jsonPath -> !jsonPath.isEmpty());
    }

    protected Optional<String> getAgentConfigRegex(Attribute<?> attribute) {
        return getAgentConfig(attribute)
            .map(map -> map.get(AGENT_LINK_CONFIG_VALUE_FILTER_REGEX))
            .filter(regex -> regex instanceof String)
            .map(regex -> ((String) regex).trim())
            .filter(regex -> !regex.isEmpty());
    }

    protected Optional<ValueType.ObjectMap> getAgentConfigValueConverter(Attribute<?> attribute) {
        return getAgentConfig(attribute)
            .map(map -> map.get(AGENT_LINK_CONFIG_VALUE_CONVERTER))
            .filter(valueConverter -> valueConverter instanceof ValueType.ObjectMap)
            .map(valueConverter -> (ValueType.ObjectMap) valueConverter);
    }

    protected Optional<ValueType.ObjectMap> getAgentConfigWriteValueConverter(Attribute<?> attribute) {
        return getAgentConfig(attribute)
            .map(map -> map.get(AGENT_LINK_CONFIG_WRITE_VALUE_CONVERTER))
            .filter(valueConverter -> valueConverter instanceof ValueType.ObjectMap)
            .map(valueConverter -> (ValueType.ObjectMap) valueConverter);
    }

    protected Optional<String> getAgentConfigWriteObjectValueTemplate(Attribute<?> attribute) {
        return getAgentConfig(attribute)
            .map(map -> map.get(AGENT_LINK_CONFIG_WRITE_OBJECT_VALUE_TEMPLATE))
            .filter(valueConverter -> valueConverter instanceof String)
            .map(valueConverter -> ((String) valueConverter).trim())
            .filter(valueConverter -> !valueConverter.isEmpty());
    }

    private boolean validateCsvRecord(CsvRecord record) {
        boolean isValid = false;
        if (record != null) {
            isValid = record.isValid();
            if (!isValid) {
                LOG.log(Level.WARNING, "CSV import skipped an invalid CSV record: " + record);
            }
        }
        return isValid;
    }

    private boolean duplicateAssetCheck(CsvRecord record) {
        if (record == null) {
            return false;
        }

        if (!duplicateAssetCheck(record.getDevEUI())) {
            LOG.log(Level.INFO, "CSV import skipped a CSV record because an asset already existed: " + record);
            return false;
        }

        return true;
    }

    protected boolean duplicateAssetCheck(String devEUI) {
        return Optional.ofNullable(devEUI)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toUpperCase)
            .map(s -> !devEuiSet.contains(s))
            .orElse(false);
    }

    protected Optional<Asset<?>> createAsset(CsvRecord csvRecord) {
        if (csvRecord == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(csvRecord.getAssetTypeName())
            .flatMap(name -> resolveAssetClass(name, csvRecord))
            .map(clazz -> instantiateAsset(clazz, csvRecord))
            .flatMap(asset -> configureAsset(asset, csvRecord));
    }

    private Optional<Class<? extends Asset<?>>> resolveAssetClass(String simpleClassName, CsvRecord csvRecord) {
        if (simpleClassName == null || csvRecord == null) {
            return Optional.empty();
        }

        return ValueUtil.getAssetClass(simpleClassName)
            .or(() -> {
                LOG.log(Level.WARNING, "CSV import skipped a CSV record because of an invalid asset type name: " + csvRecord);
                return Optional.empty();
            });
    }

    private Asset<?> instantiateAsset(Class<?> clazz, CsvRecord csvRecord) {
        if (clazz == null || csvRecord == null) {
            return null;
        }

        Asset<?> asset = null;
        try {
            Constructor<?> constructor = clazz.getConstructor(String.class);
            asset = (Asset<?>) constructor.newInstance(csvRecord.getName());
        } catch (ReflectiveOperationException e) {
            LOG.log(Level.INFO, "CSV import failed to create asset for CSV record: " + csvRecord, e);
        }
        return asset;
    }

    protected Optional<Asset<?>> configureAsset(Asset<?> asset, CsvRecord csvRecord) {
        if (asset == null || csvRecord == null) {
            return Optional.empty();
        }

        Optional.ofNullable(csvRecord.getDevEUI()).ifPresent(devEUI -> asset.getAttribute(ATTRIBUTE_NAME_DEV_EUI).ifPresent(attribute -> attribute.setValue(devEUI.toUpperCase())));
        Optional.ofNullable(csvRecord.getVendorId()).ifPresent(vendorId -> asset.getAttribute(ATTRIBUTE_NAME_VENDOR_ID).ifPresent(attribute -> attribute.setValue(vendorId)));
        Optional.ofNullable(csvRecord.getModelId()).ifPresent(modelId -> asset.getAttribute(ATTRIBUTE_NAME_MODEL_ID).ifPresent(attribute -> attribute.setValue(modelId)));
        Optional.ofNullable(csvRecord.getFirmwareVersion()).ifPresent(version -> asset.getAttribute(ATTRIBUTE_NAME_FIRMWARE_VERSION).ifPresent(attribute -> attribute.setValue(version)));

        boolean isOk = true;

        for (Map.Entry<String, Attribute<?>> entry : asset.getAttributes().entrySet()) {
            String name = entry.getKey();
            Attribute<?> attribute = entry.getValue();
            MQTTAgentLink agentLink = new MQTTAgentLink(agent.getId());

            Optional<String> jsonPath = getAgentConfigJsonPath(attribute);
            Optional<String> regex = getAgentConfigRegex(attribute);
            if (jsonPath.isPresent() || regex.isPresent()) {
                isOk = configureMQTTSubscriptionTopic(attribute, agentLink, csvRecord);
            }
            isOk = isOk && configureMQTTValueFilter(attribute, agentLink, csvRecord);
            isOk = isOk && configureMQTTMessageMatchFilterAndPredicate(attribute, agentLink, csvRecord);
            isOk = isOk && configureMQTTValueConverter(attribute, agentLink, csvRecord);
            Optional<Integer> downlinkPort = getAgentConfigDownlinkPort(attribute);
            if (downlinkPort.isPresent()) {
                isOk = isOk && configureMQTTPublishTopic(attribute, agentLink, csvRecord);
                isOk = isOk && configureMQTTWriteValueConverter(attribute, agentLink, csvRecord);
                isOk = isOk && configureMQTTWriteValueTemplate(attribute, agentLink, csvRecord);
            }

            if (getAgentConfig(attribute).isPresent() || ATTRIBUTE_NAME_DEV_EUI.equals(attribute.getName())) {
                attribute.addOrReplaceMeta(
                    new MetaItem<>(AGENT_LINK, agentLink)
                );
            }

            MetaMap metaMap = attribute.getMeta();
            metaMap.remove(AGENT_LINK_CONFIG);
            attribute.setMeta(metaMap);
        }

        return isOk ? Optional.of(asset) : Optional.empty();
    }
}
