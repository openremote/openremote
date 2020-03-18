package org.openremote.agent.protocol.dmx.artnet;

import com.google.gson.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import javassist.bytecode.AttributeInfo;
import jdk.nashorn.internal.parser.JSONParser;
import org.openremote.agent.protocol.Protocol;
import org.openremote.agent.protocol.ProtocolLinkedAttributeImport;
import org.openremote.agent.protocol.dmx.AbstractArtnetClientProtocol;
import org.openremote.agent.protocol.dmx.AbstractArtnetLight;
import org.openremote.agent.protocol.dmx.AbstractArtnetLightState;
import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.agent.protocol.udp.AbstractUdpClientProtocol;
import org.openremote.agent.protocol.udp.UdpIoClient;
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
import org.openremote.model.util.Pair;
import org.openremote.model.value.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static org.openremote.container.util.Util.joinCollections;
import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemInteger;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemObject;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class ArtnetClientProtocol extends AbstractArtnetClientProtocol<String> implements ProtocolLinkedAttributeImport {
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":artnet";
    public static final String PROTOCOL_DISPLAY_NAME = "Artnet Client";
    private static final String PROTOCOL_VERSION = "1.69";
    private static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ArtnetClientProtocol.class.getName());
    private final Map<AttributeRef, AttributeInfo> attributeInfoMap = new HashMap<>();
    private static int DEFAULT_RESPONSE_TIMEOUT_MILLIS = 3000;
    private static int DEFAULT_SEND_RETRIES = 1;
    private static int MIN_POLLING_MILLIS = 1000;

    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = joinCollections(AbstractArtnetClientProtocol.PROTOCOL_META_ITEM_DESCRIPTORS, AbstractIoClientProtocol.PROTOCOL_GENERIC_META_ITEM_DESCRIPTORS);

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

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_ATTRIBUTE_MATCH_FILTERS,
            META_ATTRIBUTE_MATCH_PREDICATE,
            META_ARTNET_LIGHT_ID,
            META_ARTNET_CONFIGURATION);

    protected final Map<AttributeRef, List<Pair<AttributeRef, Consumer<String>>>> protocolMessageConsumers = new HashMap<>();

    private HashMap<Integer, List<AbstractArtnetLight>> artnetLightMemory = new HashMap<Integer, List<AbstractArtnetLight>>();

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
    public Map<Integer, List<AbstractArtnetLight>> getLightMemory() { return artnetLightMemory; }

    @Override
    protected List<MetaItemDescriptor> getProtocolConfigurationMetaItemDescriptors() {
        return PROTOCOL_META_ITEM_DESCRIPTORS;
    }

    @Override
    protected List<MetaItemDescriptor> getLinkedAttributeMetaItemDescriptors() {
        return ATTRIBUTE_META_ITEM_DESCRIPTORS;
    }

    @Override
    public AssetAttribute getProtocolConfigurationTemplate() {
        return super.getProtocolConfigurationTemplate()
                .addMeta(
                        new MetaItem(META_PROTOCOL_HOST, null),
                        new MetaItem(META_PROTOCOL_PORT, null)
                );
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        synchronized (protocolMessageConsumers) {
            protocolMessageConsumers.remove(protocolConfiguration.getReferenceOrThrow());
        }
        super.doUnlinkProtocolConfiguration(protocolConfiguration);
    }

    @Override
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider(UdpIoClient<String> client, AssetAttribute protocolConfiguration) {
        Supplier<ChannelHandler[]> encoderDecoderProvider = () -> {
            List<ChannelHandler> encodersDecoders = new ArrayList<>();
            encodersDecoders.add(new AbstractNettyIoClient.MessageToByteEncoder<String>(String.class, client, new BiConsumer<String, ByteBuf>() {
                @Override
                public void accept(String message, ByteBuf buf) {
                    //MESSAGE CONTAINS ALL THE LIGHTS THAT NEED TO BE UPDATED FROM ONE SINGLE UNIVERSE
                    //Load states of all other lamps
                    JsonParser parser = new JsonParser();
                    JsonObject messageObject = parser.parse(message).getAsJsonObject();
                    List<ArtnetLight> lights = new ArrayList<>();
                    JsonArray array = messageObject.get("lights").getAsJsonArray();
                    for(JsonElement jelem : array) {
                        JsonObject jobject = jelem.getAsJsonObject();
                        List<String> requiredValues = new ArrayList<String>();
                        ArtnetLightState lightState = new ArtnetLightState(jobject.get("lightId").getAsInt(),
                                new HashMap<String, Integer>(),
                                0, true);
                        for(JsonElement jel : jobject.get("requiredValues").getAsJsonArray()) {
                            requiredValues.add(jel.getAsString());
                            lightState.getReceivedValues().put(jel.getAsString(), jobject.get("lightState").getAsJsonObject().get("receivedValues").getAsJsonObject().get(jel.getAsString()).getAsInt());
                        }
                        lights.add(new ArtnetLight(jobject.get("lightId").getAsInt(),
                                                    jobject.get("groupId").getAsInt(),
                                                    jobject.get("universe").getAsInt(),
                                                    jobject.get("amountOfLeds").getAsInt(),
                                                    requiredValues.stream().toArray(String[]::new),
                                                    lightState,
                                                    null)); //SHOULD BE BYTE PREFIX LATER
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
                            //ArtnetLightState lightState = (ArtnetLightState) artnetLightMemory.get(lightToAddress.getLightId()).getLightState();
                            //ArtnetPacket.writeLight(buf, lightState.getValues(), lightToAddress.getAmountOfLeds());
                        }
                        ArtnetPacket.updateLength(buf);
                        //TODO MULTIPLE UNIVERSES APPENDED MIGHT NOT WORK. IF IT DOES NOT, CALL FINALENCODER.ACCEPT
                    }
                }
            }));
            return encodersDecoders.toArray(new ChannelHandler[0]);
        };
        return encoderDecoderProvider;
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        Consumer<String> messageConsumer = Protocol.createGenericAttributeMessageConsumer(attribute, assetService, this::updateLinkedAttribute);

        if (messageConsumer != null) {
            synchronized (protocolMessageConsumers) {
                protocolMessageConsumers.compute(protocolRef, (ref, consumers) -> {
                    if (consumers == null) {
                        consumers = new ArrayList<>();
                    }
                    consumers.add(new Pair<>(
                            attribute.getReferenceOrThrow(),
                            messageConsumer
                    ));
                    return consumers;
                });
            }
        }

        //TODO FIND A GOOD PLACE FOR THIS, DUPLICATE RIGHT NOW AT doLinkProtocol AND writeProtocol
        Asset parentAsset = assetService.findAsset(getLinkedAttribute(attribute.getReference().get()).getAssetId().get());
        //THIS NOW RELIES ON ALWAYS HAVING THE FOLLOWING ATTRIBUTES IN THE SAME LEVEL
        AssetAttribute lightIdAttribute = parentAsset.getAttribute("Id").get();
        AssetAttribute groupIdAttribute = parentAsset.getAttribute("GroupId").get();
        AssetAttribute universeAttribute = parentAsset.getAttribute("Universe").get();
        AssetAttribute amountOfLedsAttribute = parentAsset.getAttribute("AmountOfLeds").get();
        AssetAttribute requiredValuesAttribute = parentAsset.getAttribute("RequiredValues").get();

        Integer lightId = lightIdAttribute.getValueAsInteger().get();
        Integer groupId = groupIdAttribute.getValueAsInteger().get();
        Integer universe = universeAttribute.getValueAsInteger().get();
        Integer amountOfLeds = amountOfLedsAttribute.getValueAsInteger().get();
        String requiredValues = requiredValuesAttribute.getValueAsString().get();
        //ASSUME REQUIRED VALUES IS IN CSV FORMAT
        String[] requiredKeys = requiredValues.split(",");
        ArtnetLightState state = new ArtnetLightState(lightId, new LinkedHashMap<String, Integer>(), 100, true);
        for(String key : requiredKeys)
            state.getReceivedValues().put(key, 0);
        ArtnetLight lightToCreate = new ArtnetLight(lightId, groupId, universe, amountOfLeds, requiredKeys, state, null);
        if(artnetLightMemory.get(universe) == null)
            artnetLightMemory.put(universe, new ArrayList<AbstractArtnetLight>());
        if(!artnetLightMemory.get(universe).stream().anyMatch(light -> light.getLightId() == lightToCreate.getLightId()))
            artnetLightMemory.get(universe).add(lightToCreate);
        System.out.println(artnetLightMemory.size());
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        synchronized (protocolMessageConsumers) {
            protocolMessageConsumers.compute(protocolConfiguration.getReferenceOrThrow(), (ref, consumers) -> {
                if (consumers != null) {
                    consumers.removeIf((attrRefConsumer) -> attrRefConsumer.key.equals(attributeRef));
                }
                return consumers;
            });
        }
        //TODO FIND A GOOD PLACE FOR THIS, DUPLICATE RIGHT NOW AT doLinkProtocol AND writeProtocol
        Asset parentAsset = assetService.findAsset(getLinkedAttribute(attribute.getReference().get()).getAssetId().get());
        //THIS NOW RELIES ON ALWAYS HAVING THE FOLLOWING ATTRIBUTES IN THE SAME LEVEL
        AssetAttribute lightIdAttribute = parentAsset.getAttribute("Id").get();
        AssetAttribute universeAttribute = parentAsset.getAttribute("Universe").get();

        Integer lightId = lightIdAttribute.getValueAsInteger().get();
        Integer universe = universeAttribute.getValueAsInteger().get();
        //ASSUME REQUIRED VALUES IS IN CSV FORMAT
        if(artnetLightMemory.get(universe).stream().filter(light -> light.getLightId() == lightId).findFirst().get() != null)
            artnetLightMemory.get(universe).remove(artnetLightMemory.get(universe).stream().filter(light -> light.getLightId() == lightId).findFirst().get());
    }

    @Override
    protected void onMessageReceived(AttributeRef protocolRef, String message) {
        List<Pair<AttributeRef, Consumer<String>>> consumers;

        synchronized (protocolMessageConsumers) {
            consumers = protocolMessageConsumers.get(protocolRef);

            if (consumers != null) {
                consumers.forEach(c -> {
                    if (c.value != null) {
                        c.value.accept(message);
                    }
                });
            }
        }
    }

    @Override
    protected String createWriteMessage(AssetAttribute protocolConfiguration, AssetAttribute attribute, AttributeEvent event, Value processedValue) {
        //TODO LATER, CHECK FOR GROUP
        Asset parentAsset = assetService.findAsset(getLinkedAttribute(attribute.getReference().get()).getAssetId().get());
        //THIS NOW RELIES ON ALWAYS HAVING THE FOLLOWING ATTRIBUTES IN THE SAME LEVEL
        AssetAttribute lightIdAttribute = parentAsset.getAttribute("Id").get();
        AssetAttribute universeAttribute = parentAsset.getAttribute("Universe").get();
        Integer universeId = universeAttribute.getValueAsInteger().get();
        Integer lightId = lightIdAttribute.getValueAsInteger().get();
        ArtnetLight updatedLight = (ArtnetLight) artnetLightMemory.get(universeId).get(lightId);
        ArtnetLightState oldLightState = (ArtnetLightState) updatedLight.getLightState();


        //UPDATE VALUES (RGBW FOR EXAMPLE)
        if(event.getAttributeRef().getAttributeName().equalsIgnoreCase("Values")) {
            Map<String, Integer> valuesToUpdate = new HashMap<String, Integer>();
            for(String requiredKey : updatedLight.getRequiredValues()) {
                valuesToUpdate.put(requiredKey, new JsonParser().parse(processedValue.toJson()).getAsJsonObject().get(requiredKey).getAsInt());
                updateLightStateInMemory(lightId, new ArtnetLightState(lightId, valuesToUpdate, oldLightState.getDim(), oldLightState.isEnabled()));
            }
        }
        //UPDATE DIM
        else if(event.getAttributeRef().getAttributeName().equalsIgnoreCase("Dim")) {
            int dimValue = new JsonParser().parse(processedValue.toJson()).getAsInt();
            updateLightStateInMemory(lightId, new ArtnetLightState(lightId, oldLightState.getReceivedValues(), dimValue, oldLightState.isEnabled()));
        }
        //UPDATE ENABLED/DISABLED
        else if(event.getAttributeRef().getAttributeName().equalsIgnoreCase("Switch")) {
            boolean enabled = new JsonParser().parse(processedValue.toJson()).getAsBoolean();
            updateLightStateInMemory(lightId, new ArtnetLightState(lightId, oldLightState.getReceivedValues(), oldLightState.getDim(), enabled));
        }

        //updateLightStateInMemory(lightId, updatedLightState);
        Value value = Values.createObject().putAll(new HashMap<String, Value>() {{
            put("lights", Values.convert(artnetLightMemory.get(universeId), Container.JSON).get());
        }});
        return value.toJson();
    }

    @Override
    public void updateLightStateInMemory(Integer lightId, AbstractArtnetLightState updatedLightState)
    {
        for(int universe : artnetLightMemory.keySet())
            if(artnetLightMemory.get(universe).stream().anyMatch(light -> light.getLightId() == lightId))
                artnetLightMemory.get(universe).stream().filter(light -> light.getLightId() == lightId).findFirst().get().setLightState(updatedLightState);
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
        //WARNING: This code overwrites the JSON config file ||| TODO: Compare the imported file with the current config file, If certain lights already exist: THEY SHOULDN'T BE ALTERED
        //protocolConfiguration.getMetaItem("urn:openremote:protocol:artnet:areaConfiguration").orElse(null).setValue(Values.convert(jsonString, Container.JSON).get());

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
        lightAttributes.add(new AssetAttribute("GroupId", AttributeValueType.NUMBER, Values.create(groupId)));
        lightAttributes.add(new AssetAttribute("Universe", AttributeValueType.NUMBER, Values.create(0)));//WARNING: HARDCODED
        lightAttributes.add(new AssetAttribute("AmountOfLeds", AttributeValueType.NUMBER, Values.create(3)));//WARNING: HARDCODED
        lightAttributes.add(new AssetAttribute("RequiredValues", AttributeValueType.STRING, Values.create(requiredValues)));
        lightAttributes.add(light.getAttribute("Dim").orElse(new AssetAttribute("Dim", AttributeValueType.NUMBER, Values.create(100)).setMeta(
                agentLink
        )));
        lightAttributes.add(light.getAttribute("Switch").orElse(new AssetAttribute("Switch", AttributeValueType.BOOLEAN, Values.create(true)).setMeta(
                agentLink
        )));
        lightAttributes.add(light.getAttribute("Values").orElse(new AssetAttribute("Values", AttributeValueType.OBJECT, Values.createObject().putAll(jsonProperties)).setMeta(
                agentLink
        )));

        AssetTreeNode output = new AssetTreeNode(light);
        output.asset.setAttributes(lightAttributes);

        return output;
    }
}
