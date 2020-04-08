package org.openremote.agent.protocol.dmx.artnet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.openremote.agent.protocol.ProtocolLinkedAttributeImport;
import org.openremote.agent.protocol.dmx.AbstractArtnetClientProtocol;
import org.openremote.agent.protocol.dmx.AbstractArtnetLight;
import org.openremote.agent.protocol.dmx.AbstractArtnetLightState;
import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.agent.protocol.udp.UdpIoClient;
import org.openremote.container.util.CodecUtil;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.AssetType;
import org.openremote.model.asset.agent.AgentLink;
import org.openremote.model.attribute.*;
import org.openremote.model.file.FileInfo;
import org.openremote.model.util.Pair;
import org.openremote.model.value.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.openremote.container.util.Util.joinCollections;
import static org.openremote.model.Constants.MASTER_REALM;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemInteger;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemObject;
import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

public class ArtnetClientProtocol extends AbstractArtnetClientProtocol<ArtnetPacket> implements ProtocolLinkedAttributeImport {

    private static final String PROTOCOL_VERSION = "1.70";

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":artnet";
    public static final String PROTOCOL_DISPLAY_NAME = "Artnet Client";
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

    protected final Map<AttributeRef, List<Pair<AttributeRef, Consumer<ArtnetPacket>>>> protocolMessageConsumers = new HashMap<>();

    private List<AbstractArtnetLight> artnetLightMemory = new ArrayList<>();
    //private HashMap<Integer, List<AbstractArtnetLight>> artnetLightMemory = new HashMap<Integer, List<AbstractArtnetLight>>();

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
    public List<AbstractArtnetLight> getLightMemory() { return artnetLightMemory; }

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
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider(UdpIoClient<ArtnetPacket> client, AssetAttribute protocolConfiguration) {
        Supplier<ChannelHandler[]> encoderDecoderProvider = () -> {
            List<ChannelHandler> encodersDecoders = new ArrayList<>();
            encodersDecoders.add(new AbstractNettyIoClient.MessageToByteEncoder<ArtnetPacket>(ArtnetPacket.class, client, new BiConsumer<ArtnetPacket, ByteBuf>() {
                @Override
                public void accept(ArtnetPacket packet, ByteBuf buf) {
                    packet.toByteBuf(buf);
                }
            }));
            return encodersDecoders.toArray(new ChannelHandler[0]);
        };
        return encoderDecoderProvider;
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();
        //Todo check if this has any significance for us.
        Consumer<ArtnetPacket> messageConsumer = artnetPacket -> {};
        if (messageConsumer != null) {
            synchronized (protocolMessageConsumers) {
                protocolMessageConsumers.compute(protocolRef, (ref, consumers) -> {
                    if (consumers == null) {
                        consumers = new ArrayList<>();
                    }
                    consumers.add(new Pair<AttributeRef, Consumer<ArtnetPacket>>(
                            attribute.getReferenceOrThrow(),
                            messageConsumer
                    ));
                    return consumers;
                });
            }
        }
        Asset parentAsset = assetService.findAsset(getLinkedAttribute(attribute.getReference().get()).getAssetId().get());
        //THIS NOW RELIES ON ALWAYS HAVING THE FOLLOWING ATTRIBUTES IN THE SAME LEVEL
        Integer lightId = parentAsset.getAttribute("Id").get().getValueAsInteger().get();
        Integer groupId = parentAsset.getAttribute("GroupId").get().getValueAsInteger().get();
        Integer universe = parentAsset.getAttribute("Universe").get().getValueAsInteger().get();
        Integer amountOfLeds = parentAsset.getAttribute("AmountOfLeds").get().getValueAsInteger().get();
        String[] requiredKeys = parentAsset.getAttribute("RequiredValues").get().getValueAsString().get().split(",");
        ArtnetLightState state = new ArtnetLightState(lightId, new LinkedHashMap<String, Integer>(), 100, true);
        for(String key : requiredKeys)
            state.getReceivedValues().put(key, 0);
        ArtnetLight lightToCreate = new ArtnetLight(lightId, groupId, universe, amountOfLeds, requiredKeys, state, null);
        if(artnetLightMemory.stream().noneMatch(light -> light.getLightId() == lightToCreate.getLightId()))
            artnetLightMemory.add(lightToCreate);
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef attributeRef = attribute.getReferenceOrThrow();
        Asset parentAsset = assetService.findAsset(getLinkedAttribute(attribute.getReference().get()).getAssetId().get());
        //THIS NOW RELIES ON ALWAYS HAVING THE FOLLOWING ATTRIBUTES IN THE SAME LEVEL
        Integer lightId = parentAsset.getAttribute("Id").get().getValueAsInteger().get();
        Integer universe = parentAsset.getAttribute("Universe").get().getValueAsInteger().get();
        if(artnetLightMemory.stream().anyMatch(light -> light.getLightId() == lightId))
            artnetLightMemory.remove(artnetLightMemory.stream().filter(light -> light.getLightId() == lightId).findFirst().get());
        synchronized (protocolMessageConsumers) {
            protocolMessageConsumers.compute(protocolConfiguration.getReferenceOrThrow(), (ref, consumers) -> {
                if (consumers != null) {
                    consumers.removeIf((attrRefConsumer) -> attrRefConsumer.key.equals(attributeRef));
                }
                return consumers;
            });
        }

    }

