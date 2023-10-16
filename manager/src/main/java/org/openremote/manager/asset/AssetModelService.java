/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.manager.asset;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.apache.camel.builder.RouteBuilder;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.gateway.GatewayService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebService;
import org.openremote.model.AssetModelProvider;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.openremote.model.syslog.SyslogCategory.MODEL_AND_VALUES;

// TODO: Implement model client event support
/**
 * A service for abstracting {@link org.openremote.model.util.ValueUtil} and handling local model requests vs
 * {@link org.openremote.model.asset.impl.GatewayAsset} model requests. It also manages the {@link
 * org.openremote.model.asset.AssetModelResource} and provides support for model requests via the client event bus.
 * <p>
 * Also implements an {@link AssetModelProvider} that loads descriptors from the file system; specifically in
 * {@link PersistenceService#OR_STORAGE_DIR}/{@link #DIRECTORY_NAME}; file structure should be a JSON representation
 * of {@link AssetTypeInfo}.
 */
public class AssetModelService extends RouteBuilder implements ContainerService, AssetModelProvider {
    protected static ObjectMapper JSON = ValueUtil.JSON.copy()
            .addMixIn(AssetTypeInfo.class, AssetTypeInfoMixin.class);

    @JsonDeserialize(using = AssetTypeInfoDeserializer.class)
    private static final class AssetTypeInfoMixin {
        @JsonDeserialize
        @JsonSerialize
        MetaItemDescriptor<?>[] metaItemDescriptors;
        @JsonDeserialize
        @JsonSerialize
        ValueDescriptor<?>[] valueDescriptors;

        //AttributeDescriptor<?>[] getAttributeDescriptors() { return null; }
    }

    /**
     * Extracts the {@link ValueDescriptor}s then {@link MetaItemDescriptor}s then {@link AttributeDescriptor}
     * making each available to the next during deserialization.
     */
    private static final class AssetTypeInfoDeserializer extends StdDeserializer<AssetTypeInfo> {

        private static final JavaType VALUE_DESCRIPTOR_TYPE = TypeFactory.defaultInstance().constructType(ValueDescriptor[].class);
        private static final JavaType META_ITEM_DESCRIPTOR_TYPE = TypeFactory.defaultInstance().constructType(MetaItemDescriptor[].class);
        private static final JavaType ATTRIBUTE_DESCRIPTOR_TYPE = TypeFactory.defaultInstance().constructType(AttributeDescriptor[].class);
        private static final JavaType ASSET_DESCRIPTOR_TYPE = TypeFactory.defaultInstance().constructType(AssetDescriptor.class);

