package org.openremote.agent.protocol.dmx.artnet;

import com.google.gson.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.ProtocolLinkedAttributeImport;
import org.openremote.agent.protocol.artnet.ArtNetPacket;
import org.openremote.agent.protocol.dmx.AbstractDMXClientProtocol;
import org.openremote.agent.protocol.dmx.AbstractDMXLight;
import org.openremote.agent.protocol.dmx.AbstractDMXLightState;
import org.openremote.agent.protocol.io.IoClient;
import org.openremote.agent.protocol.knx.EtsFileUriResolver;
import org.openremote.agent.protocol.knx.KNXProtocol;
import org.openremote.agent.protocol.udp.AbstractUdpClient;
import org.openremote.container.Container;
import org.openremote.container.util.CodecUtil;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.*;
import org.openremote.model.file.FileInfo;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.*;
import org.openremote.model.value.impl.ArrayValueImpl;
import org.openremote.model.value.impl.NumberValueImpl;
import org.openremote.model.value.impl.StringValueImpl;
import tuwien.auto.calimero.datapoint.DatapointMap;
import tuwien.auto.calimero.datapoint.StateDP;
import tuwien.auto.calimero.xml.KNXMLException;
import tuwien.auto.calimero.xml.XmlInputFactory;
import tuwien.auto.calimero.xml.XmlReader;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemInteger;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemObject;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class ArtnetClientProtocol extends AbstractDMXClientProtocol implements ProtocolLinkedAttributeImport {

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":artnet";
    public static final String PROTOCOL_DISPLAY_NAME = "Artnet Client";
    private static final String PROTOCOL_VERSION = "1.0";
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, org.openremote.agent.protocol.artnet.ArtnetClientProtocol.class.getName());
    private final Map<AttributeRef, AttributeInfo> attributeInfoMap = new HashMap<>();
    private static int DEFAULT_RESPONSE_TIMEOUT_MILLIS = 3000;
    private static int DEFAULT_SEND_RETRIES = 1;
    private static int MIN_POLLING_MILLIS = 1000;
    public static final MetaItemDescriptor META_ARTNET_LIGHT_ID = metaItemInteger(
            "lightId",
            ACCESS_PRIVATE,
            true,
            0,
            Integer.MAX_VALUE
    );
    public static final MetaItemDescriptor META_ARTNET_CONFIGURATION = metaItemObject(
            PROTOCOL_NAME + ":areaConfiguration",
            ACCESS_PRIVATE,
            true,
            Values.createObject().putAll(new HashMap<String, Value>() {{
                put("lights", Values.createArray().add(Values.createObject().putAll(new HashMap<String, Value>() {{
                    put("lightId", Values.create(0));
                    put("universe", Values.create(0));
                    put("amountOfLeds", Values.create(3));
                }})));
            }})
    );


    private HashMap<Integer, AbstractDMXLightState> artnetLightMemory = new HashMap<Integer, AbstractDMXLightState>();

    @Override
    public Map<Integer, AbstractDMXLightState> getLightStateMemory() {
        return this.artnetLightMemory;
    }

    @Override
    public void updateLightInMemory(Integer lightId, AbstractDMXLightState updatedLightState) {
        Optional<Integer> foundLightId = this.artnetLightMemory.keySet().stream().filter(lid -> lid == lightId).findAny();
        if(foundLightId.orElse(null) != null) {
            ArtnetLightState lightState = (ArtnetLightState) this.artnetLightMemory.get(this.artnetLightMemory.keySet().stream().filter(lid -> lid == foundLightId.get()).findAny().orElse(null));
            this.artnetLightMemory.replace(foundLightId.get(), updatedLightState);
        }
            this.artnetLightMemory.get(this.artnetLightMemory.keySet().stream().filter(lid -> lid == lightId).findAny().get());
    }

    @Override
    protected IoClient<String> createIoClient(String host, int port, Integer bindPort, Charset charset, boolean binaryMode, boolean hexMode) {
        BiConsumer<ByteBuf, List<String>> decoder;
        BiConsumer<String, ByteBuf> encoder;

        if (hexMode) {
            decoder = (buf, messages) -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String msg = Protocol.bytesToHexString(bytes);
                messages.add(msg);
            };
            encoder = (message, buf) -> {
                byte[] bytes = Protocol.bytesFromHexString(message);
                buf.writeBytes(bytes);
            };
        } else if (binaryMode) {
            decoder = (buf, messages) -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                String msg = Protocol.bytesToBinaryString(bytes);
                messages.add(msg);
            };
            encoder = (message, buf) -> {
                byte[] bytes = Protocol.bytesFromBinaryString(message);
                buf.writeBytes(bytes);
            };
        } else {
            final Charset finalCharset = charset != null ? charset : CharsetUtil.UTF_8;
            decoder = (buf, messages) -> {
                String msg = buf.toString(finalCharset);
                messages.add(msg);
                buf.readerIndex(buf.readerIndex() + buf.readableBytes());
            };
            encoder = (message, buf) -> buf.writeBytes(message.getBytes(finalCharset));
        }

        BiConsumer<ByteBuf, List<String>> finalDecoder = decoder;
        BiConsumer<String, ByteBuf> finalEncoder = encoder;


        return new AbstractUdpClient<String>(host, port, bindPort, executorService) {

            @Override
            protected void decode(ByteBuf buf, List<String> messages) {
                finalDecoder.accept(buf, messages);
            }

            @Override
            protected void encode(String message, ByteBuf buf) {
                //Load states of all other lamps
                JsonParser parser = new JsonParser();
                JsonObject messageObject = parser.parse(message).getAsJsonObject();
                JsonArray array = messageObject.get("lights").getAsJsonArray();
                List<ArtnetLight> lights = new ArrayList<>();
                for(JsonElement element : array) {
                    lights.add(new Gson().fromJson(element, ArtnetLight.class));
                }

                //TODO SEND PROTOCOL PER GROUP. (PER UNIVERSE ALREADY WORKS AT THIS POINT)
                Map<Integer, List<ArtnetLight>> lightsPerUniverse = new HashMap<>();
                for(ArtnetLight light : lights) {
                    if(lightsPerUniverse.get(light.getUniverse()) == null)
                        lightsPerUniverse.put(light.getUniverse(), new ArrayList<>());
                    lightsPerUniverse.get(light.getUniverse()).add(light);
                }
                for(List<ArtnetLight> lightLists : lightsPerUniverse.values())
                    Collections.sort(lightLists, Comparator.comparingInt(ArtnetLight ::getLightId));
                for(Integer universeId : lightsPerUniverse.keySet()) {
                    ArtnetPacket.writePrefix(buf, universeId);
                    for(ArtnetLight lightToAddress : lightsPerUniverse.get(universeId)) {
                        ArtnetLightState lightState = (ArtnetLightState) artnetLightMemory.get(lightToAddress.getLightId());
                        ArtnetPacket.writeLight(buf, lightState.getValues(), lightToAddress.getAmountOfLeds());
                    }
                    ArtnetPacket.updateLength(buf);
                    //TODO MULTIPLE UNIVERSES APPENDED MIGHT NOT WORK. IF IT DOES NOT, CALL FINALENCODER.ACCEPT
                }

                /*
                List<String> lightIdsStrings = Arrays.asList(messageObject.get("lightIds").getAsString().split(","));
                int[] lightIds = new int[lightIdsStrings.size()];
                for (int i = 0; i < lightIdsStrings.size(); i++)
                    lightIds[i] = Integer.parseInt(lightIdsStrings.get(i));
                Arrays.sort(lightIds);

                // Create packet
                ArtnetPacket.writePrefix(buf, messageObject.get("universe").getAsInt());
                for (int lightId : lightIds) {
                    ArtnetLightState lightState = (ArtnetLightState) artnetLightMemoryget(lightId);
                    ArtnetPacket.writeLight(buf, lightState.getValues(), messageObject.get("amountOfLeds").getAsJsonObject().get(lightId + "").getAsInt());
                }
                ArtnetPacket.updateLength(buf);
                 */
                //Send packet (Look over it)
                try{
                    finalEncoder.accept("", buf);
                }catch(IllegalArgumentException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    //Runs if a new Asset is created with an ArtNet Client as attribute.
    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        super.doLinkProtocolConfiguration(protocolConfiguration);

    }

    //Runs if an Asset is deleted with an ArtNet Client as attribute.
    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        super.doUnlinkProtocolConfiguration(protocolConfiguration);
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return null;
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) throws IOException {
        MetaItem metaItem = protocolConfiguration.getMetaItem(META_ARTNET_CONFIGURATION.getUrn()).orElse(null);
        String configJsonString = metaItem.getValue().orElse(null).toJson();
        JsonObject configJson = new JsonParser().parse(configJsonString).getAsJsonObject();
        JsonArray jerry = configJson.getAsJsonArray("lights");

        for(JsonElement l : jerry)
        {
            JsonObject light = l.getAsJsonObject();
            int id = light.get("lightId").getAsInt();
            String[] requiredKeys = light.get("requiredValues").getAsString().split(",");
            ArtnetLightState state = new ArtnetLightState(id, new LinkedHashMap<String, Integer>(), 100, true);
            for(String key : requiredKeys)
                state.getReceivedValues().put(key, 0);
            artnetLightMemory.put(id, state);
        }

        if (!protocolConfiguration.isEnabled()) {
            LOG.info("Protocol configuration is disabled so ignoring: " + protocolConfiguration.getReferenceOrThrow());
            return;
        }

        ClientAndQueue clientAndQueue = clientMap.get(protocolConfiguration.getReferenceOrThrow());

        if (clientAndQueue == null) {
            return;
        }

        final Integer pollingMillis = Values.getMetaItemValueOrThrow(attribute, META_POLLING_MILLIS, false, true)
                .flatMap(Values::getIntegerCoerced)
                .orElse(null);

        final int responseTimeoutMillis = Values.getMetaItemValueOrThrow(attribute, META_RESPONSE_TIMEOUT_MILLIS, false, true)
                .flatMap(Values::getIntegerCoerced)
                .orElseGet(() ->
                        Values.getMetaItemValueOrThrow(protocolConfiguration, META_RESPONSE_TIMEOUT_MILLIS, false, true)
                                .flatMap(Values::getIntegerCoerced)
                                .orElse(DEFAULT_RESPONSE_TIMEOUT_MILLIS)
                );

        final int sendRetries = Values.getMetaItemValueOrThrow(attribute, META_SEND_RETRIES, false, true)
                .flatMap(Values::getIntegerCoerced)
                .orElseGet(() ->
                        Values.getMetaItemValueOrThrow(protocolConfiguration, META_SEND_RETRIES, false, true)
                                .flatMap(Values::getIntegerCoerced)
                                .orElse(DEFAULT_SEND_RETRIES)
                );

        Consumer<Value> sendConsumer = null;
        ScheduledFuture pollingTask = null;
        AttributeInfo info = new AttributeInfo(attribute.getReferenceOrThrow(), sendRetries, responseTimeoutMillis);

        if (!attribute.isReadOnly()) {
            sendConsumer = Protocol.createDynamicAttributeWriteConsumer(attribute, str ->
                    clientAndQueue.send(
                            str,
                            responseStr -> {
                                // TODO: Add discovery
                                // Just drop the response; something in the future could be used to verify send was successful
                            },
                            info));
        }

        if (pollingMillis != null && pollingMillis < MIN_POLLING_MILLIS) {
            LOG.warning("Polling ms must be >= " + MIN_POLLING_MILLIS);
            return;
        }

        final String writeValue = Values.getMetaItemValueOrThrow(attribute, META_ATTRIBUTE_WRITE_VALUE, false, true)
                .map(Object::toString).orElse(null);

        if (pollingMillis != null && TextUtil.isNullOrEmpty(writeValue)) {
            LOG.warning("Polling requires the META_ATTRIBUTE_WRITE_VALUE meta item to be set");
            return;
        }

        if (pollingMillis != null) {
            Consumer<String> responseConsumer = str -> {
                LOG.fine("Polling response received updating attribute: " + attribute.getReferenceOrThrow());
                updateLinkedAttribute(
                        new AttributeState(
                                attribute.getReferenceOrThrow(),
                                str != null ? Values.create(str) : null));
            };

            Runnable pollingRunnable = () -> clientAndQueue.send(writeValue, responseConsumer, info);
            pollingTask = schedulePollingRequest(clientAndQueue, attribute.getReferenceOrThrow(), pollingRunnable, pollingMillis);
        }

        attributeInfoMap.put(attribute.getReferenceOrThrow(), info);
        info.pollingTask = pollingTask;
        info.sendConsumer = sendConsumer;
    }

    protected ScheduledFuture schedulePollingRequest(ClientAndQueue clientAndQueue,
                                                     AttributeRef attributeRef,
                                                     Runnable pollingTask,
                                                     int pollingMillis) {

        LOG.fine("Scheduling polling request on client '" + clientAndQueue.client + "' to execute every " + pollingMillis + " ms for attribute: " + attributeRef);

        return executorService.scheduleWithFixedDelay(pollingTask, 0, pollingMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        /*
        int lightId = protocolConfiguration.getMetaItem(META_ARTNET_CONFIGURATION.getUrn()).get().getValueAsInteger().get();
        artnetLightStates.remove(lightId);
        */
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
        //TODO LATER, CHECK FOR GROUP

        Attribute attr =  getLinkedAttribute(event.getAttributeRef());
        MetaItem metaItem = attr.getMetaItem("lightId").orElse(null);
        List<ArtnetLight> lightsToSend = new ArrayList<>();
        int lampId = metaItem.getValueAsInteger().orElse(-1);
        int universeId = -1;
        int amountOfLeds = 0;
        ArrayValue protocolMetaItemsArray = protocolConfiguration.getObjectValue().getArray("meta").get();
        List<Integer> lightIdsWithinUniverse = new ArrayList<>();
        HashMap<String,Value> amountOfLedsPerLightId = new HashMap<String, Value>();
        for(int i = 0; i < protocolMetaItemsArray.length(); i++) {
            ObjectValue objvl = protocolMetaItemsArray.getObject(i).get();
            if(objvl.getString("name").get().equals("urn:openremote:protocol:artnet:areaConfiguration")) {
                String lightConfig = objvl.get("value").get().toJson();
                JsonObject configurationObject = new JsonParser().parse(lightConfig).getAsJsonObject();
                JsonArray jerry = configurationObject.get("lights").getAsJsonArray();
                for(JsonElement individualLightConfig : jerry) {
                    //TODO CHECK FOR THE GROUP WHICH UNIVERSES NEED TO BE UPDATED
                    if(individualLightConfig.getAsJsonObject().get("lightId").getAsInt() == lampId)
                        universeId = individualLightConfig.getAsJsonObject().get("universe").getAsInt();
                }
                for(JsonElement individualLightConfig : jerry) {
                    if(individualLightConfig.getAsJsonObject().get("universe").getAsInt() == universeId) {
                        //TODO CHECK FOR THE GROUP WHICH UNIVERSES NEED TO BE UPDATED
                        int childLightId = individualLightConfig.getAsJsonObject().get("lightId").getAsInt();
                        lightIdsWithinUniverse.add(individualLightConfig.getAsJsonObject().get("lightId").getAsInt());
                        amountOfLeds = individualLightConfig.getAsJsonObject().get("amountOfLeds").getAsInt();
                        amountOfLedsPerLightId.put(childLightId + "", Values.create(amountOfLeds));
                        String[] requiredValues = individualLightConfig.getAsJsonObject().get("requiredValues").getAsString().split(",");
                        byte[] prefix = configurationObject.get("protocolPrefix").getAsString().getBytes();
                        int groupId = 0;
                        lightsToSend.add(new ArtnetLight(childLightId, groupId, universeId, amountOfLeds, requiredValues));
                    }
                }
            }
        }

        artnetLightMemory.get(lampId).fromAttribute(event, attr);

        AttributeInfo info = attributeInfoMap.get(event.getAttributeRef());
        if (info == null || info.sendConsumer == null) {
            LOG.info("Request to write unlinked attribute or attribute that doesn't support writes so ignoring: " + event);
            return;
        }

        String lightIdsString = "";

        for(int lid : lightIdsWithinUniverse)
            if(lightIdsString.length() == 0)
                lightIdsString += lid;
            else
                lightIdsString += "," + lid;

        String finalLightIdsString = lightIdsString;
        int finalUniverseId = universeId;

        Value value = Values.createObject().putAll(new HashMap<String, Value>() {{
            put("lights", Values.convert(lightsToSend, Container.JSON).get());
        }});

        /*
        Value value = Values.createObject().putAll(new HashMap<String, Value>() {{
            put("universe", Values.create(finalUniverseId));
            put("lightIds", Values.create(finalLightIdsString));
            put("amountOfLeds", Values.createObject().putAll(amountOfLedsPerLightId));
        }});
        */

        info.sendConsumer.accept(value);
        updateLinkedAttribute(event.getAttributeState());
    }

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
        return PROTOCOL_VERSION;
    }

    @Override
    public AssetTreeNode[] discoverLinkedAssetAttributes(AssetAttribute protocolConfiguration, FileInfo fileInfo) throws IllegalStateException {

        String jsonString;
        if(fileInfo.isBinary())//Read any file that isn't an XML file
        {
            //Read from .json file || Works on files without extention || Works on CSV
            byte[] rawBinaryData = CodecUtil.decodeBase64(fileInfo.getContents());
            jsonString = new String(rawBinaryData);
        }
        else
            jsonString = fileInfo.getContents();//Read from .xml file

        JsonObject jobj = new JsonParser().parse(jsonString).getAsJsonObject();//Contents of the file as JSON object

        byte[] prefix = jobj.get("protocolPrefix").toString().getBytes();
        JsonArray jLights = jobj.getAsJsonArray("lights");

        MetaItem agentLink = AgentLink.asAgentLinkMetaItem(protocolConfiguration.getReferenceOrThrow());

        List<AssetTreeNode> output = new ArrayList<AssetTreeNode>();

        for (JsonElement jel : jLights) {

            int id = jel.getAsJsonObject().get("lightId").getAsInt();
            int groupId = jel.getAsJsonObject().get("groupId").getAsInt();
            String requiredValues = jel.getAsJsonObject().get("requiredValues").getAsString();

            output.add(createLightAsset(id, groupId, requiredValues, protocolConfiguration, agentLink));
        }

        return output.toArray(new AssetTreeNode[output.size()]);
    }

    protected AssetTreeNode createLightAsset(int id, int groupId, String requiredValues, AssetAttribute parent, MetaItem agentLink)
    {
        Asset light = new Asset();
        light.setParent(assetService.getAgent(parent));
        light.setName("ArtNetLight" + id);
        light.setType(AssetType.THING);
        light.setRealm(MASTER_REALM);

        //Append the required values to a HashMap, these are interpreted by the 'Values' JSON OBJECT parameter
        HashMap<String, Value> jsonProperties = new HashMap<String, Value>();

        List<String> requiredKeys = Arrays.asList(requiredValues.split(","));
        //TODO SORT BASED ON SEQUENCE IN CONFIG?
        for(String key : requiredKeys)
            jsonProperties.put(key, Values.create(0));

        //Create Attributes for the Asset
        List<AssetAttribute> lightAttributes = new ArrayList<>();
        lightAttributes.add(new AssetAttribute("Id", AttributeValueType.NUMBER, Values.create(id)));
        lightAttributes.add(light.getAttribute("Dim").orElse(new AssetAttribute("Dim", AttributeValueType.NUMBER, Values.create(100)).setMeta(
                new MetaItem(ArtnetClientProtocol.META_ARTNET_LIGHT_ID, Values.create(id)),
                agentLink
        )));
        lightAttributes.add(light.getAttribute("Switch").orElse(new AssetAttribute("Switch", AttributeValueType.BOOLEAN, Values.create(true)).setMeta(
                new MetaItem(ArtnetClientProtocol.META_ARTNET_LIGHT_ID, Values.create(id)),
                agentLink
        )));
        lightAttributes.add(light.getAttribute("Values").orElse(new AssetAttribute("Values", AttributeValueType.OBJECT, Values.createObject().putAll(jsonProperties)).setMeta(
                new MetaItem(ArtnetClientProtocol.META_ARTNET_LIGHT_ID, Values.create(id)),
                agentLink
        )));

        AssetTreeNode output = new AssetTreeNode(light);
        output.asset.setAttributes(lightAttributes);

        return output;
    }
}