    @Override
    protected void onMessageReceived(AttributeRef protocolRef, ArtnetPacket packet) {
        List<Pair<AttributeRef, Consumer<ArtnetPacket>>> consumers;
        synchronized (protocolMessageConsumers) {
            consumers = protocolMessageConsumers.get(protocolRef);
            if (consumers != null) {
                consumers.forEach(c -> {
                    if (c.value != null) {
                        c.value.accept(packet);
                    }
                });
            }
        }
    }

    @Override
    protected ArtnetPacket createWriteMessage(AssetAttribute protocolConfiguration, AssetAttribute attribute, AttributeEvent event, Value processedValue) {
        //Todo check for group later here
        Asset parentAsset = assetService.findAsset(getLinkedAttribute(attribute.getReference().get()).getAssetId().get());
        //THIS NOW RELIES ON ALWAYS HAVING THE FOLLOWING ATTRIBUTES IN THE SAME LEVEL
        Integer universeId = parentAsset.getAttribute("Universe").get().getValueAsInteger().get();
        Integer lightId = parentAsset.getAttribute("Id").get().getValueAsInteger().get();
        ArtnetLight updatedLight = (ArtnetLight) artnetLightMemory.stream().filter(light -> light.getLightId() == lightId).findFirst().get();
        ArtnetLightState oldLightState = (ArtnetLightState) updatedLight.getLightState();
        ObjectMapper mapper = new ObjectMapper();
        //UPDATE LIGHT VALUES (R,G,B FOR EXAMPLE)
        if(event.getAttributeRef().getAttributeName().equalsIgnoreCase("Values")) {
            Map<String, Integer> valuesToUpdate = new LinkedHashMap<String, Integer>();
            for(String requiredKey : updatedLight.getRequiredValues()) {
                try {
                    JsonNode node = mapper.readTree(processedValue.toJson());
                    int requiredKeyValue = node.get(requiredKey).asInt();
                    valuesToUpdate.put(requiredKey, requiredKeyValue);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            updateLightStateInMemory(lightId, new ArtnetLightState(lightId, valuesToUpdate, oldLightState.getDim(), oldLightState.isEnabled()));
        }
        //UPDATE DIM
        else if(event.getAttributeRef().getAttributeName().equalsIgnoreCase("Dim")) {
            try {
                JsonNode node = mapper.readTree(processedValue.toJson());
                int dimValue = node.asInt();
                updateLightStateInMemory(lightId, new ArtnetLightState(lightId, oldLightState.getReceivedValues(), dimValue, oldLightState.isEnabled()));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        //UPDATE ENABLED/DISABLED
        else if(event.getAttributeRef().getAttributeName().equalsIgnoreCase("Switch")) {
            try{
                JsonNode node = mapper.readTree(processedValue.toJson());
                boolean enabled = node.asBoolean();
                updateLightStateInMemory(lightId, new ArtnetLightState(lightId, oldLightState.getReceivedValues(), oldLightState.getDim(), enabled));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
        //SEND ALL LIGHTS TO UPDATE TO THE ENCODER
        List<ArtnetLight> lightsToSend = new ArrayList<ArtnetLight>();
        for(AbstractArtnetLight abstractLight : artnetLightMemory.stream().filter(light -> light.getUniverse() == universeId).collect(Collectors.toList()))
            lightsToSend.add((ArtnetLight)abstractLight);
        updateLinkedAttribute(event.getAttributeState());
        return new ArtnetPacket(universeId, lightsToSend);
    }

    @Override
    public void updateLightStateInMemory(Integer lightId, AbstractArtnetLightState updatedLightState)
    {
        if(artnetLightMemory.stream().anyMatch(light -> light.getLightId() == lightId))
            artnetLightMemory.stream().filter(light -> light.getLightId() == lightId).findFirst().get().setLightState(updatedLightState);
    }

    @Override
    public AssetTreeNode[] discoverLinkedAssetAttributes(AssetAttribute protocolConfiguration, FileInfo fileInfo) throws IllegalStateException {
        //todo sync inmemory
        //todo sync assettree

        String jsonString;
        if(fileInfo.isBinary())//Read any file that isn't an XML file
        {
            //Read from .json file || Works on files without extention || Works on CSV
            byte[] rawBinaryData = CodecUtil.decodeBase64(fileInfo.getContents());
            jsonString = new String(rawBinaryData);
        }
        else
            jsonString = fileInfo.getContents();//Read from .xml file

        try{
            List<ArtnetLight> newLights = parseArtnetLightsFromImport(new ObjectMapper().readTree(jsonString));
            Asset parentAsset = assetService.getAgent(protocolConfiguration);
            for(AbstractArtnetLight abstractLight : artnetLightMemory) {
                ArtnetLight light = (ArtnetLight) abstractLight;
                ArtnetLightState state = new ArtnetLightState(light.getLightId(), new LinkedHashMap<String, Integer>(), 100, true);
                for(String key : light.getRequiredValues())
                    state.getReceivedValues().put(key, 0);
                light.setLightState(state);
                //A light is found with the current id in the import file which is present in-memory
                if(newLights.stream().anyMatch(l -> l.getLightId() == light.getLightId())) {
                    //Replace the in-memory light with the attributes/values from the import file
                    ArtnetLight foundLight = (ArtnetLight) artnetLightMemory.stream().filter(l -> l.getLightId() == light.getLightId()).findFirst().get();
                    artnetLightMemory.set(artnetLightMemory.indexOf(foundLight), light);
                }
                //No light is found with the current id in the import file which is present in-memory
                else if(newLights.stream().noneMatch(l -> l.getLightId() == light.getLightId())) {
                    //Remove the light from in-memory
                    ArtnetLight foundLight = (ArtnetLight) artnetLightMemory.stream().filter(l -> l.getLightId() == light.getLightId()).findFirst().get();
                    artnetLightMemory.remove(foundLight);
                }
            }
            for(ArtnetLight light : newLights) {
                //The import file contains a light id which is not present in-memory yet
                if(artnetLightMemory.stream().noneMatch(l -> l.getLightId() == light.getLightId())) {
                    artnetLightMemory.add(light);
                }
            }
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode node = mapper.readTree(jsonString);
                byte[] prefix = node.get("protocolPrefix").asText().getBytes();
                JsonNode lightArray = node.get("lights");
                if(lightArray.isArray()) {
                    List<AssetTreeNode> output = new ArrayList<AssetTreeNode>();
                    for(JsonNode individualLightNode : lightArray) {
                        MetaItem agentLink = AgentLink.asAgentLinkMetaItem(protocolConfiguration.getReferenceOrThrow());
                        //WARNING: This code overwrites the JSON config file ||| TODO: Compare the imported file with the current config file, If certain lights already exist: THEY SHOULDN'T BE ALTERED
                        //protocolConfiguration.getMetaItem("urn:openremote:protocol:artnet:areaConfiguration").orElse(null).setValue(Values.convert(jsonString, Container.JSON).get());
                        int id = individualLightNode.get("lightId").asInt();
                        int groupId = individualLightNode.get("groupId").asInt();
                        String requiredValues = individualLightNode.get("requiredValues").asText();
                        output.add(createLightAsset(id, groupId, requiredValues, protocolConfiguration, agentLink));
                    }
                    return output.toArray(new AssetTreeNode[output.size()]);
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            return null;
    }

    private List<ArtnetLight> parseArtnetLightsFromImport(JsonNode jsonNode) {
        JsonNode lightsNode = jsonNode.get("lights");
        List<ArtnetLight> parsedLights = new ArrayList<>();
        for(JsonNode lightNode : lightsNode) {
            int lightId = lightNode.get("lightId").asInt();
            int groupId = lightNode.get("groupId").asInt();
            int universe = lightNode.get("universe").asInt();
            int amountOfLeds = lightNode.get("amountOfLeds").asInt();
            String[] requiredValues = lightNode.get("requiredValues").asText().split(",");
            ArtnetLight light = new ArtnetLight(lightId, groupId, universe, amountOfLeds, requiredValues);
            parsedLights.add(light);
        }
        return parsedLights;
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
