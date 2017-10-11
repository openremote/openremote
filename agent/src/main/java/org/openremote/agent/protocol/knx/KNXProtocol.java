package org.openremote.agent.protocol.knx;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItem.isMetaNameEqualTo;
import static org.openremote.model.util.TextUtil.REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO;
import static org.openremote.model.util.TextUtil.isNullOrEmpty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.model.asset.agent.ConnectionStatus;
import org.openremote.agent.protocol.ProtocolLinkedAttributeImport;
import org.openremote.container.util.Util;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetMeta;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.attribute.AttributeType;
import org.openremote.model.attribute.AttributeValidationResult;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.attribute.MetaItemDescriptorImpl;
import org.openremote.model.file.FileInfo;
import org.openremote.model.util.Pair;
import org.openremote.model.value.Value;
import org.openremote.model.value.ValueType;
import org.openremote.model.value.Values;

import tuwien.auto.calimero.GroupAddress;
import tuwien.auto.calimero.KNXException;
import tuwien.auto.calimero.KNXFormatException;
import tuwien.auto.calimero.datapoint.CommandDP;
import tuwien.auto.calimero.datapoint.Datapoint;
import tuwien.auto.calimero.datapoint.DatapointMap;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XmlInputFactory;
import tuwien.auto.calimero.xml.XmlReader;

/**
 * This protocol is used to connect to a KNX bus via an IP interface.
 */
public class KNXProtocol extends AbstractProtocol implements ProtocolLinkedAttributeImport {

    private static final Logger LOG = Logger.getLogger(KNXProtocol.class.getName());

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":knx";
    public static final String PROTOCOL_DISPLAY_NAME = "KNX";

    //Protocol specific configuration meta items
    /**
     * IP address of the KNX gateway to connect to in TUNNEL mode. Optional if {@link #META_KNX_IP_CONNECTION_TYPE} is ROUTING.<br>
     * In ROUTING mode, the IP address specifies the multicast group to join.
     */
    public static final String META_KNX_GATEWAY_IP = PROTOCOL_NAME + ":gatewayIp";
    
    /**
     * The KNX gateway port to connect to in TUNNEL mode. Not used in ROUTING mode.<br>
     * Default: 3671
     */
    public static final String META_KNX_GATEWAY_PORT = PROTOCOL_NAME + ":gatewayPort";
    
    /**
     * <code>TRUE</code> to use network address translation in TUNNELLING mode, <code>FALSE</code>
     * to use the default (non aware) mode; parameter is ignored for ROUTING<br>
     * Default: <code>FALSE</code>
     */
    public static final String META_KNX_GATEWAY_USENAT = PROTOCOL_NAME + ":useNAT";
    
    /**
     * ServiceMode mode of communication to open, <code>connectionType</code> is one of TUNNELLING or ROUTING<br>
     * Default: TUNNELLING
     */
    public static final String META_KNX_IP_CONNECTION_TYPE = PROTOCOL_NAME + ":connectionType";
    
    /**
     * Device individual address to use as source address in KNX messages.<br>
     * Default: 0.0.0
     */
    public static final String META_KNX_LOCAL_BUS_ADDRESS = PROTOCOL_NAME + ":localBusAddress";
    
    /**
     * Local IP address or hostname to establish connection from.<br>
     * Default: hostname
     */
    public static final String META_KNX_LOCAL_IP = PROTOCOL_NAME + ":localIP";
    
    
    //Attribute specific configuration meta items
    public static final String META_KNX_DPT = PROTOCOL_NAME + ":dpt";
    public static final String META_KNX_STATUS_GA = PROTOCOL_NAME + ":statusGA";
    public static final String META_KNX_ACTION_GA = PROTOCOL_NAME + ":actionGA";

    protected static final String VERSION = "1.0";

    public static final String REGEXP_GROUP_ADDRESS = "^\\d{1,3}/\\d{1,3}/\\d{1,3}$";
    public static final String REGEXP_BUS_ADDRESS = "^\\d\\.\\d\\.\\d$";
    public static final String REGEXP_DPT = "^\\d{1,3}\\.\\d{1,3}$";
    public static final String PATTERN_FAILURE_CONNECTION_TYPE = "TUNNELLING|ROUTING";
    public static final String PATTERN_FAILURE_DPT = "KNX DPT (e.g. 1.001)";
    public static final String PATTERN_FAILURE_GROUP_ADDRESS = "KNX Group Address (e.g. 1/1/1)";

