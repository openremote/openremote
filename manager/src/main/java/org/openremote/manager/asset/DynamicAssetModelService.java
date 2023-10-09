package org.openremote.manager.asset;

import org.openremote.container.persistence.PersistenceService;
import org.openremote.model.AssetModelProvider;
import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.syslog.SyslogCategory;
import org.openremote.model.util.ValueUtil;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemDescriptor;
import org.openremote.model.value.ValueDescriptor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.openremote.model.syslog.SyslogCategory.MODEL_AND_VALUES;

/**
 * An {@link AssetModelProvider} that loads descriptors from the file system; specifically in
 * {@link PersistenceService#OR_STORAGE_DIR}/{@link #DIRECTORY_NAME}; files can be updated at runtime to provide a
 * dynamic element to the asset model.
 */
public class DynamicAssetModelService implements ContainerService, AssetModelProvider {

    // Doesn't make sense to have asset and attribute descriptors separate
    protected static class AssetAndAttributeDescriptors {
        protected AssetDescriptor<?> assetDescriptor;
        protected AttributeDescriptor<?>[] attributeDescriptors;

        public AssetAndAttributeDescriptors(AssetDescriptor<?> assetDescriptor, AttributeDescriptor<?>[] attributeDescriptors) {
            this.assetDescriptor = assetDescriptor;
            this.attributeDescriptors = attributeDescriptors;
        }
    }

    protected static Logger LOG = SyslogCategory.getLogger(MODEL_AND_VALUES, DynamicAssetModelService.class);
    public static final String DIRECTORY_NAME = "asset_model";
    public static final String ASSET_DESCRIPTORS_DIR = "asset";
    public static final String META_ITEM_DIR = "meta";
    public static final String VALUE_DIR = "value";
    protected Path storageDir;

    @Override
    public int getPriority() {
        return PersistenceService.PRIORITY + 10;
    }

    @Override
    public void init(Container container) {
        Path rootStorageDir = container.getService(PersistenceService.class).getStorageDir();
        storageDir = rootStorageDir.resolve(DIRECTORY_NAME);

        Stream.of(rootStorageDir.resolve(ASSET_DESCRIPTORS_DIR), storageDir.resolve(META_ITEM_DIR), storageDir.resolve(VALUE_DIR))
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
        Path assetDescriptorsPath = storageDir.resolve(ASSET_DESCRIPTORS_DIR);
        try {
            return Files.list(assetDescriptorsPath).map(assetDescriptorFile -> {
                LOG.log(Level.FINE, "Reading asset descriptor from: " + assetDescriptorFile);
                String assetDescriptorStr;
                try {
                    assetDescriptorStr = Files.readString(assetDescriptorFile);
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "Failed to read asset descriptor from file: " + assetDescriptorFile, e);
                    throw new RuntimeException(e);
                }
                return ValueUtil.parse(assetDescriptorStr, AssetDescriptor.class).orElseThrow(() -> {
                    String msg = "Failed to parse asset descriptor from file: " + assetDescriptorFile;
                    LOG.log(Level.SEVERE, msg);
                    return new RuntimeException(msg);
                });
            }).toArray(AssetDescriptor[]::new);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<Class<? extends Asset<?>>, List<AttributeDescriptor<?>>> getAttributeDescriptors() {
        return null;
    }

    @Override
    public Map<Class<? extends Asset<?>>, List<MetaItemDescriptor<?>>> getMetaItemDescriptors() {
        return null;
    }

    @Override
    public Map<Class<? extends Asset<?>>, List<ValueDescriptor<?>>> getValueDescriptors() {
        return null;
    }
}
