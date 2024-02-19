package org.openremote.agent.protocol.homeassistant;

import org.openremote.agent.protocol.homeassistant.assets.*;
import org.openremote.agent.protocol.homeassistant.entities.HomeAssistantBaseEntity;
import org.openremote.agent.protocol.homeassistant.entities.HomeAssistantEntityStateEvent;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.asset.Asset;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.protocol.ProtocolAssetService;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import java.util.*;
import java.util.logging.Logger;

import static org.openremote.agent.protocol.homeassistant.entities.HomeAssistantEntityType.*;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.value.MetaItemType.AGENT_LINK;

public class HomeAssistantEntityProcessor {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, HomeAssistantProtocol.class);
    private final HomeAssistantProtocol protocol;
    private final ProtocolAssetService protocolAssetService;
    private final String agentId;

    public HomeAssistantEntityProcessor(HomeAssistantProtocol protocol, ProtocolAssetService assetService) {
        this.protocol = protocol;
        this.protocolAssetService = assetService;
        this.agentId = protocol.getAgent().getId();
    }

    // Retrieves the entity type from the given home assistant entity id (format <entity_type>.<entity_id>)
    public static String getEntityTypeFromEntityId(String entityId) {
        String[] parts = entityId.split("\\.");
        return parts[0];
    }

    // Processes a Home Assistant entity state event and updates the appropriate asset
    public void handleEntityStateEvent(HomeAssistantEntityStateEvent event) {
        LOG.info("Processing entity state event: " + event.getData().getEntityId() + " with value: " + event.getData().getNewBaseEntity().getState());
        var entityId = event.getData().getEntityId();
        var entityTypeId = getEntityTypeFromEntityId(entityId);
        if (entityCanBeSkipped(entityTypeId)) {
            return;
        }
        var asset = findAssetByEntityId(entityId);
        if (asset == null)
            return;

        processEntityStateEvent(asset, event);
    }

    // Converts a list of Home Assistant entities to a list of OpenRemote assets
    public Optional<List<HomeAssistantBaseAsset>> convertEntitiesToAssets(List<HomeAssistantBaseEntity> entities) {
        List<String> currentAssets = protocolAssetService.findAssets(new AssetQuery().attributeName("HomeAssistantEntityId")).stream()
                .map(asset -> asset.getAttributes().get("HomeAssistantEntityId").orElseThrow().getValue().get().toString())
                .toList();

        List<HomeAssistantBaseAsset> assets = new ArrayList<>();

        for (HomeAssistantBaseEntity entity : entities) {
            Map<String, Object> homeAssistantAttributes = entity.getAttributes();
            String entityId = entity.getEntityId();
            String entityType = getEntityTypeFromEntityId(entityId);

            if (currentAssets.contains(entityId) || entityCanBeSkipped(entityType)) {
                continue;
            }

            HomeAssistantBaseAsset asset = initiateAssetClass(homeAssistantAttributes, entityType, entityId);

            handleStateConversion(entity, asset);

            for (Map.Entry<String, Object> entry : homeAssistantAttributes.entrySet()) {
                handleAttributeConversion(entry, asset);
            }

            asset.getAttributes().forEach(attribute -> {
                var agentLink = new HomeAssistantAgentLink(agentId, entityType, entity.getEntityId());
                attribute.addOrReplaceMeta(new MetaItem<>(AGENT_LINK, agentLink));
            });

            asset.setId(UniqueIdentifierGenerator.generateId());
            assets.add(asset);
        }
        return Optional.of(assets);
    }

    // Initiates the appropriate asset class based on the given entity type
    HomeAssistantBaseAsset initiateAssetClass(Map<String, Object> homeAssistantAttributes, String entityType, String entityId) {
        var friendlyName = (String) homeAssistantAttributes.get("friendly_name");
        return switch (entityType) {
            case ENTITY_TYPE_LIGHT -> new HomeAssistantLightAsset(friendlyName, entityId);
            case ENTITY_TYPE_BINARY_SENSOR -> new HomeAssistantBinarySensorAsset(friendlyName, entityId);
            case ENTITY_TYPE_SENSOR -> new HomeAssistantSensorAsset(friendlyName, entityId);
            case ENTITY_TYPE_SWITCH -> new HomeAssistantSwitchAsset(friendlyName, entityId);
            default -> new HomeAssistantBaseAsset(friendlyName, entityId);
        };
    }

    // Handles the conversion of Home Assistant attributes to OpenRemote attributes
    private void handleAttributeConversion(Map.Entry<String, Object> entry, Asset<?> asset) {
        LOG.info("Processing attribute: " + entry.getKey() + " with value: " + entry.getValue());
        var attributeValue = entry.getValue();
        var attributeKey = entry.getKey();

        if (entry.getKey().isEmpty() || entry.getValue() == null)
            return;

        //Do not import attribute keys that contain min, max, or supported_ (these are not useful for the user)
        if (attributeKey.contains("min") || attributeKey.contains("max") || attributeKey.contains("supported_features"))
            return;

        if (attributeValue instanceof Integer) {
            Attribute<Integer> attribute = asset.getAttributes().getOrCreate(new AttributeDescriptor<>(attributeKey, ValueType.INTEGER));
            attribute.setValue((Integer) attributeValue);

            if (attributeKey.equals("off_brightness")) { //
                attribute = asset.getAttributes().getOrCreate(new AttributeDescriptor<>("brightness", ValueType.INTEGER));
                attribute.setValue((Integer) attributeValue);
            }
            return; // skip the rest of the checks
        }

        //String based boolean check (true, false, on, off)
        if (attributeValue instanceof String) {
            if (attributeValue.equals("on") || attributeValue.equals("off") || attributeValue.equals("true") || attributeValue.equals("false")) {
                Attribute<Boolean> attribute = asset.getAttributes().getOrCreate(new AttributeDescriptor<>(attributeKey, ValueType.BOOLEAN));
                attribute.setValue(attributeValue.equals("on") || attributeValue.equals("true"));
            }
        }
    }

    // Handles the conversion of Home Assistant state to OpenRemote state
    private void handleStateConversion(HomeAssistantBaseEntity entity, Asset<?> asset) {
        var assetState = entity.getState();

        try {
            var value = Double.parseDouble(assetState); // attempt parse before asset attribute creation
            Attribute<Double> attribute = asset.getAttributes().getOrCreate(new AttributeDescriptor<>("state", ValueType.NUMBER));
            attribute.setValue(value);

        } catch (NumberFormatException | NullPointerException e) {
            // not a number - can continue
        }

        if (assetState.equals("on") || assetState.equals("off") || assetState.equals("true") || assetState.equals("false")) {
            Attribute<Boolean> attribute = asset.getAttributes().getOrCreate(new AttributeDescriptor<>("state", ValueType.BOOLEAN));
            attribute.setValue(assetState.equals("on") || assetState.equals("true"));
        } else {
            Attribute<String> attribute = asset.getAttributes().getOrCreate(new AttributeDescriptor<>("state", ValueType.TEXT));
            attribute.setValue(assetState);
        }
    }

    @SuppressWarnings("unchecked") // suppress unchecked cast warnings for attribute.get() calls
    private void processEntityStateEvent(Asset<?> asset, HomeAssistantEntityStateEvent event) {

        LOG.info("Processing entity state event for asset: " + asset.getName() + " with value: " + event.getData().getNewBaseEntity().getState());

        for (Map.Entry<String, Object> eventAttribute : event.getData().getNewBaseEntity().getAttributes().entrySet()) {
            var assetAttribute = asset.getAttributes().get(eventAttribute.getKey());
            if (assetAttribute.isEmpty()) {
                continue;
            }

            if (assetAttribute.get().getValue().equals(eventAttribute.getValue())) {
                continue;
            }

            AttributeEvent attributeEvent = new AttributeEvent(asset.getId(), assetAttribute.get().getName(), eventAttribute.getValue());
            protocol.handleExternalAttributeChange(attributeEvent);
        }

        var stateAttribute = asset.getAttribute("state");
        if (stateAttribute.isPresent()) {
            AttributeEvent attributeEvent;
            if (isAttributeAssignableFrom(stateAttribute.get(), Boolean.class)) {
                LOG.info("Processing boolean state attribute change for asset: " + asset.getName() + " with value: " + event.getData().getNewBaseEntity().getState());
                Attribute<Object> attribute = stateAttribute.get();
                boolean value = event.getData().getNewBaseEntity().getState().equals("on") || event.getData().getNewBaseEntity().getState().equals("true");
                attributeEvent = new AttributeEvent(asset.getId(), attribute.getName(), value);
                protocol.handleExternalAttributeChange(attributeEvent);
            } else if (isAttributeAssignableFrom(stateAttribute.get(), String.class)) {
                LOG.info("Processing string state attribute change for asset: " + asset.getName() + " with value: " + event.getData().getNewBaseEntity().getState());
                Attribute<Object> attribute = stateAttribute.get();
                attributeEvent = new AttributeEvent(asset.getId(), attribute.getName(), event.getData().getNewBaseEntity().getState());
                protocol.handleExternalAttributeChange(attributeEvent);
            }
        }

    }

    // Checks whether the attribute<?> can be assigned from the given class, allowing safe casting
    private Boolean isAttributeAssignableFrom(Attribute<?> attribute, Class<?> clazz) {
        assert attribute.getType() != null;
        return attribute.getType().getType().isAssignableFrom(clazz);
    }

    // Retrieves the appropriate asset based on the given home assistant entity id
    private Asset<?> findAssetByEntityId(String homeAssistantEntityId) {
        return protocolAssetService.findAssets(new AssetQuery().attributeName("HomeAssistantEntityId")).stream()
                .filter(a -> a.getAttributes().get("HomeAssistantEntityId").orElseThrow().getValue().flatMap(v -> v.equals(homeAssistantEntityId) ? Optional.of(v) : Optional.empty()).isPresent())
                .findFirst().orElse(null);
    }

    private boolean entityCanBeSkipped(String entityType) {
        List<String> importedEntityTypes = new ArrayList<>(Arrays.stream(protocol.getAgent().getImportedOtherEntityTypes().orElse("").split(",")).toList());

        protocol.getAgent().getImportedLight().ifPresent(bool -> {
            if (bool) importedEntityTypes.add(ENTITY_TYPE_LIGHT);
        });
        protocol.getAgent().getImportedSensor().ifPresent(bool -> {
            if (bool) importedEntityTypes.add(ENTITY_TYPE_SENSOR);
        });
        protocol.getAgent().getImportedBinarySensor().ifPresent(bool -> {
            if (bool) importedEntityTypes.add(ENTITY_TYPE_BINARY_SENSOR);
        });
        protocol.getAgent().getImportedSwitch().ifPresent(bool -> {
            if (bool) importedEntityTypes.add(ENTITY_TYPE_SWITCH);
        });
        for (String importedEntityType : importedEntityTypes) {
            if (importedEntityType.equals(entityType))
                return false;
        }

        return true;
    }

}