    protected static final List<MetaItemDescriptor> PROTOCOL_CONFIG_META_ITEM_DESCRIPTORS = Arrays.asList(
        new MetaItemDescriptorImpl("PROTOCOL_KNX_IP", META_KNX_GATEWAY_IP, ValueType.STRING, false, null, null, 1, null, false),
        new MetaItemDescriptorImpl("PROTOCOL_KNX_PORT", META_KNX_GATEWAY_PORT, ValueType.NUMBER, false, REGEXP_PATTERN_INTEGER_POSITIVE_NON_ZERO, MetaItemDescriptor.PatternFailure.INTEGER_POSITIVE_NON_ZERO.name(), 1, null, false),
        new MetaItemDescriptorImpl("PROTOCOL_KNX_USENAT", META_KNX_GATEWAY_USENAT, ValueType.BOOLEAN, false, null, null, 1, Values.create(false), false),
        new MetaItemDescriptorImpl("PROTOCOL_KNX_CONNECTION_TYPE", META_KNX_IP_CONNECTION_TYPE, ValueType.STRING, false, "^(TUNNELLING|ROUTING)$", PATTERN_FAILURE_CONNECTION_TYPE, 1, Values.create("TUNNELLING"), false),
        new MetaItemDescriptorImpl("PROTOCOL_KNX_LOCAL_BUS_ADDRESS", META_KNX_LOCAL_BUS_ADDRESS, ValueType.STRING, false, REGEXP_BUS_ADDRESS, "0.0.0", 1, null, false),
        new MetaItemDescriptorImpl("PROTOCOL_KNX_LOCAL_IP", META_KNX_LOCAL_IP, ValueType.STRING, false, null, null, 1, null, false)
    );

