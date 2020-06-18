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
import org.openremote.model.rules.flow.Option;
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
 * This protocol is used to connect to a Tradfri Gateway
 */
public class TradfriProtocol extends AbstractProtocol {

    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, TradfriProtocol.class);

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":tradfri";
    public static final String PROTOCOL_DISPLAY_NAME = "IKEA TRÅDFRI";

    /**
     * IP address of the Tradfri gateway to connect to
     */
    public static final String META_TRADFRI_GATEWAY_HOST = PROTOCOL_NAME + ":gatewayHost";

    /**
     * Secret key that is needed to connect to the Tradfri gateway
     */
    public static final String META_TRADFRI_SECURITY_CODE = PROTOCOL_NAME + ":psk";

    protected static final String VERSION = "1.0";

    protected static final List<MetaItemDescriptor> PROTOCOL_CONFIG_META_ITEM_DESCRIPTORS = Arrays.asList(
            new MetaItemDescriptorImpl(META_TRADFRI_GATEWAY_HOST, ValueType.STRING, false, null, null, 1, null, false, null, null, null),
            new MetaItemDescriptorImpl(META_TRADFRI_SECURITY_CODE, ValueType.STRING, false, null, null, 1, null, false, null, null, null)
    );

    final protected Map<String, TradfriConnection> tradfriConnections = new HashMap<>();
    final protected HashMap<String, Device> tradfriDevices = new HashMap<>();
    final protected Map<AttributeRef, Consumer<ConnectionStatus>> statusConsumerMap = new HashMap<>();
    final protected Map<AttributeRef, Pair<TradfriConnection, Device>> attributeMap = new HashMap<>();

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    public String getProtocolDisplayName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
                .addMeta(
                        new MetaItem(META_TRADFRI_GATEWAY_HOST, null),
                        new MetaItem(META_TRADFRI_SECURITY_CODE, null)
                );
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return PROTOCOL_CONFIG_META_ITEM_DESCRIPTORS;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return null;
    }

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
                tradfriConnection.disconnect();
                tradfriConnections.remove(gatewayIp);
            }
        }
    }

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

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        final AttributeRef attributeRef = attribute.getReferenceOrThrow();
        removeDevice(attributeRef);
    }

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

    protected TradfriConnection getConnection(String gatewayIp) {
        synchronized (tradfriConnections) {
            return tradfriConnections.get(gatewayIp);
        }
    }

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

    protected void removeDevice(AttributeRef attributeRef) {
        synchronized (attributeMap) {
            attributeMap.remove(attributeRef);
        }
    }
}