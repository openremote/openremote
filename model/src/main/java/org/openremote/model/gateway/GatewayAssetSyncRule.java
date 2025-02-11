package org.openremote.model.gateway;

import org.openremote.model.attribute.MetaMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A sync rule lists any {@link org.openremote.model.attribute.Attribute}s and/or
 * {@link org.openremote.model.attribute.MetaItem}s that should be stripped from a given asset type before it is sent
 * to the central instance
 */
public class GatewayAssetSyncRule {
    /**
     * List of {@link org.openremote.model.attribute.Attribute} names to be stripped from the asset before syncing.
     */
    public List<String> excludeAttributes;
    /**
     * A map where keys should be the name of an {@link org.openremote.model.attribute.Attribute} to which the exclusions
     * should be applied; to apply to all attributes use the * wildcard. The list of
     * {@link org.openremote.model.attribute.MetaItem} will then be stripped from these attributes before syncing.
     */
    public Map<String, List<String>> excludeAttributeMeta;

    /**
     * A map where keys should be the name of an {@link org.openremote.model.attribute.Attribute} to which the additions
     * should be applied; to apply to all attributes use the * wildcard. The {@link MetaMap} will then be added to each
     * matching attribute before syncing.
     */
    public Map<String, MetaMap> addAttributeMeta;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GatewayAssetSyncRule that = (GatewayAssetSyncRule) o;
        return Objects.equals(excludeAttributes, that.excludeAttributes) && Objects.equals(excludeAttributeMeta, that.excludeAttributeMeta) && Objects.equals(addAttributeMeta, that.addAttributeMeta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(excludeAttributes, excludeAttributeMeta, addAttributeMeta);
    }
}