        private AssetTypeInfoDeserializer() {
            super(AssetTypeInfo.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        public AssetTypeInfo deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
            if (!jp.isExpectedStartObjectToken()) {
                throw JsonMappingException.from(jp, "Must be an object");
            }

            TokenBuffer attributeDescriptorBuffer = null;
            TokenBuffer metaItemDescriptorBuffer = null;
            AssetDescriptor<?> assetDescriptor = null;
            AttributeDescriptor<?>[] attributeDescriptors = null;
            final AtomicReference<ValueDescriptor[]> valueDescriptors = new AtomicReference<>();
            final AtomicReference<MetaItemDescriptor[]> metaItemDescriptors = new AtomicReference<>();

            Function<String, ValueDescriptor<?>> valueDescriptorProvider = (name) -> {
                ValueDescriptor<?> found = null;
                if (valueDescriptors.get() != null) {
                    found = Arrays.stream(valueDescriptors.get()).filter(vd -> vd.getName().equals(name)).findFirst().orElse(null);
                }
                if (found == null) {
                    found = ValueUtil.getValueDescriptor(name).orElse(null);
                }
                return found;
            };

            Function<String, MetaItemDescriptor<?>> metaDescriptorProvider = (name) -> {
                if (metaItemDescriptors.get() != null) {
                    return Arrays.stream(metaItemDescriptors.get()).filter(mid -> mid.getName().equals(name)).findFirst().orElse(null);
                }
                return null;
            };
            ctxt.setAttribute(ValueDescriptor.ValueDescriptorDeserializer.VALUE_DESCRIPTOR_PROVIDER, valueDescriptorProvider);
            ctxt.setAttribute(MetaMap.MetaObjectDeserializer.META_DESCRIPTOR_PROVIDER, metaDescriptorProvider);

            while (jp.nextToken() != JsonToken.END_OBJECT) {
                String propName = jp.currentName();
                if (jp.currentToken() == JsonToken.FIELD_NAME) {
                    jp.nextToken();
                }
                if (jp.currentToken() == JsonToken.VALUE_NULL) {
                    continue;
                }

                switch (propName) {
                    case "attributeDescriptors" -> {
                        if (metaItemDescriptors.get() == null) {
                            attributeDescriptorBuffer = new TokenBuffer(jp, ctxt);
                            attributeDescriptorBuffer.copyCurrentStructure(jp);
                        } else {
                            attributeDescriptors = (AttributeDescriptor<?>[])ctxt.findRootValueDeserializer(ATTRIBUTE_DESCRIPTOR_TYPE).deserialize(jp, ctxt);
                        }
                    }
                    case "metaItemDescriptors" -> {
                        if (valueDescriptors.get() == null) {
                            metaItemDescriptorBuffer = new TokenBuffer(jp, ctxt);
                            metaItemDescriptorBuffer.copyCurrentStructure(jp);
                        } else {
                            metaItemDescriptors.set((MetaItemDescriptor<?>[])ctxt.findRootValueDeserializer(META_ITEM_DESCRIPTOR_TYPE).deserialize(jp, ctxt));
                        }
                    }
                    case "valueDescriptors" -> {
                        valueDescriptors.set((ValueDescriptor<?>[]) ctxt.findRootValueDeserializer(VALUE_DESCRIPTOR_TYPE).deserialize(jp, ctxt));
                    }
                    case "assetDescriptor" -> {
                        assetDescriptor = (AssetDescriptor<?>) ctxt.findRootValueDeserializer(ASSET_DESCRIPTOR_TYPE).deserialize(jp, ctxt);
                    }
                }
            }

            if (metaItemDescriptorBuffer != null) {
                JsonParser parser = metaItemDescriptorBuffer.asParser();
                parser.nextToken();
                metaItemDescriptors.set((MetaItemDescriptor<?>[])ctxt.findRootValueDeserializer(META_ITEM_DESCRIPTOR_TYPE).deserialize(parser, ctxt));
            }
            if (attributeDescriptorBuffer != null) {
                JsonParser parser = attributeDescriptorBuffer.asParser();
                parser.nextToken();
                attributeDescriptors = (AttributeDescriptor<?>[])ctxt.findRootValueDeserializer(ATTRIBUTE_DESCRIPTOR_TYPE).deserialize(parser, ctxt);
            }

            if (assetDescriptor == null) {
                throw new JsonParseException(jp, "Must contain an asset descriptor");
            }
            return new AssetTypeInfo(
                assetDescriptor,
                attributeDescriptors,
                metaItemDescriptors.get() != null ? metaItemDescriptors.get() : new MetaItemDescriptor[0],
                valueDescriptors.get() != null ? valueDescriptors.get() : new ValueDescriptor[0]
            );
        }
    }

    protected static Logger LOG = SyslogCategory.getLogger(MODEL_AND_VALUES, AssetModelService.class);
    public static final String DIRECTORY_NAME = "asset_model";
    protected ManagerIdentityService identityService;
    protected ClientEventService clientEventService;
    protected GatewayService gatewayService;
    protected PersistenceService persistenceService;
    protected Map<String, AssetTypeInfo> dynamicAssetTypeInfos;
    protected Path storageDir;

    @Override
    public int getPriority() {
        // Need storageDir from PersistenceService
        return PersistenceService.PRIORITY + 10;
    }

    @Override
    public void configure() throws Exception {
//        // React if a client wants to read assets and attributes
//        from(CLIENT_EVENT_TOPIC)
//            .routeId("FromClientReadRequests")
//            .filter(
//                or(body().isInstanceOf(ReadAssetsEvent.class), body().isInstanceOf(ReadAssetEvent.class), body().isInstanceOf(ReadAttributeEvent.class)))
//            .choice()
//            .when(body().isInstanceOf(ReadAssetEvent.class))
//            .end();
    }

    @Override
    public void init(Container container) throws Exception {

        identityService = container.getService(ManagerIdentityService.class);
        clientEventService = container.getService(ClientEventService.class);
        gatewayService = container.getService(GatewayService.class);
        persistenceService = container.getService(PersistenceService.class);

        container.getService(ManagerWebService.class).addApiSingleton(
            new AssetModelResourceImpl(
                container.getService(TimerService.class),
                identityService,
                this
            )
        );

        container.getService(MessageBrokerService.class).getContext().addRoutes(this);
    }

