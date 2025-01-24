package org.openremote.model.gateway;

import java.util.List;
import java.util.Map;

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
    public Map<String, String> excludeAttributeMeta;
}
