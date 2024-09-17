package org.openremote.agent.protocol.artnet;

import io.netty.channel.ChannelHandler;
import org.openremote.agent.protocol.io.AbstractNettyIOClient;
import org.openremote.agent.protocol.io.AbstractNettyIOClientProtocol;
import org.openremote.agent.protocol.udp.UDPIOClient;
import org.openremote.model.asset.AssetTreeNode;
import org.openremote.model.asset.agent.DefaultAgentLink;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.protocol.ProtocolAssetImport;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ArtnetProtocol extends AbstractNettyIOClientProtocol<ArtnetProtocol, ArtnetAgent, ArtnetPacket, UDPIOClient<ArtnetPacket>, DefaultAgentLink> implements ProtocolAssetImport {

    public static final String PROTOCOL_DISPLAY_NAME = "Artnet";
    protected final Map<AttributeRef, Consumer<ArtnetPacket>> protocolMessageConsumers = new HashMap<>();
    protected final Map<String, ArtnetLightAsset> lights = new HashMap<>();

    public ArtnetProtocol(ArtnetAgent agent) {
        super(agent);
    }

    @Override
    public String getProtocolName() {
        return PROTOCOL_DISPLAY_NAME;
    }

    @Override
    protected UDPIOClient<ArtnetPacket> doCreateIoClient() throws Exception {
        String host = agent.getHost().orElseThrow(() ->
            new IllegalArgumentException("Required host attribute is null or missing"));

        int port = agent.getPort().orElseThrow(() ->
            new IllegalArgumentException("Port must be in the range 1-65536"));

        Integer bindPort = agent.getBindPort().orElse(null);

        return new UDPIOClient<>(host, port, bindPort);
    }

    @Override
    protected Supplier<ChannelHandler[]> getEncoderDecoderProvider() {
        return () ->
            new ChannelHandler[] {
                new AbstractNettyIOClient.MessageToByteEncoder<>(ArtnetPacket.class, client, ArtnetPacket::toByteBuf)
            };
    }

    @Override
    protected void doLinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {
//        AttributeRef attributeRef = new AttributeRef(assetId, attribute.getName());
//        ArtnetLight light = lights.get(assetId);
//
//        if (light == null) {
//            // Light isn't loaded
//            light = assetService.findAsset(assetId, ArtnetLight.class);
//        }
//
//        if (light == null) {
//            // Maybe it has just been deleted so ignore
//            return;
//        }
//
//        lights.put(assetId, light);
//
//
//
//        Attribute<?> lightAttribute = asset.getAttribute("Id").orElse(null);
//        Attribute<?> groupAttribute = asset.getAttribute("GroupId").orElse(null);
//        Attribute<?> universeAttribute = asset.getAttribute("Universe").orElse(null);
//        Attribute<?> amountOfLedsAttribute = asset.getAttribute("AmountOfLeds").orElse(null);
//        Attribute<?> requiredValuesAttribute = asset.getAttribute("RequiredValues").orElse(null);
//        if(lightAttribute != null && groupAttribute != null && universeAttribute != null && amountOfLedsAttribute != null && requiredValuesAttribute != null)
//        {
//            int lightId = lightAttribute.getValueAsInteger().orElse(-1);
//            int groupId = groupAttribute.getValueAsInteger().orElse(-1);
//            int universe = universeAttribute.getValueAsInteger().orElse(-1);
//            int amountOfLeds = amountOfLedsAttribute.getValueAsInteger().orElse(-1);
//            String requiredKeysField = requiredValuesAttribute.getValueAsString().orElse(null);
//
//            if(lightId != -1 && groupId != -1 && universe != -1 && amountOfLeds != -1 && requiredKeysField != null)
//            {
//                String[] requiredKeys = requiredKeysField.split(",");
//                ArtnetLightState state = new ArtnetLightState(lightId, new HashMap<String, Integer>(), 100, true);
//                for(String key : requiredKeys)
//                    state.getReceivedValues().put(key, 0);
//                ArtnetLight lightToCreate = new ArtnetLight(lightId, groupId, universe, amountOfLeds, requiredKeys, state, null);
//                if(artnetLightMemory.stream().noneMatch(light -> light.getLightId() == lightToCreate.getLightId()))
//                    artnetLightMemory.add(lightToCreate);
//            }
//        }
    }

    @Override
    protected void doUnlinkAttribute(String assetId, Attribute<?> attribute, DefaultAgentLink agentLink) {
//        Attribute<?> assetAttribute = getLinkedAttribute(attribute.getReference().orElse(null));
//        if(assetAttribute != null) {
//            String assetId = assetAttribute.getAssetId().orElse(null);
//            if(assetId != null) {
//                Asset<?> parentAsset = assetService.findAsset(assetId);
//                Attribute<?> lightAttribute = parentAsset.getAttribute("Id").orElse(null);
//                Attribute<?> universeAttribute = parentAsset.getAttribute("Universe").orElse(null);
//                if(lightAttribute != null && universeAttribute != null) {
//                    int lightId = lightAttribute.getValueAsInteger().orElse(-1);
//                    int universe = universeAttribute.getValueAsInteger().orElse(-1);
//                    if(lightId != -1 && universe != -1) {
//                        if(artnetLightMemory.stream().anyMatch(light -> light.getLightId() == lightId)) {
//                            artnetLightMemory.stream().filter(light -> light.getLightId() == lightId).findFirst().ifPresent(artnetLight -> artnetLightMemory.remove(artnetLight));
//                        }
//                    }
//                }
//            }
//        }
    }

    @Override
    protected void onMessageReceived(ArtnetPacket packet) {
        synchronized (protocolMessageConsumers) {
            protocolMessageConsumers.forEach((ref, c) -> c.accept(packet));
        }
    }

    @Override
    protected ArtnetPacket createWriteMessage(DefaultAgentLink agentLink, AttributeEvent event, Object processedValue) {
//        // TODO check for group later here
//        AttributeRef attributeRef = event.getAttributeRef();
//
//        Asset<?> parentAsset = assetService.findAsset(parentAssetId);
//        Attribute<?> universeAttribute = parentAsset.getAttribute("Universe").orElse(null);
//        Attribute<?> lightAttribute = parentAsset.getAttribute("Id").orElse(null);
//        if (universeAttribute != null && lightAttribute != null) {
//            int universeId = universeAttribute.getValueAsInteger().orElse(-1);
//            int lightId = lightAttribute.getValueAsInteger().orElse(-1);
//            if(universeId != -1 && lightId != -1) {
//                ArtnetLight updatedLight = artnetLightMemory.stream().filter(light -> light.getLightId() == lightId).findFirst().orElse(null);
//                if(updatedLight != null) {
//                    ArtnetLightState oldLightState = updatedLight.getLightState();
//                    //UPDATE LIGHT VALUES (R,G,B FOR EXAMPLE)
//                    if(event.getAttributeRef().getName().equalsIgnoreCase("Values")) {
//                        Map<String, Integer> valuesToUpdate = new HashMap<>();
//                        for(String requiredKey : updatedLight.getRequiredValues()) {
//                            try {
//                                JsonNode node = ValueUtil.JSON.readTree(processedValue.toJson());
//                                JsonNode requiredKeyValue = node.get(requiredKey);
//                                if(requiredKeyValue == null)
//                                    throw new NullPointerException("Could not find key: " + requiredKey + " in the json-file.");
//                                valuesToUpdate.put(requiredKey, requiredKeyValue.asInt());
//                            } catch (JsonProcessingException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        updateLightStateInMemory(lightId, new ArtnetLightState(lightId, valuesToUpdate, oldLightState.getDim(), oldLightState.isEnabled()));
//                    }
//                    //UPDATE DIM
//                    else if(event.getAttributeRef().getName().equalsIgnoreCase("Dim")) {
//                        try {
//                            JsonNode node = ValueUtil.JSON.readTree(processedValue.toJson());
//                            int dimValue = node.asInt();
//                            updateLightStateInMemory(lightId, new ArtnetLightState(lightId, oldLightState.getReceivedValues(), dimValue, oldLightState.isEnabled()));
//                        } catch (JsonProcessingException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    //UPDATE ENABLED/DISABLED
//                    else if(event.getAttributeRef().getName().equalsIgnoreCase("Switch")) {
//                        try{
//                            JsonNode node = ValueUtil.JSON.readTree(processedValue.toJson());
//                            boolean enabled = node.asBoolean();
//                            updateLightStateInMemory(lightId, new ArtnetLightState(lightId, oldLightState.getReceivedValues(), oldLightState.getDim(), enabled));
//                        } catch (JsonProcessingException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    //SEND ALL LIGHTS TO UPDATE TO THE ENCODER
//                    List<ArtnetLight> lightsToSend = artnetLightMemory.stream().filter(light -> light.getUniverse() == universeId).collect(Collectors.toList());
//                    updateLinkedAttribute(event.getAttributeState());
//                    return new ArtnetPacket(universeId, lightsToSend);
//                }
//            }
//        }
        return null;
    }

    @Override
    public Future<Void> startAssetImport(byte[] fileData, Consumer<AssetTreeNode[]> assetConsumer) {

//        assetImportTask = executorService.submit(() -> {
//
//            String jsonString;
//            jsonString = new String(fileData);
//            ObjectMap lightsObjectNode = ValueUtil.convert(ObjectNode.class, jsonString);
//            List<ArtnetLight> newLights = parseArtnetLightsFromImport(lightsObjectNode);
//            syncLightsToMemory(newLights);
//            assetConsumer.accept(syncLightsToAssets(newLights, protocolConfiguration));
//        }, null);
        return CompletableFuture.completedFuture(null);
    }

//    private List<ArtnetLight> parseArtnetLightsFromImport(JsonNode jsonNode) {
//        JsonNode lightsNode = jsonNode.get("lights");
//        List<ArtnetLight> parsedLights = new ArrayList<>();
//        for(JsonNode lightNode : lightsNode) {
//            int lightId = lightNode.get("lightId").asInt();
//            int groupId = lightNode.get("groupId").asInt();
//            int universe = lightNode.get("universe").asInt();
//            int amountOfLeds = lightNode.get("amountOfLeds").asInt();
//            String[] requiredValues = lightNode.get("requiredValues").asText().split(",");
//            ArtnetLight light = new ArtnetLight(lightId, groupId, universe, amountOfLeds, requiredValues, null, null);
//            parsedLights.add(light);
//        }
//        return parsedLights;
//    }
//
//    private void syncLightsToMemory(List<ArtnetLight> lights) {
//        for(ArtnetLight light : new ArrayList<ArtnetLight>(artnetLightMemory)) {
//            ArtnetLightState state = new ArtnetLightState(light.getLightId(), new HashMap<String, Integer>(), 100, true);
//            for(String key : light.getRequiredValues())
//                state.getReceivedValues().put(key, 0);
//            light.setLightState(state);
//            //A light is found with the current id in the import file which is present in-memory
//            if(lights.stream().anyMatch(l -> l.getLightId() == light.getLightId())) {
//                //Replace the in-memory light with the attributes/values from the import file
//                ArtnetLight foundLight = (ArtnetLight) artnetLightMemory.stream().filter(l -> l.getLightId() == light.getLightId()).findFirst().orElse(null);
//                if(foundLight != null) {
//                    artnetLightMemory.set(artnetLightMemory.indexOf(foundLight), light);
//                }
//            }
//            //No light is found with the current id in the import file which is present in-memory
//            else if(lights.stream().noneMatch(l -> l.getLightId() == light.getLightId())) {
//                //Remove the light from in-memory
//                ArtnetLight foundLight = (ArtnetLight) artnetLightMemory.stream().filter(l -> l.getLightId() == light.getLightId()).findFirst().orElse(null);
//                if(foundLight != null) {
//                    artnetLightMemory.remove(foundLight);
//                }
//            }
//        }
//        for(ArtnetLight light : lights) {
//            ArtnetLightState state = new ArtnetLightState(light.getLightId(), new HashMap<String, Integer>(), 100, true);
//            for(String key : light.getRequiredValues())
//                state.getReceivedValues().put(key, 0);
//            light.setLightState(state);
//            //The import file contains a light id which is not present in-memory yet
//            if(artnetLightMemory.stream().noneMatch(l -> l.getLightId() == light.getLightId())) {
//                artnetLightMemory.add(light);
//            }
//        }
//    }
//
//    private AssetTreeNode[] syncLightsToAssets(List<ArtnetLight> lights, Attribute<?> protocolConfiguration) throws Exception {
//        List<AssetTreeNode> output = new ArrayList<AssetTreeNode>();
//
//        //Fetch all the assets that're connected to the ArtNet agent.
//        List<Asset<?>> assetsUnderProtocol = assetService.findAssets(protocolConfiguration.getAssetId().orElse(null), new AssetQuery());
//        //Get the instance of the ArtNet agent itself.
//        Asset<?> parentAgent = assetsUnderProtocol.stream().filter(a -> a.getWellKnownType() == AssetType.AGENT).findFirst().orElse(null);
//        if(parentAgent != null) {
//            for(Asset<?> asset : assetsUnderProtocol)
//            {
//                //TODO CHANGE ASSET TYPE THING TO LIGHT
//                if(asset.getWellKnownType() != AssetType.THING)
//                    continue;
//
//                if(!asset.hasAttribute("Id"))//Confirm the asset is a light
//                    continue;
//
//                //Asset is valid
//                Attribute<?> lightAttribute = asset.getAttribute("Id").orElse(null);
//                if(lightAttribute != null) {
//                    int lightId = lightAttribute.getValueAsInteger().orElse(-1);
//                    if(lightId != -1) {
//                        if(lights.stream().anyMatch(l -> l.getLightId() == lightId)) {
//                            ArtnetLight updatedLight = lights.stream().filter(l -> l.getLightId() == lightId).findFirst().orElse(null);
//                            if(updatedLight != null) {
//                                Map<String, Integer> values = new HashMap<>();
//                                for(String key : updatedLight.getRequiredValues())
//                                    values.put(key, 0);
//                                List<Attribute<?>> artNetLightAttributes = Arrays.asList(
//                                        new Attribute<>("Id", NUMBER, ValueUtil.create(updatedLight.getLightId())).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                                        new Attribute<>("GroupId", NUMBER, ValueUtil.create(updatedLight.getGroupId())).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                                        new Attribute<>("Universe", NUMBER, ValueUtil.create(updatedLight.getUniverse())).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                                        new Attribute<>("AmountOfLeds", NUMBER, ValueUtil.create(updatedLight.getAmountOfLeds())).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                                        new Attribute<>("RequiredValues", STRING, ValueUtil.create(String.join(",", updatedLight.getRequiredValues()))).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                                        new Attribute<>("ValueUtil", OBJECT, ValueUtil.parseOrNull(ValueUtil.JSON.writeValueAsString(values))).addMeta(
//                                                new MetaItem<>(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
//                                        ),
//                                        new Attribute<>("Switch", BOOLEAN, true).addMeta(
//                                                new MetaItem<>(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
//                                        ),
//                                        new Attribute<>("Dim", NUMBER, 100).addMeta(
//                                                new MetaItem<>(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
//                                        )
//                                );
//                                asset.setAttributes(artNetLightAttributes);
//                                assetService.mergeAsset(asset);
//                            }
//                        }else{
//                            if(lights.stream().noneMatch(l -> l.getLightId() == lightId))
//                                assetService.deleteAsset(asset.getId());
//                        }
//                    }
//                }
//            }
//
//            //New data is fetched based on the changes.
//            assetsUnderProtocol = assetService.findAssets(protocolConfiguration.getAssetId().orElse(null), new AssetQuery());
//            for(ArtnetLight light : lights)
//            {
//                boolean lightAssetExistsAlready = false;
//                for(Asset<?> asset : assetsUnderProtocol)
//                {
//                    //TODO CHANGE ASSET TYPE THING TO LIGHT
//                    if((asset.getWellKnownType() != AssetType.THING))
//                        continue;
//
//                    if(!asset.hasAttribute("Id"))
//                        continue;
//
//                    Attribute<?> lightIdAttribute = asset.getAttribute("Id").orElse(null);
//                    if(lightIdAttribute != null) {
//                        int lightId = lightIdAttribute.getValueAsInteger().orElse(-1);
//                        if(lightId != -1) {
//                            if(lightId == light.getLightId())
//                                lightAssetExistsAlready = true;
//                        }
//                    }
//
//                }
//                if(!lightAssetExistsAlready)
//                    output.add(formLightAsset(light, parentAgent));
//            }
//            return output.toArray(new AssetTreeNode[output.size()]);
//        }
//        return null;
//    }
//
//    protected AssetTreeNode formLightAsset(ArtnetLight light, Asset<?> parentAgent) throws JsonProcessingException {
//        Asset<?> asset = new ThingAsset();
//        asset.setId(UniqueIdentifierGenerator.generateId());
//        asset.setParent(parentAgent);
//        asset.setName("ArtNet Light " + light.getLightId());
//        asset.setType(THING);
//        Map<String, Integer> values = new HashMap<>();
//        for(String key : light.getRequiredValues())
//            values.put(key, 0);
//        List<Attribute<?>> artNetLightAttributes = Arrays.asList(
//                new Attribute<>("Id", NUMBER, ValueUtil.create(light.getLightId())).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                new Attribute<>("GroupId", NUMBER, ValueUtil.create(light.getGroupId())).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                new Attribute<>("Universe", NUMBER, ValueUtil.create(light.getUniverse())).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                new Attribute<>("AmountOfLeds", NUMBER, ValueUtil.create(light.getAmountOfLeds())).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                new Attribute<>("RequiredValues", STRING, ValueUtil.create(String.join(",", light.getRequiredValues()))).addMeta(new Meta(new MetaItem<>(READ_ONLY, true))),
//                new Attribute<>("Values", OBJECT, Values.parseOrNull(Values.JSON.writeValueAsString(values))).addMeta(
//                        new MetaItem<>(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
//                ),
//                new Attribute<>("Switch", BOOLEAN, true).addMeta(
//                        new MetaItem<>(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
//                ),
//                new Attribute<>("Dim", NUMBER, 100).addMeta(
//                        new MetaItem<>(AGENT_LINK, new AttributeRef(parentAgent.getId(), agentProtocolConfigName).toArrayValue())
//                )
//        );
//        asset.setAttributes(artNetLightAttributes);
//        return new AssetTreeNode(asset);
//    }
//
//    protected static String getLightAssetId(int groupId, int lightId, int universe) {
//        return UniqueIdentifierGenerator.generateId("ArtnetLight" + groupId + lightId + universe);
//    }
}
