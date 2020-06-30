package org.openremote.agent.protocol.artnet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.openremote.agent.protocol.ProtocolLinkedAttributeImport;
import org.openremote.agent.protocol.io.AbstractIoClientProtocol;
import org.openremote.agent.protocol.io.AbstractNettyIoClient;
import org.openremote.agent.protocol.udp.UdpIoClient;
import org.openremote.container.Container;
import org.openremote.container.util.CodecUtil;
import org.openremote.container.util.UniqueIdentifierGenerator;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.*;
import org.openremote.model.file.FileInfo;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.Pair;
import org.openremote.model.value.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.openremote.container.util.Util.joinCollections;
import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.asset.AssetType.THING;
import static org.openremote.model.attribute.AttributeValueType.*;
import static org.openremote.model.attribute.MetaItemDescriptor.Access.ACCESS_PRIVATE;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemInteger;
import static org.openremote.model.attribute.MetaItemDescriptorImpl.metaItemObject;
import static org.openremote.model.attribute.MetaItemType.AGENT_LINK;
import static org.openremote.model.attribute.MetaItemType.READ_ONLY;

public class ArtnetClientProtocol extends AbstractIoClientProtocol<ArtnetPacket, UdpIoClient<ArtnetPacket>> implements ProtocolLinkedAttributeImport {

    private static final String PROTOCOL_VERSION = "1.70";
    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":artnet";
    public static final String PROTOCOL_DISPLAY_NAME = "Artnet Client";
    public static final List<MetaItemDescriptor> PROTOCOL_META_ITEM_DESCRIPTORS = joinCollections(ArtnetClientProtocol.PROTOCOL_META_ITEM_DESCRIPTORS, AbstractIoClientProtocol.PROTOCOL_GENERIC_META_ITEM_DESCRIPTORS);
    public static final String agentProtocolConfigName = "ArtnetProtocolAgent";
    public static final String ARTNET_DEFAULT_LIGHT_STATE = "{'r': 0, 'g': 0, 'b': 0, 'w': 0}";
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
    /**
     * Optionally sets the port that this Artnet client will bind to (if not set then a random ephemeral port will be used)
     */
    public static final MetaItemDescriptor META_PROTOCOL_BIND_PORT = metaItemInteger(
            PROTOCOL_NAME + ":bindPort",
            ACCESS_PRIVATE,
            true,
            1,
            65536);

    public static final List<MetaItemDescriptor> ATTRIBUTE_META_ITEM_DESCRIPTORS = Arrays.asList(
            META_ATTRIBUTE_MATCH_FILTERS,
            META_ATTRIBUTE_MATCH_PREDICATE,
            META_ARTNET_LIGHT_ID,
            META_ARTNET_CONFIGURATION);

    protected final Map<AttributeRef, List<Pair<AttributeRef, Consumer<ArtnetPacket>>>> protocolMessageConsumers = new HashMap<>();

    private List<ArtnetLight> artnetLightMemory = new ArrayList<>();

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

    public List<ArtnetLight> getLightMemory() { return artnetLightMemory; }

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
    protected void doUnlinkProtocolConfiguration(Asset agent, AssetAttribute protocolConfiguration) {
        synchronized (protocolMessageConsumers) {
            protocolMessageConsumers.remove(protocolConfiguration.getReferenceOrThrow());
        }
        super.doUnlinkProtocolConfiguration(agent, protocolConfiguration);
    }

    @Override
    protected UdpIoClient<ArtnetPacket> createIoClient(AssetAttribute protocolConfiguration) throws Exception {
        String host = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_HOST,
                false,
                false
        ).flatMap(Values::getString).orElse(null);

