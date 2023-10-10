package org.openremote.manager.asset;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.util.StdConverter;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.AssetModelProvider;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.AssetTypeInfo;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;
import org.openremote.model.syslog.SyslogCategory;
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

/**
 * An {@link AssetModelProvider} that loads descriptors from the file system; specifically in
 * {@link PersistenceService#OR_STORAGE_DIR}/{@link #DIRECTORY_NAME}; files can be updated at runtime to provide a
 * dynamic element to the asset model. File structure:
 * <ul>
 *     <li>{@link #ASSET_DESCRIPTORS_DIR} - {@link AssetTypeInfo} files with filename set to {@link AssetDescriptor#getName()}</li>
 *     <li>{@link #META_DESCRIPTORS_DIR} - {@link MetaItemDescriptor} files with filename set to {@link MetaItemDescriptor#getName()}</li>
 *     <li>{@link #VALUE_DESCRIPTORS_DIR} - {@link ValueDescriptor} files with filename set to {@link ValueDescriptor#getName()}</li>
 * </ul>
 */
public class DynamicAssetModelService implements ContainerService, AssetModelProvider {

    protected ObjectMapper JSON;

    private interface AssetTypeInfoMixin {
        @JsonDeserialize(contentConverter = StringMetaItemDescriptorConverter.class)
        MetaItemDescriptor<?>[] getMetaItemDescriptors();
        @JsonDeserialize(contentConverter = StringValueDescriptorConverter.class)
        ValueDescriptor<?>[] getValueDescriptors();
    }

    private interface AbstractNameValueDescriptorHolderMixin<T> {
        @JsonDeserialize(contentConverter = StringValueDescriptorConverter.class)
        ValueDescriptor<T> getType();
    }

    @JsonDeserialize(using = MetaObjectDeserializer.class)
    private interface MetaMapMixin {}

    private static class StringMetaItemDescriptorConverter extends StdConverter<String, MetaItemDescriptor<?>> {
        @Override
        public MetaItemDescriptor<?> convert(String value) {
            return ValueUtil.getMetaItemDescriptor(value).orElse(null);
        }
    }

    private static class StringValueDescriptorConverter extends StdConverter<String, ValueDescriptor<?>> {
        @Override
        public ValueDescriptor<?> convert(String value) {
            return getValueDescriptor(value).orElse(null);
        }
    }

    protected static class MetaObjectDeserializer extends StdDeserializer<MetaMap> {

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
        if (metaDescriptors != null) {
            MetaItemDescriptor<?> descriptor = metaDescriptors.get(name);
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
        if (valueDescriptors != null) {
            ValueDescriptor<?> descriptor = valueDescriptors.get(name);
            if (descriptor != null) {
                return Optional.of(descriptor);
            }
        }

        return ValueUtil.getValueDescriptor(name);
    }

    protected <T> Optional<T> parse(String jsonString, Class<T> type) {
        try {
            return Optional.of(JSON.readValue(jsonString, JSON.constructType(type)));
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse JSON", e);
        }
        return Optional.empty();
    }

    protected static Logger LOG = SyslogCategory.getLogger(MODEL_AND_VALUES, DynamicAssetModelService.class);
    public static final String DIRECTORY_NAME = "asset_model";
    public static final String ASSET_DESCRIPTORS_DIR = "asset";
    public static final String META_DESCRIPTORS_DIR = "meta";
    public static final String VALUE_DESCRIPTORS_DIR = "value";
    protected static Map<String, ValueDescriptor<?>> valueDescriptors; // This is needed for deserialisation - This class is a singleton so it's ok
    protected static Map<String, MetaItemDescriptor<?>> metaDescriptors; // This is needed for deserialisation - This class is a singleton so it's ok
    protected Map<String, AssetTypeInfo> assetTypeInfos;
    protected Path storageDir;

    @Override
    public int getPriority() {
        return PersistenceService.PRIORITY + 10;
    }

    @Override
    public void init(Container container) {
        if (JSON == null) {
             JSON = ValueUtil.JSON.copy()
                .addMixIn(AssetTypeInfo.class, AssetTypeInfoMixin.class)
                .addMixIn(AbstractNameValueDescriptorHolder.class, AbstractNameValueDescriptorHolderMixin.class)
                .addMixIn(MetaMap.class, MetaMapMixin.class);
        }

        Path rootStorageDir = container.getService(PersistenceService.class).getStorageDir();
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
        valueDescriptors = loadDescriptors(ValueDescriptor.class, storageDir.resolve(VALUE_DESCRIPTORS_DIR))
            .collect(Collectors.toMap(ValueDescriptor::getName, vd -> (ValueDescriptor<?>) vd));
        // Then meta item descriptors
        metaDescriptors = loadDescriptors(MetaItemDescriptor.class, storageDir.resolve(META_DESCRIPTORS_DIR))
            .collect(Collectors.toMap(MetaItemDescriptor::getName, md -> (MetaItemDescriptor<?>) md));
        // Then attribute and asset descriptors
        assetTypeInfos = loadDescriptors(AssetTypeInfo.class, storageDir.resolve(ASSET_DESCRIPTORS_DIR))
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

    @Override
    public AssetDescriptor<?>[] getAssetDescriptors() {
        if (assetTypeInfos == null) {
            return null;
        }
        return assetTypeInfos.values().stream().map(AssetTypeInfo::getAssetDescriptor).toArray(AssetDescriptor[]::new);
    }

    @Override
    public Map<String, Collection<AttributeDescriptor<?>>> getAttributeDescriptors() {
        if (assetTypeInfos == null) {
            return null;
        }
        return assetTypeInfos.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, es -> es.getValue().getAttributeDescriptors().values()));
    }

    @Override
    public Map<String, Collection<MetaItemDescriptor<?>>> getMetaItemDescriptors() {
        if (assetTypeInfos == null) {
            return null;
        }
        return assetTypeInfos.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, es -> Arrays.asList(es.getValue().getMetaItemDescriptors())));
    }

    @Override
    public Map<String, Collection<ValueDescriptor<?>>> getValueDescriptors() {
        if (assetTypeInfos == null) {
            return null;
        }
        return assetTypeInfos.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, es -> Arrays.asList(es.getValue().getValueDescriptors())));
    }
//
//    public boolean mergeAssetDescriptor(AssetTypeInfo descriptor) {
//
//    }
//
//    public boolean removeAssetDescriptor(String name) {
//
//    }
//
//    public boolean mergeMetaItemDescriptor(MetaItemDescriptor<?> descriptor) {
//
//    }
//
//    public boolean removeMetaItemDescriptor(String name) {
//
//    }
//
//    public boolean mergeValueDescriptor(ValueDescriptor<?> descriptor) {
//
//    }
//
//    public boolean removeValueDescriptor(String name) {
//
//    }
}