    protected static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
        new MetaItemDescriptorImpl("PROTOCOL_KNX_DPT", META_KNX_DPT, ValueType.STRING, true, REGEXP_DPT, PATTERN_FAILURE_DPT, 1, null, false),
        new MetaItemDescriptorImpl("PROTOCOL_KNX_STATUS_GA", META_KNX_STATUS_GA, ValueType.STRING, false, REGEXP_GROUP_ADDRESS, PATTERN_FAILURE_GROUP_ADDRESS, 1, null, false),
        new MetaItemDescriptorImpl("PROTOCOL_KNX_ACTION_GA", META_KNX_ACTION_GA, ValueType.STRING, false, REGEXP_GROUP_ADDRESS, PATTERN_FAILURE_GROUP_ADDRESS, 1, Values.create(false), false)
    );

    final protected Map<String, KNXConnection> knxConnections = new HashMap<>();
    final protected Map<AttributeRef, Consumer<ConnectionStatus>> statusConsumerMap = new HashMap<>();
    final protected Map<AttributeRef, Pair<KNXConnection, Datapoint>> attributeActionMap = new HashMap<>();
    final protected Map<AttributeRef, Pair<KNXConnection, StateDP>> attributeStatusMap = new HashMap<>();
    
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
                new MetaItem(META_KNX_IP_CONNECTION_TYPE, Values.create("TUNNELLING")),
                new MetaItem(META_KNX_GATEWAY_IP, null)
            );
    }

    @Override
    public AttributeValidationResult validateProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeValidationResult result = super.validateProtocolConfiguration(protocolConfiguration);
        if (result.isValid()) {
            boolean ipFound = false;

            if (protocolConfiguration.getMeta() != null && !protocolConfiguration.getMeta().isEmpty()) {
                for (int i = 0; i < protocolConfiguration.getMeta().size(); i++) {
                    MetaItem actionMetaItem = protocolConfiguration.getMeta().get(i);
                    if (isMetaNameEqualTo(actionMetaItem, META_KNX_IP_CONNECTION_TYPE)) {
                        String connectionType = actionMetaItem.getValueAsString().orElse("TUNNELLING");
                        if (!connectionType.equals("TUNNELLING") && !connectionType.equals("ROUTING")) {
                            result.addMetaFailure(
                                new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_VALUE_MISMATCH, PATTERN_FAILURE_CONNECTION_TYPE)
                            );
                        }

                        ipFound = "ROUTING".equals(connectionType);
                    } else if (isMetaNameEqualTo(actionMetaItem, META_KNX_GATEWAY_IP)) {
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
                    new ValidationFailure(MetaItem.MetaItemFailureReason.META_ITEM_MISSING, META_KNX_GATEWAY_IP)
                );
            }
        }

        return result;
    }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return PROTOCOL_CONFIG_META_ITEM_DESCRIPTORS;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return ATTRIBUTE_META_ITEM_DESCRIPTORS;
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        String connectionType = protocolConfiguration.getMetaItem(META_KNX_IP_CONNECTION_TYPE).flatMap(AbstractValueHolder::getValueAsString).orElse("TUNNELLING");
        if (!connectionType.equals("TUNNELLING") && !connectionType.equals("ROUTING")) {
            LOG.severe("KNX connectionType can either be 'TUNNELLING' or 'ROUTING' for protocol configuration: " + protocolConfiguration);
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.ERROR_CONFIGURATION);
            return;
        }
        
        Optional<String> gatewayIpParam = protocolConfiguration.getMetaItem(META_KNX_GATEWAY_IP).flatMap(AbstractValueHolder::getValueAsString);
        // RT: KNXConnection constructor implies gateway IP is always required so removed TUNNELLING only check here
        if (!gatewayIpParam.isPresent()) {
            LOG.severe("No KNX gateway IP address provided for protocol configuration: " + protocolConfiguration);
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.ERROR_CONFIGURATION);
            return;
        }

        if (!protocolConfiguration.isEnabled()) {
            updateStatus(protocolConfiguration.getReferenceOrThrow(), ConnectionStatus.DISABLED);
            return;
        }
        
        String localIp = protocolConfiguration.getMetaItem(META_KNX_LOCAL_IP).flatMap(AbstractValueHolder::getValueAsString).orElse(null);
        Integer remotePort = protocolConfiguration.getMetaItem(META_KNX_GATEWAY_PORT).flatMap(AbstractValueHolder::getValueAsInteger).orElse(3671);
        String localKNXAddress = protocolConfiguration.getMetaItem(META_KNX_LOCAL_BUS_ADDRESS).flatMap(AbstractValueHolder::getValueAsString).orElse("0.0.0");
        Boolean useNat = protocolConfiguration.getMetaItem(META_KNX_GATEWAY_USENAT).flatMap(AbstractValueHolder::getValueAsBoolean).orElse(Boolean.FALSE);
        
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        synchronized (knxConnections) {
            Consumer<ConnectionStatus> statusConsumer = status -> updateStatus(protocolRef, status);

            KNXConnection knxConnection = knxConnections.computeIfAbsent(
                            gatewayIpParam.get(), gatewayIp ->
                    new KNXConnection(gatewayIp, connectionType, executorService, localIp, remotePort, useNat, localKNXAddress)
            );
            knxConnection.addConnectionStatusConsumer(statusConsumer);
            knxConnection.connect();

            synchronized (statusConsumerMap) {
                statusConsumerMap.put(protocolRef, statusConsumer);
            }
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {

        Consumer<ConnectionStatus> statusConsumer;
        synchronized (statusConsumerMap) {
            statusConsumer = statusConsumerMap.get(protocolConfiguration.getReferenceOrThrow());
        }

        String gatewayIp = protocolConfiguration.getMetaItem(META_KNX_GATEWAY_IP).flatMap(AbstractValueHolder::getValueAsString).orElse("");
        synchronized (knxConnections) {
            KNXConnection knxConnection = knxConnections.get(gatewayIp);
            if (knxConnection != null) {
                if (!isKNXConnectionStillUsed(knxConnection)) {
                    knxConnection.removeConnectionStatusConsumer(statusConsumer);
                    knxConnection.disconnect();
                    knxConnections.remove(gatewayIp);
                }
            }
        }
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        String gatewayIp = protocolConfiguration.getMetaItem(META_KNX_GATEWAY_IP).flatMap(AbstractValueHolder::getValueAsString).orElse("");
        final AttributeRef attributeRef = attribute.getReferenceOrThrow();

        // Check there is a META_KNX_DPT
        Optional<String> dpt = attribute.getMetaItem(META_KNX_DPT).flatMap(AbstractValueHolder::getValueAsString);
        if (!dpt.isPresent()) {
            LOG.severe("No META_KNX_DPT for protocol attribute: " + attributeRef);
            return;
        }

        Optional<String> statusGA = attribute.getMetaItem(META_KNX_STATUS_GA).flatMap(AbstractValueHolder::getValueAsString);
        Optional<String> actionGA = attribute.getMetaItem(META_KNX_ACTION_GA).flatMap(AbstractValueHolder::getValueAsString);

        if (!statusGA.isPresent() && !actionGA.isPresent()) {
            LOG.warning("No status group address or action group address provided so nothing to do for protocol attribute: " + attributeRef);
            return;
        }

        KNXConnection knxConnection = getConnection(gatewayIp);

        if (knxConnection == null) {
            // Means that the protocol configuration is disabled
            return;
        }

        // If this attribute relates to a read groupthen start monitoring that measurement and broadcast any changes to the value
        statusGA.ifPresent(groupAddress -> {
            try {
                addStatusDatapoint(attributeRef, knxConnection, groupAddress, dpt.get());
            } catch (KNXFormatException e) {
                LOG.severe("Give action group address is invalid for protocol attribute: " + attributeRef + " - " + e.getMessage());
            }
        });

        // If this attribute relates to an action then store it
        actionGA.ifPresent(groupAddress -> {
            try {
                addActionDatapoint(attributeRef, knxConnection, groupAddress, dpt.get());
            } catch (KNXFormatException e) {
                LOG.severe("Give action group address is invalid for protocol attribute: " + attributeRef + " - " + e.getMessage());
            }
        });
    }


    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        final AttributeRef attributeRef = attribute.getReferenceOrThrow();

        // If this attribute is registered for status updates then un-subscribe it
        removeStatusDatapoint(attributeRef);

        // If this attribute has a stored action then remove it
        removeActionDatapoint(attributeRef);
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
        if (!protocolConfiguration.isEnabled()) {
            LOG.fine("Protocol configuration is disabled so ignoring write request");
            return;
        }

        synchronized (attributeActionMap) {
            Pair<KNXConnection, Datapoint> controlInfo = attributeActionMap.get(event.getAttributeRef());

            if (controlInfo == null) {
                LOG.fine("Attribute isn't linked to a KNX datapoint so cannot process write: " + event);
                return;
            }

            controlInfo.key.sendCommand(controlInfo.value, event.getValue());

            // We assume KNX actuator will send new status on relevant status group address which will be picked up by listener and updates the state again later
            updateLinkedAttribute(event.getAttributeState());
        }
    }

    protected KNXConnection getConnection(String gatewayIp) {
        synchronized (knxConnections) {
            return knxConnections.get(gatewayIp);
        }
    }
    
    protected void addActionDatapoint(AttributeRef attributeRef, KNXConnection knxConnection, String groupAddress, String dpt) throws KNXFormatException {
        synchronized (attributeActionMap) {
            Pair<KNXConnection, Datapoint> controlInfo = attributeActionMap.get(attributeRef);
            if (controlInfo != null) {
                return;
            }

            Datapoint datapoint = new CommandDP(new GroupAddress(groupAddress), attributeRef.getAttributeName());
            datapoint.setDPT(0, dpt);

            attributeActionMap.put(attributeRef, new Pair<>(knxConnection, datapoint));
            LOG.info("Attribute registered for sending commands: " + attributeRef + " with datapoint: " + datapoint);
        }
    }
    
    protected void removeActionDatapoint(AttributeRef attributeRef) {
        synchronized (attributeActionMap) {
            attributeActionMap.remove(attributeRef);
        }
    }

    protected void addStatusDatapoint(AttributeRef attributeRef, KNXConnection knxConnection, String groupAddress, String dpt) throws KNXFormatException {
        synchronized (attributeStatusMap) {
            Pair<KNXConnection, StateDP> controlInfo = attributeStatusMap.get(attributeRef);
            if (controlInfo != null) {
                return;
            }

            StateDP datapoint = new StateDP(new GroupAddress(groupAddress), attributeRef.getAttributeName(), 0, dpt);
            knxConnection.addDatapointValueConsumer(datapoint, value -> handleKNXValueChange(attributeRef, value));
           
            attributeStatusMap.put(attributeRef, new Pair<>(knxConnection, datapoint));
            LOG.info("Attribute registered for status updates: " + attributeRef + " with datapoint: " + datapoint);
        }
    }
    
    protected void handleKNXValueChange(AttributeRef attributeRef, Value value) {
        LOG.fine("KNX protocol received value '" + value + "' for : " + attributeRef);
        updateLinkedAttribute(new AttributeState(attributeRef, value));
    }
    
    protected void removeStatusDatapoint(AttributeRef attributeRef) {
        synchronized (attributeStatusMap) {
            Pair<KNXConnection, StateDP> controlInfo = attributeStatusMap.remove(attributeRef);
            if (controlInfo != null) {
                controlInfo.key.removeDatapointValueConsumer(controlInfo.value);
            }
        }
    }
    
    
    public Map<AttributeRef, Pair<KNXConnection, Datapoint>> getAttributeActionMap() {
        return attributeActionMap;
    }

    protected boolean isKNXConnectionStillUsed(KNXConnection knxConnection) {
        boolean clientStillUsed;

        synchronized (attributeStatusMap) {
            clientStillUsed = attributeStatusMap.values().stream().anyMatch(p -> p.key == knxConnection);
        }

        if (!clientStillUsed) {
            synchronized (attributeActionMap) {
                clientStillUsed = attributeActionMap.values().stream().anyMatch(p -> p.key == knxConnection);
            }
        }

        return clientStillUsed;
    }

    @Override
    public Asset[] discoverLinkedAssetAttributes(AssetAttribute protocolConfiguration, FileInfo fileInfo) throws IllegalStateException {
        ZipInputStream zin = null;

        try {
            boolean fileFound = false;
            byte[] data = Util.decodeBase64(fileInfo.getContents());
            zin = new ZipInputStream(new ByteArrayInputStream(data));
            ZipEntry zipEntry = zin.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.getName().endsWith("/0.xml")) {
                    fileFound = true;
                    break;
                }
                zipEntry = zin.getNextEntry();
            }

            if (!fileFound) {
                String msg = "Failed to find '0.xml' in project file";
                LOG.info(msg);
                throw new IllegalStateException(msg);
            }

            // Create a transform factory instance.
            System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
            TransformerFactory tfactory = TransformerFactory.newInstance();

            // Create a transformer for the stylesheet.
            Transformer transformer = tfactory.newTransformer(new StreamSource(this.getClass().getResourceAsStream("/org/openremote/agent/protocol/knx/ets_calimero_group_name.xsl")));

            // Set the URIResolver
            transformer.setURIResolver(new EtsFileUriResolver(data));

            // Transform the source XML into byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            transformer.transform(new StreamSource(zin), new StreamResult(bos));
            byte[] result = bos.toByteArray();

            // we use a map of state-based datapoints and read from the transformed xml
            final DatapointMap<StateDP> datapoints = new DatapointMap<>();
            try (final XmlReader r = XmlInputFactory.newInstance().createXMLStreamReader(new ByteArrayInputStream(result))) {
                datapoints.load(r);
            } catch (final KNXMLException e) {
                String msg = "Error loading parsed ETS file: " + e.getMessage();
                LOG.warning(msg);
                throw new IllegalStateException(msg, e);
            }

            MetaItem agentLink = AgentLink.asAgentLinkMetaItem(protocolConfiguration.getReferenceOrThrow());
            Map<String, Asset> createdAssets = new HashMap<>();
            for (StateDP dp : datapoints.getDatapoints()) {
                if (dp.getName().endsWith("#A")) {
                    createAsset(dp, false, agentLink, createdAssets);
                } else if (dp.getName().endsWith("#S")) {
                    createAsset(dp, true, agentLink, createdAssets);
                } else if (dp.getName().endsWith("#SA") || dp.getName().endsWith("#AS")) {
                    createAsset(dp, false, agentLink, createdAssets);
                    createAsset(dp, true, agentLink, createdAssets);
                } else {
                    LOG.info("Only group addresses ending on #A, #S, #AS or #SA will be imported. Ignoring: " + dp.getName());
                }
            }
            
            return createdAssets.values().toArray(new Asset[createdAssets.values().size()]);
            
        } catch (Exception e) {
            throw new IllegalStateException("ETS import error", e);
        } finally {
            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    protected void createAsset(StateDP datapoint, boolean isStatusGA, MetaItem agentLink, Map<String, Asset> createdAssets) throws KNXException {
        String name = datapoint.getName().substring(0, datapoint.getName().length()-3);
        String assetName = name.replaceAll(" -.*-", "");
        Asset asset;
        if (createdAssets.containsKey(assetName)) {
            asset = createdAssets.get(assetName);
        } else {
            asset = new Asset(assetName, AssetType.THING);
        }

        String attrName = assetName.replaceAll(" ", "");
        AttributeType type = TypeMapper.toAttributeType(datapoint);

        AssetAttribute attr = asset.getAttribute(attrName).orElse(new AssetAttribute(attrName, type).setMeta(
                        new MetaItem(AssetMeta.LABEL, Values.create(name)), 
                        new MetaItem(KNXProtocol.META_KNX_DPT, Values.create(datapoint.getDPT())),
                        agentLink
        ));
        if (isStatusGA) {
            attr.addMeta(new MetaItem(KNXProtocol.META_KNX_STATUS_GA, Values.create(datapoint.getMainAddress().toString())));
        } else {
            attr.addMeta(new MetaItem(KNXProtocol.META_KNX_ACTION_GA, Values.create(datapoint.getMainAddress().toString())));
        }

        if (!asset.hasAttribute(attrName)) {
            asset.addAttributes(attr);
        }
        createdAssets.put(assetName, asset);
    }

}