        Integer port = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_PORT,
                false,
                false
        ).flatMap(Values::getIntegerCoerced).orElse(null);

        Integer bindPort = Values.getMetaItemValueOrThrow(
                protocolConfiguration,
                META_PROTOCOL_BIND_PORT,
                false,
                false
        ).flatMap(Values::getIntegerCoerced).orElse(null);

        if (port != null && (port < 1 || port > 65536)) {
            throw new IllegalArgumentException("Port must be in the range 1-65536");
        }

        if (bindPort != null && (bindPort < 1 || bindPort > 65536)) {
            throw new IllegalArgumentException("Bind port must be in the range 1-65536 if specified");
        }

        return new UdpIoClient<>(host, port, bindPort, executorService);
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

        if(getLinkedAttribute(attribute.getReference().orElse(null)) == null)
            return;

        AttributeRef parentAttributeRef = attribute.getReference().orElse(null);
        if(parentAttributeRef == null)
            return;

        String parentAssetId = getLinkedAttribute(parentAttributeRef).getAssetId().orElse(null);
        if(parentAssetId == null)
            return;

        Asset parentAsset = assetService.findAsset(parentAssetId);
        if(parentAsset == null)
            return;

        Attribute lightAttribute = parentAsset.getAttribute("Id").orElse(null);
        Attribute groupAttribute = parentAsset.getAttribute("GroupId").orElse(null);
        Attribute universeAttribute = parentAsset.getAttribute("Universe").orElse(null);
        Attribute amountOfLedsAttribute = parentAsset.getAttribute("AmountOfLeds").orElse(null);
        Attribute requiredValuesAttribute = parentAsset.getAttribute("RequiredValues").orElse(null);
        if(lightAttribute != null && groupAttribute != null && universeAttribute != null && amountOfLedsAttribute != null && requiredValuesAttribute != null)
        {
            int lightId = lightAttribute.getValueAsInteger().orElse(-1);
            int groupId = groupAttribute.getValueAsInteger().orElse(-1);
            int universe = universeAttribute.getValueAsInteger().orElse(-1);
            int amountOfLeds = amountOfLedsAttribute.getValueAsInteger().orElse(-1);
            String requiredKeysField = requiredValuesAttribute.getValueAsString().orElse(null);

            if(lightId != -1 && groupId != -1 && universe != -1 && amountOfLeds != -1 && requiredKeysField != null)
            {
                String[] requiredKeys = requiredKeysField.split(",");
                ArtnetLightState state = new ArtnetLightState(lightId, new LinkedHashMap<String, Integer>(), 100, true);
                for(String key : requiredKeys)
                    state.getReceivedValues().put(key, 0);
                ArtnetLight lightToCreate = new ArtnetLight(lightId, groupId, universe, amountOfLeds, requiredKeys, state, null);
                if(artnetLightMemory.stream().noneMatch(light -> light.getLightId() == lightToCreate.getLightId()))
                    artnetLightMemory.add(lightToCreate);
            }
        }
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AssetAttribute assetAttribute = getLinkedAttribute(attribute.getReference().orElse(null));
        if(assetAttribute != null) {
            String assetId = assetAttribute.getAssetId().orElse(null);
            if(assetId != null) {
                Asset parentAsset = assetService.findAsset(assetId);
                Attribute lightAttribute = parentAsset.getAttribute("Id").orElse(null);
                Attribute universeAttribute = parentAsset.getAttribute("Universe").orElse(null);
                if(lightAttribute != null && universeAttribute != null) {
                    int lightId = lightAttribute.getValueAsInteger().orElse(-1);
                    int universe = universeAttribute.getValueAsInteger().orElse(-1);
                    if(lightId != -1 && universe != -1) {
                        if(artnetLightMemory.stream().anyMatch(light -> light.getLightId() == lightId)) {
                            artnetLightMemory.stream().filter(light -> light.getLightId() == lightId).findFirst().ifPresent(artnetLight -> artnetLightMemory.remove(artnetLight));
                        }
                    }
                }
            }
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
        AttributeRef attributeRef = attribute.getReference().orElse(null);
        if(attributeRef != null) {
            AssetAttribute linkedAttribute = getLinkedAttribute(attributeRef);
            if(linkedAttribute != null) {
                String parentAssetId = linkedAttribute.getAssetId().orElse(null);
                if(parentAssetId != null) {
                    Asset parentAsset = assetService.findAsset(parentAssetId);
                    Attribute universeAttribute = parentAsset.getAttribute("Universe").orElse(null);
                    Attribute lightAttribute = parentAsset.getAttribute("Id").orElse(null);
                    if(universeAttribute != null && lightAttribute != null) {
                        int universeId = universeAttribute.getValueAsInteger().orElse(-1);
                        int lightId = lightAttribute.getValueAsInteger().orElse(-1);
                        if(universeId != -1 && lightId != -1) {
                            ArtnetLight updatedLight = (ArtnetLight) artnetLightMemory.stream().filter(light -> light.getLightId() == lightId).findFirst().orElse(null);
                            if(updatedLight != null) {
                                ArtnetLightState oldLightState = (ArtnetLightState) updatedLight.getLightState();
                                //UPDATE LIGHT VALUES (R,G,B FOR EXAMPLE)
                                if(event.getAttributeRef().getAttributeName().equalsIgnoreCase("Values")) {
                                    Map<String, Integer> valuesToUpdate = new LinkedHashMap<String, Integer>();
                                    for(String requiredKey : updatedLight.getRequiredValues()) {
                                        try {
                                            JsonNode node = Container.JSON.readTree(processedValue.toJson());
                                            JsonNode requiredKeyValue = node.get(requiredKey);
                                            if(requiredKeyValue == null)
                                                throw new NullPointerException("Could not find key: " + requiredKey.toString() + " in the json-file.");
                                            valuesToUpdate.put(requiredKey, requiredKeyValue.asInt());
                                        } catch (JsonProcessingException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    updateLightStateInMemory(lightId, new ArtnetLightState(lightId, valuesToUpdate, oldLightState.getDim(), oldLightState.isEnabled()));
                                }
                                //UPDATE DIM
                                else if(event.getAttributeRef().getAttributeName().equalsIgnoreCase("Dim")) {
                                    try {
                                        JsonNode node = Container.JSON.readTree(processedValue.toJson());
                                        int dimValue = node.asInt();
                                        updateLightStateInMemory(lightId, new ArtnetLightState(lightId, oldLightState.getReceivedValues(), dimValue, oldLightState.isEnabled()));
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }
                                //UPDATE ENABLED/DISABLED
                                else if(event.getAttributeRef().getAttributeName().equalsIgnoreCase("Switch")) {
                                    try{
                                        JsonNode node = Container.JSON.readTree(processedValue.toJson());
                                        boolean enabled = node.asBoolean();
                                        updateLightStateInMemory(lightId, new ArtnetLightState(lightId, oldLightState.getReceivedValues(), oldLightState.getDim(), enabled));
                                    } catch (JsonProcessingException e) {
                                        e.printStackTrace();
                                    }
                                }
                                //SEND ALL LIGHTS TO UPDATE TO THE ENCODER
                                List<ArtnetLight> lightsToSend = new ArrayList<ArtnetLight>();
                                for(ArtnetLight abstractLight : artnetLightMemory.stream().filter(light -> light.getUniverse() == universeId).collect(Collectors.toList()))
                                    lightsToSend.add((ArtnetLight)abstractLight);
                                updateLinkedAttribute(event.getAttributeState());
                                return new ArtnetPacket(universeId, lightsToSend);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public void updateLightStateInMemory(Integer lightId, ArtnetLightState updatedLightState)
    {
        if(artnetLightMemory.stream().anyMatch(light -> light.getLightId() == lightId))
            artnetLightMemory.stream().filter(light -> light.getLightId() == lightId).findFirst().ifPresent(artnetLight -> Objects.requireNonNull(artnetLightMemory.stream().filter(light -> light.getLightId() == lightId).findFirst().orElse(null)).setLightState(updatedLightState));
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
            throw new IllegalStateException("The import-file format should be .json.");
        try{
            List<ArtnetLight> newLights = parseArtnetLightsFromImport(new ObjectMapper().readTree(jsonString));
            syncLightsToMemory(newLights);
            return syncLightsToAssets(newLights, protocolConfiguration);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("The provided json is invalid.");
        } catch (Exception e) {
            throw new IllegalStateException("Could not import the provided lights.");
        }
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
            ArtnetLight light = new ArtnetLight(lightId, groupId, universe, amountOfLeds, requiredValues, null, null);
            parsedLights.add(light);
        }
        return parsedLights;
    }

    private void syncLightsToMemory(List<ArtnetLight> lights)
    {
        for(ArtnetLight light : new ArrayList<ArtnetLight>(artnetLightMemory)) {
            ArtnetLightState state = new ArtnetLightState(light.getLightId(), new LinkedHashMap<String, Integer>(), 100, true);
            for(String key : light.getRequiredValues())
                state.getReceivedValues().put(key, 0);
            light.setLightState(state);
            //A light is found with the current id in the import file which is present in-memory
            if(lights.stream().anyMatch(l -> l.getLightId() == light.getLightId())) {
                //Replace the in-memory light with the attributes/values from the import file
                ArtnetLight foundLight = (ArtnetLight) artnetLightMemory.stream().filter(l -> l.getLightId() == light.getLightId()).findFirst().orElse(null);
                if(foundLight != null) {
                    artnetLightMemory.set(artnetLightMemory.indexOf(foundLight), light);
                }
            }
            //No light is found with the current id in the import file which is present in-memory
            else if(lights.stream().noneMatch(l -> l.getLightId() == light.getLightId())) {
                //Remove the light from in-memory
                ArtnetLight foundLight = (ArtnetLight) artnetLightMemory.stream().filter(l -> l.getLightId() == light.getLightId()).findFirst().orElse(null);
                if(foundLight != null) {
                    artnetLightMemory.remove(foundLight);
                }
            }
        }
        for(ArtnetLight light : lights) {
            ArtnetLightState state = new ArtnetLightState(light.getLightId(), new LinkedHashMap<String, Integer>(), 100, true);
            for(String key : light.getRequiredValues())
                state.getReceivedValues().put(key, 0);
            light.setLightState(state);
            //The import file contains a light id which is not present in-memory yet
            if(artnetLightMemory.stream().noneMatch(l -> l.getLightId() == light.getLightId())) {
                artnetLightMemory.add(light);
            }
        }
    }

    private AssetTreeNode[] syncLightsToAssets(List<ArtnetLight> lights, AssetAttribute protocolConfiguration) throws Exception
    {
        List<AssetTreeNode> output = new ArrayList<AssetTreeNode>();

        //Fetch all the assets that're connected to the ArtNet agent.
        List<Asset> assetsUnderProtocol = assetService.findAssets(protocolConfiguration.getAssetId().orElse(null), new AssetQuery());
        //Get the instance of the ArtNet agent itself.
        Asset parentAgent = assetsUnderProtocol.stream().filter(a -> a.getWellKnownType() == AssetType.AGENT).findFirst().orElse(null);
        if(parentAgent != null) {
            for(Asset asset : assetsUnderProtocol)
            {
                //TODO CHANGE ASSET TYPE THING TO LIGHT
                if(asset.getWellKnownType() != AssetType.THING)
                    continue;

                if(!asset.hasAttribute("Id"))//Confirm the asset is a light
                    continue;

                //Asset is valid
                AssetAttribute lightAttribute = asset.getAttribute("Id").orElse(null);
                if(lightAttribute != null) {
                    int lightId = lightAttribute.getValueAsInteger().orElse(-1);
                    if(lightId != -1) {
                        if(lights.stream().anyMatch(l -> l.getLightId() == lightId)) {
                            ArtnetLight updatedLight = lights.stream().filter(l -> l.getLightId() == lightId).findFirst().orElse(null);
                            if(updatedLight != null) {
                                List<AssetAttribute> artNetLightAttributes = Arrays.asList(
                                        new AssetAttribute("Id", NUMBER, Values.create(updatedLight.getLightId())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                                        new AssetAttribute("GroupId", NUMBER, Values.create(updatedLight.getGroupId())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                                        new AssetAttribute("Universe", NUMBER, Values.create(updatedLight.getUniverse())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                                        new AssetAttribute("AmountOfLeds", NUMBER, Values.create(updatedLight.getAmountOfLeds())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                                        new AssetAttribute("RequiredValues", STRING, Values.create(String.join(",", updatedLight.getRequiredValues()))).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                                        new AssetAttribute("Values", OBJECT, Values.parseOrNull(ARTNET_DEFAULT_LIGHT_STATE)).addMeta(
                                                new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                                        ),
                                        new AssetAttribute("Switch", BOOLEAN, Values.create(true)).addMeta(
                                                new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                                        ),
                                        new AssetAttribute("Dim", NUMBER, Values.create(100)).addMeta(
                                                new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                                        )
                                );
                                asset.setAttributes(artNetLightAttributes);
                                assetService.mergeAsset(asset);
                            }
                        }else{
                            if(lights.stream().noneMatch(l -> l.getLightId() == lightId))
                                assetService.deleteAsset(asset.getId());
                        }
                    }
                }
            }

            //New data is fetched based on the changes.
            assetsUnderProtocol = assetService.findAssets(protocolConfiguration.getAssetId().orElse(null), new AssetQuery());
            for(ArtnetLight light : lights)
            {
                boolean lightAssetExistsAlready = false;
                for(Asset asset : assetsUnderProtocol)
                {
                    //TODO CHANGE ASSET TYPE THING TO LIGHT
                    if((asset.getWellKnownType() != AssetType.THING))
                        continue;

                    if(!asset.hasAttribute("Id"))
                        continue;

                    AssetAttribute lightIdAttribute = asset.getAttribute("Id").orElse(null);
                    if(lightIdAttribute != null) {
                        int lightId = lightIdAttribute.getValueAsInteger().orElse(-1);
                        if(lightId != -1) {
                            if(lightId == light.getLightId())
                                lightAssetExistsAlready = true;
                        }
                    }

                }
                if(!lightAssetExistsAlready)
                    output.add(formLightAsset(light, parentAgent));
            }
            return output.toArray(new AssetTreeNode[output.size()]);
        }
        return null;
    }

    protected AssetTreeNode formLightAsset(ArtnetLight light, Asset parentAgent) {
        Asset asset = new Asset();
        asset.setId(UniqueIdentifierGenerator.generateId());
        asset.setParent(parentAgent);
        asset.setName("ArtNet Light " + light.getLightId());
        asset.setType(THING);
        List<AssetAttribute> artNetLightAttributes = Arrays.asList(
                new AssetAttribute("Id", NUMBER, Values.create(light.getLightId())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                new AssetAttribute("GroupId", NUMBER, Values.create(light.getGroupId())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                new AssetAttribute("Universe", NUMBER, Values.create(light.getUniverse())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                new AssetAttribute("AmountOfLeds", NUMBER, Values.create(light.getAmountOfLeds())).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                new AssetAttribute("RequiredValues", STRING, Values.create(String.join(",", light.getRequiredValues()))).setMeta(new Meta(new MetaItem(READ_ONLY, Values.create(true)))),
                new AssetAttribute("Values", OBJECT, Values.parseOrNull(ARTNET_DEFAULT_LIGHT_STATE)).addMeta(
                        new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                ),
                new AssetAttribute("Switch", BOOLEAN, Values.create(true)).addMeta(
                        new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                ),
                new AssetAttribute("Dim", NUMBER, Values.create(100)).addMeta(
                        new MetaItem(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
                )
        );
        asset.setAttributes(artNetLightAttributes);
        return new AssetTreeNode(asset);
    }
}
