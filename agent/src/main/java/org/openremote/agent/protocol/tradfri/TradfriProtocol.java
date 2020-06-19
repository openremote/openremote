package org.openremote.agent.protocol.tradfri;

import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.agent.protocol.tradfri.device.Gateway;
import org.openremote.agent.protocol.tradfri.device.Light;
import org.openremote.agent.protocol.tradfri.device.Plug;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.tradfri.device.event.*;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.model.attribute.*;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;

import java.util.*;
import java.util.logging.Logger;
import java.util.function.Consumer;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

/**
 * The class that represents the configuration for the IKEA TRÅDFRI protocol.
 */
public class TradfriProtocol extends AbstractProtocol {

    /**
     * The logger for the IKEA TRÅDFRI protocol.
     */
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, TradfriProtocol.class);

    /**
     * The protocol name for the IKEA TRÅDFRI protocol.
     */
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":tradfri";

    /**
     * The display name for the IKEA TRÅDFRI protocol.
     */
    public static final String PROTOCOL_DISPLAY_NAME = "IKEA TRÅDFRI";

    /**
     * The meta gateway host for the IKEA TRÅDFRI protocol.
     */
    public static final String META_TRADFRI_GATEWAY_HOST = PROTOCOL_NAME + ":gatewayHost";

    /**
     * The meta security code for the IKEA TRÅDFRI protocol
     * This security code is used when connecting to the gateway.
     */
    public static final String META_TRADFRI_SECURITY_CODE = PROTOCOL_NAME + ":securityCode";

    /**
     * The current version of the IKEA TRÅDFRI protocol
     */
    protected static final String VERSION = "1.0";

    /**
     * The protocol meta item descriptors that are used to connect to the gateway.
     * The first MetaItemDescriptorImpl is used to hold the entered IP address of the Gateway.
     * The second MetaItemDescriptorImpl is used to hold the entered security code (needed to connect to the gateway).
     */
    protected static final List<MetaItemDescriptor> PROTOCOL_CONFIG_META_ITEM_DESCRIPTORS = Arrays.asList(
            new MetaItemDescriptorImpl(META_TRADFRI_GATEWAY_HOST, ValueType.STRING, false, null, null, 1, null, false, null, null, null),
            new MetaItemDescriptorImpl(META_TRADFRI_SECURITY_CODE, ValueType.STRING, false, null, null, 1, null, false, null, null, null)
    );

    /**
     * Map to store/manage the connections to the gateway.
     */
    final protected Map<String, TradfriConnection> tradfriConnections = new HashMap<>();

    /**
     * Map to store/manage the TRÅDFRI devices.
     */
    final protected HashMap<String, Device> tradfriDevices = new HashMap<>();

    /**
     * Map to store/manage the consumer of the connection status.
     */
    final protected Map<AttributeRef, Consumer<ConnectionStatus>> statusConsumerMap = new HashMap<>();

    /**
     * Map to store/manage the attributes of the TRÅDFRI devices.
     */
    final protected Map<AttributeRef, Pair<TradfriConnection, Device>> attributeMap = new HashMap<>();

    /**
     * Gets the name of the protocol.
     * @return the name of the protocol.
     */
    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    /**
     * Gets the display name of the protocol.
     * @return the display name of the protocol.
     */
    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    /**
     * Gets the current version number of the protocol.
     * @return the current version number of the protocol.
     */
    @Override
    public String getVersion() {
        return VERSION;
    }

    /**
     * Gets the protocol configuration template.
     * @return the configuration template of the protocol.
     */
    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
                .addMeta(
                        new MetaItem(META_TRADFRI_GATEWAY_HOST, null),
                        new MetaItem(META_TRADFRI_SECURITY_CODE, null)
                );
    }

    /**
     * Gets the protocol configuration meta item descriptors.
     * @return the protocol configuration meta item descriptors.
     */
    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return PROTOCOL_CONFIG_META_ITEM_DESCRIPTORS;
    }

    /**
     * Gets the linked attribute meta item descriptors.
     * @return the linked attribute meta item descriptors.
     */
    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return null;
    }

    /**
     * Validates the protocol configuration.
     * @param protocolConfiguration the protocol configuration
     * @return the attribute validation result.
     */
    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration){
        AttributeValidationResult result = super.validateProtocolConfiguration(protocolConfiguration);
        if (result.isValid()) {
            boolean ipFound = false;
            if (protocolConfiguration.getMeta() != null && !protocolConfiguration.getMeta().isEmpty()) {
                for (int i = 0; i < protocolConfiguration.getMeta().size(); i++) {
                    MetaItem actionMetaItem = protocolConfiguration.getMeta().get(i);
                    if (isMetaNameEqualTo(actionMetaItem, META_TRADFRI_GATEWAY_HOST)) {
                        ipFound = true;
                        if (isNullOrEmpty(actionMetaItem.getValueAsString().orElse(null))) {
                            result.addMetaFailure(
                                    new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_VALUE_IS_REQUIRED, ValueType.STRING.name())
                            );
                        }
                    }
                }
            }
            if (!ipFound) {
                result.addMetaFailure(
                        new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_MISSING, META_TRADFRI_GATEWAY_HOST)
                );
            }
        }
        return result;
    }

    /**
     * Manages the configuration of the protocol.
     * Checks if assets are not duplicated, makes the connection to the gateway,
     * retrieves the devices, and adds a event handler for the gateway.
     * @param protocolConfiguration the protocol configuration.
     */
    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        Optional<String> gatewayIpParam = protocolConfiguration.getMetaItem(META_TRADFRI_GATEWAY_HOST).flatMap(AbstractValueHolder::getValueAsString);
        if (!gatewayIpParam.isPresent()) {
            LOG.severe("No Tradfri gateway IP address provided for protocol configuration: " + protocolConfiguration);
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.ERROR_CONFIGURATION);
            return;
        }

        String securityCode = protocolConfiguration.getMetaItem(META_TRADFRI_SECURITY_CODE).flatMap(AbstractValueHolder::getValueAsString).orElse("");
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        MetaItem agentLink = AgentLink.asAgentLinkMetaItem(protocolConfiguration.getReferenceOrThrow());
        synchronized (tradfriConnections) {
            Consumer<ConnectionStatus> statusConsumer = status -> updateStatus(protocolRef, status);

            TradfriConnection tradfriConnection = tradfriConnections.computeIfAbsent(
                    gatewayIpParam.get(), gatewayIp ->
                            new TradfriConnection(gatewayIp, securityCode, executorService)
            );
            tradfriConnection.addConnectionStatusConsumer(statusConsumer);
            Gateway gateway = tradfriConnection.connect();
            if(gateway != null){
                Device[] devices = gateway.getDevices();
                if(devices != null){
                    addDevices(devices, agentLink, protocolConfiguration);
                    EventHandler<GatewayEvent> gatewayEventHandler = new EventHandler<GatewayEvent>() {
                        @Override
                        public void handle(GatewayEvent event) {
                            Device[] newDevices = event.getGateway().getDevices();
                            if(newDevices == null) return;
                            ArrayList<Device> added = new ArrayList<>();
                            HashMap<String, Device> removed = (HashMap<String, Device>) tradfriDevices.clone();
                            for (Device device : newDevices) {
                                String name = UniqueIdentifierGenerator.generateId("tradfri_" + device.getInstanceId());
                                if (tradfriDevices.containsKey(name)) {
                                    removed.remove(name);
                                } else {
                                    added.add(device);
                                }
                            }
                            addDevices(added.toArray(new Device[added.size()]), agentLink, protocolConfiguration);
                            for (String removedDeviceId : removed.keySet()) {
                                tradfriDevices.remove(removedDeviceId);
                                assetService.deleteAsset(removedDeviceId);
                            }
                        }
                    };
                    gateway.addEventHandler(gatewayEventHandler);
                }
            } else {
                updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.ERROR_CONFIGURATION);
            }
            synchronized (statusConsumerMap) {
                statusConsumerMap.put(protocolRef, statusConsumer);
            }
        }
    }

    /**
     * Handles the disconnections of the protocol.
     * @param protocolConfiguration the protocol configuration.
     */
    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        Consumer<ConnectionStatus> statusConsumer;
        synchronized (statusConsumerMap) {
            statusConsumer = statusConsumerMap.get(protocolConfiguration.getReferenceOrThrow());
        }

        String gatewayIp = protocolConfiguration.getMetaItem(META_TRADFRI_GATEWAY_HOST).flatMap(AbstractValueHolder::getValueAsString).orElse("");
        synchronized (tradfriConnections) {
            TradfriConnection tradfriConnection = tradfriConnections.get(gatewayIp);
            if (tradfriConnection != null) {
                tradfriConnection.removeConnectionStatusConsumer(statusConsumer);
                for (Device device: tradfriDevices.values()) {
                    device.getEventHandlers().clear();
                    device.disableObserve();
                }

                tradfriConnection.disconnect();
                tradfriConnections.remove(gatewayIp);
            }
        }
    }

    /**
     * Links the attribute.
     * @param attribute the attribute of the asset.
     * @param protocolConfiguration the protocol configuration.
     */
    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
       String gatewayIp = protocolConfiguration.getMetaItem(META_TRADFRI_GATEWAY_HOST).flatMap(AbstractValueHolder::getValueAsString).orElse("");
       final AttributeRef attributeRef = attribute.getReferenceOrThrow();
       TradfriConnection tradfriConnection = getConnection(gatewayIp);
       if (tradfriConnection == null) return;
       if(tradfriDevices.containsKey(attributeRef.getEntityId())){
           Device device = tradfriDevices.get(attributeRef.getEntityId());
           addDevice(attributeRef, tradfriConnection, device);
       }
    }

    /**
     * Unlinks the attribute.
     * @param attribute the attribute of the asset.
     * @param protocolConfiguration the protocol configuration.
     */
    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        final AttributeRef attributeRef = attribute.getReferenceOrThrow();
        removeDevice(attributeRef);
    }

    /**
     * Handles the updates of the attribute values
     * @param event the event that contains the attribute ref.
     * @param processedValue the processed value.
     * @param protocolConfiguration the protocol configuration.
     */
    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, Value processedValue, AssetAttribute protocolConfiguration) {
        if (!protocolConfiguration.isEnabled()) {
            LOG.fine("Protocol configuration is disabled so ignoring write request");
            return;
        }

        synchronized (attributeMap) {
            Pair<TradfriConnection, Device> controlInfo = attributeMap.get(event.getAttributeRef());

            if (controlInfo == null) {
                LOG.fine("Attribute is not linked to a Tradfri light so cannot process event: " + event);
                return;
            }
            controlInfo.key.controlDevice(controlInfo.value, event);
        }
    }

    /**
     * Method to retrieve the connection.
     * @param gatewayIp the gateway IP address of the connection.
     * @return the corresponding TRÅDFRI connection.
     */
    protected TradfriConnection getConnection(String gatewayIp) {
        synchronized (tradfriConnections) {
            return tradfriConnections.get(gatewayIp);
        }
    }

    /**
     * Adds a device to the attributeMap.
     * By doing this, the attribute is registered for sending commands.
     * @param attributeRef the reference of the entity.
     * @param tradfriConnection the TRÅDFRI connection.
     * @param device the TRÅDFRI device that needs to be added to the map.
     */
    protected void addDevice(AttributeRef attributeRef, TradfriConnection tradfriConnection, Device device) {
        synchronized (attributeMap) {
            Pair<TradfriConnection, Device> controlInfo = attributeMap.get(attributeRef);
            if (controlInfo != null) {
                return;
            }
            attributeMap.put(attributeRef, new Pair<>(tradfriConnection, device));
            LOG.info("Attribute registered for sending commands: " + attributeRef + " with device: " + device);
        }
    }

    /**
     * Removes a device from the attributeMap.
     * By doing this, the attribute is unregistered for sending commands.
     * @param attributeRef the reference of the entity.
     */
    protected void removeDevice(AttributeRef attributeRef) {
        synchronized (attributeMap) {
            attributeMap.remove(attributeRef);
        }
    }

    /**
     * Checks the device type, and creates an asset for that device type.
     * @param devices the discovered devices.
     * @param agentLink the agent link.
     * @param protocolConfiguration the protocol configuration.
     */
    private void addDevices(Device[] devices, MetaItem agentLink, AssetAttribute protocolConfiguration){
        String parentId = null;
        Optional<String> assetId = protocolConfiguration.getAssetId();
        if(assetId.isPresent()) parentId = assetId.get();
        for (Device device : devices) {
            if (device.isPlug()) {
                Plug plug = device.toPlug();
                Asset asset = new TradfriPlugAsset(parentId, agentLink, plug, assetService);
                tradfriDevices.put(asset.getId(), plug);
            }
            else if (device.isLight()) {
                Light light = device.toLight();
                Asset asset = new TradfriLightAsset(parentId, agentLink, light, assetService);
                tradfriDevices.put(asset.getId(), light);
            }
        }
    }
}