    protected void initDynamicModel() {
        try {
            Path rootStorageDir = persistenceService.getStorageDir();
            storageDir = rootStorageDir.resolve(DIRECTORY_NAME);

            if (!Files.exists(storageDir)) {
                try {
                    Files.createDirectories(storageDir);
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "Failed to create asset model storage directory", e);
                    throw new RuntimeException(e);
                }
            } else if (!Files.isDirectory(storageDir)) {
                throw new IllegalStateException("Asset model storage directory is not a directory: " + storageDir);
            }

            dynamicAssetTypeInfos = loadDescriptors(AssetTypeInfo.class, storageDir)
                .collect(Collectors.toMap(ati -> ati.getAssetDescriptor().getName(), ati -> ati));

            LOG.fine("Loaded asset type infos from '" + storageDir + "': count = " + dynamicAssetTypeInfos.size());
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to load custom asset types from '" + storageDir + "':" + e.getMessage());
        }
    }

    @Override
    public void start(Container container) throws Exception {

    }

    @Override
    public void stop(Container container) throws Exception {

    }


    @Override
    public boolean useAutoScan() {
        return false;
    }

    @Override
    public AssetDescriptor<?>[] getAssetDescriptors() {
        if (dynamicAssetTypeInfos == null) {
            return null;
        }
        return dynamicAssetTypeInfos.values().stream().map(AssetTypeInfo::getAssetDescriptor).toArray(AssetDescriptor[]::new);
    }

    @Override
    public Map<String, Collection<AttributeDescriptor<?>>> getAttributeDescriptors() {
        if (dynamicAssetTypeInfos == null) {
            initDynamicModel();
        }
        return dynamicAssetTypeInfos.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, es -> es.getValue().getAttributeDescriptors().values()));
    }

    @Override
    public Map<String, Collection<MetaItemDescriptor<?>>> getMetaItemDescriptors() {
        if (dynamicAssetTypeInfos == null) {
            initDynamicModel();
        }
        return dynamicAssetTypeInfos.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, es -> Arrays.asList(es.getValue().getMetaItemDescriptors())));
    }

    @Override
    public Map<String, Collection<ValueDescriptor<?>>> getValueDescriptors() {
        if (dynamicAssetTypeInfos == null) {
            initDynamicModel();
        }
        return dynamicAssetTypeInfos.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, es -> Arrays.asList(es.getValue().getValueDescriptors())));
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    public AssetTypeInfo[] getAssetInfos(String parentId, String parentType) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return new AssetTypeInfo[0];
        }

        return ValueUtil.getAssetInfos(parentType);
    }

    public AssetTypeInfo getAssetInfo(String parentId, String assetType) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return null;
        }

        return ValueUtil.getAssetInfo(assetType).orElse(null);
    }

    public AssetDescriptor<?>[] getAssetDescriptors(String parentId, String parentType) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return new AssetDescriptor[0];
        }

        return ValueUtil.getAssetDescriptors(parentType);
    }

    public Map<String, ValueDescriptor<?>> getValueDescriptors(String parentId) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return null;
        }

        return ValueUtil.getValueDescriptors();
    }

    public Map<String, MetaItemDescriptor<?>> getMetaItemDescriptors(String parentId) {

        if (!TextUtil.isNullOrEmpty(parentId) && gatewayService.getLocallyRegisteredGatewayId(parentId, null) != null) {
            // TODO: Asset is on a gateway so need to get model info from the gateway instance
            return null;
        }

        return ValueUtil.getMetaItemDescriptors();
    }

    protected <T> T parse(String jsonString, Class<T> type) throws JsonProcessingException {
        return JSON.readValue(jsonString, JSON.constructType(type));
    }

    protected <T> Stream<T> loadDescriptors(Class<T> descriptorClazz, Path descriptorPath) {
        try {
            return Files.list(descriptorPath).map(descriptorFile -> {
                LOG.log(Level.FINE, "Reading descriptor from: " + descriptorFile);
                String descriptorStr;
                try {
                    descriptorStr = Files.readString(descriptorFile);
                    if (descriptorStr != null) {
                        return parse(descriptorStr, descriptorClazz);
                    }
                } catch (JsonProcessingException e) {
                    LOG.log(Level.SEVERE, "Failed to parse descriptor file '" + descriptorFile + "': " + e.getMessage());
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "Failed to read descriptor file '" + descriptorFile + "': " + e.getMessage());
                }
                return null;
            }).filter(Objects::nonNull);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
