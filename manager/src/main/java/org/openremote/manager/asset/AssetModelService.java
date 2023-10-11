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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.util.StdConverter;
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
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.TextUtil;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AbstractNameValueDescriptorHolder;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
 * {@link PersistenceService#OR_STORAGE_DIR}/{@link #DIRECTORY_NAME}; files can be updated at runtime to provide a
 * dynamic element to the asset model. File structure:
 * <ul>
 *     <li>{@link #ASSET_DESCRIPTORS_DIR} - {@link AssetTypeInfo} files with filename set to {@link AssetDescriptor#getName()}</li>
 *     <li>{@link #META_DESCRIPTORS_DIR} - {@link MetaItemDescriptor} files with filename set to {@link MetaItemDescriptor#getName()}</li>
 *     <li>{@link #VALUE_DESCRIPTORS_DIR} - {@link ValueDescriptor} files with filename set to {@link ValueDescriptor#getName()}</li>
 * </ul>
 */
public class AssetModelService extends RouteBuilder implements ContainerService, AssetModelProvider {
    protected ObjectMapper JSON;


    private class AssetTypeInfoMixin {
        @JsonDeserialize(contentConverter = StringMetaItemDescriptorConverter.class)
        MetaItemDescriptor<?>[] metaItemDescriptors;
        @JsonDeserialize(contentConverter = StringValueDescriptorConverter.class)
        ValueDescriptor<?>[] valueDescriptors;
    }

    private class AbstractNameValueDescriptorHolderMixin<T> {
        @JsonDeserialize(converter = StringValueDescriptorConverter.class)
        ValueDescriptor<T> type;
    }

    @JsonDeserialize(using = MetaObjectDeserializer.class)
    private interface MetaMapMixin {}

    private static class StringMetaItemDescriptorConverter extends StdConverter<String, MetaItemDescriptor<?>> {
        @Override
        public MetaItemDescriptor<?> convert(String value) {
            return dynamicMetaDescriptors != null ? dynamicMetaDescriptors.get(value) : null;
        }
    }

    private static class StringValueDescriptorConverter extends StdConverter<String, ValueDescriptor<?>> {
        @Override
        public ValueDescriptor<?> convert(String value) {
            return getValueDescriptor(value).orElse(null);
        }
    }

    private static class MetaObjectDeserializer extends StdDeserializer<MetaMap> {

        public MetaObjectDeserializer() {
            super(MetaMap.class);
        }

        @Override
        public MetaMap deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            List<MetaItem<?>> list = MetaMap.deserialiseMetaMap(jp, ctxt, (metaItemName) -> getMetaItemDescriptor(metaItemName)
                .map(MetaItemDescriptor::getType).orElse(null));

            MetaMap metaMap = new MetaMap();
            metaMap.putAll(list);
            return metaMap;
        }
    }

    /**
     * This is needed as {@link MetaItemDescriptor}s from this model provider aren't yet available from
     * {@link ValueUtil#getMetaItemDescriptor}
     */
    protected static Optional<MetaItemDescriptor<?>> getMetaItemDescriptor(String name) {
        if (dynamicMetaDescriptors != null) {
            MetaItemDescriptor<?> descriptor = dynamicMetaDescriptors.get(name);
            if (descriptor != null) {
                return Optional.of(descriptor);
            }
        }
        return ValueUtil.getMetaItemDescriptor(name);
    }

    /**
     * This is needed as {@link ValueDescriptor}s from this model provider aren't yet available from
     * {@link ValueUtil#getValueDescriptor}
     */
    protected static Optional<ValueDescriptor<?>> getValueDescriptor(String name) {
        if (dynamicValueDescriptors != null) {
            ValueDescriptor<?> descriptor = dynamicValueDescriptors.get(name);
            if (descriptor != null) {
                return Optional.of(descriptor);
            }
        }

        return ValueUtil.getValueDescriptor(name);
    }

    protected static Logger LOG = SyslogCategory.getLogger(MODEL_AND_VALUES, AssetModelService.class);
    protected ManagerIdentityService identityService;
    protected ClientEventService clientEventService;
    protected GatewayService gatewayService;
    protected PersistenceService persistenceService;
    public static final String DIRECTORY_NAME = "model";
    public static final String ASSET_DESCRIPTORS_DIR = "asset";
    public static final String META_DESCRIPTORS_DIR = "meta";
    public static final String VALUE_DESCRIPTORS_DIR = "value";
    protected static Map<String, ValueDescriptor<?>> dynamicValueDescriptors; // This is needed for deserialisation - This class is a singleton so it's ok
    protected static Map<String, MetaItemDescriptor<?>> dynamicMetaDescriptors; // This is needed for deserialisation - This class is a singleton so it's ok
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
        if (JSON == null) {
            JSON = ValueUtil.JSON.copy()
                .addMixIn(AssetTypeInfo.class, AssetTypeInfoMixin.class)
                .addMixIn(AbstractNameValueDescriptorHolder.class, AbstractNameValueDescriptorHolderMixin.class)
                .addMixIn(MetaMap.class, MetaMapMixin.class);
        }

        Path rootStorageDir = persistenceService.getStorageDir();
        storageDir = rootStorageDir.resolve(DIRECTORY_NAME);

        Stream.of(rootStorageDir.resolve(ASSET_DESCRIPTORS_DIR), storageDir.resolve(META_DESCRIPTORS_DIR), storageDir.resolve(VALUE_DESCRIPTORS_DIR))
            .forEach(modelPath -> {
                if (!Files.exists(modelPath)) {
                    try {
                        Files.createDirectories(modelPath);
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Failed to create asset model storage directory", e);
                        throw new RuntimeException(e);
                    }
                } else if (!Files.isDirectory(modelPath)) {
                    throw new IllegalStateException("Asset model storage directory is not a directory: " + modelPath);
                }
            });

        // Load value descriptors first
        dynamicValueDescriptors = loadDescriptors(ValueDescriptor.class, storageDir.resolve(VALUE_DESCRIPTORS_DIR))
            .collect(Collectors.toMap(ValueDescriptor::getName, vd -> (ValueDescriptor<?>) vd));
        // Then meta item descriptors
        dynamicMetaDescriptors = loadDescriptors(MetaItemDescriptor.class, storageDir.resolve(META_DESCRIPTORS_DIR))
            .collect(Collectors.toMap(MetaItemDescriptor::getName, md -> (MetaItemDescriptor<?>) md));
        // Then attribute and asset descriptors
        dynamicAssetTypeInfos = loadDescriptors(AssetTypeInfo.class, storageDir.resolve(ASSET_DESCRIPTORS_DIR))
            .collect(Collectors.toMap(ati -> ati.getAssetDescriptor().getName(), ati -> ati));
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

    protected <T> Optional<T> parse(String jsonString, Class<T> type) {
        try {
            return Optional.of(JSON.readValue(jsonString, JSON.constructType(type)));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse JSON", e);
        }
        return Optional.empty();
    }

    protected <T> Stream<T> loadDescriptors(Class<T> descriptorClazz, Path descriptorPath) {
        try {
            return Files.list(descriptorPath).map(descriptorFile -> {
                LOG.log(Level.FINE, "Reading meta item descriptors from: " + descriptorFile);
                String descriptorStr = null;
                try {
                    descriptorStr = Files.readString(descriptorFile);
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "Failed to read " + descriptorClazz.getSimpleName() + " from file: " + descriptorFile, e);
                }
                if (descriptorStr != null) {
                    return parse(descriptorStr, descriptorClazz).orElseGet(() -> {
                        String msg = "Failed to parse descriptor(s) from file: " + descriptorFile;
                        LOG.log(Level.SEVERE, msg);
                        return null;
                    });
                }
                return null;
            }).filter(Objects::nonNull);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
