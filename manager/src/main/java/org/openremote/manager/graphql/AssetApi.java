package org.openremote.manager.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;
import org.openremote.container.Container;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.datapoint.AssetDatapoint;
import org.openremote.model.datapoint.DatapointPeriod;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointAllQuery;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.impl.ThingAsset;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.query.filter.*;

import java.util.List;

@GraphQLType(name = "Asset")
public class AssetApi {
    private final AssetStorageService storage;
    private final ManagerIdentityService identity;

    public AssetApi(AssetStorageService storage, ManagerIdentityService identity) {
        this.storage = storage;
        this.identity = identity;
    }

    @GraphQLQuery(name = "assets")
    public List<ThingAsset> getAssets(
            @GraphQLArgument(name = "AssetQuery") AssetQuery query
    ) {
        // Apply security context
        if (query == null) {
            query = new AssetQuery();
        }
        
        // Set default access level based on user's permissions
        if (query.access == null) {
            query.access(AssetQuery.Access.PRIVATE);
        }

        // Convert results to ThingAsset since GraphQL needs a concrete type
        return storage.findAll(query).stream()
            .map(asset -> {
                if (asset instanceof ThingAsset) {
                    return (ThingAsset) asset;
                }
                // Convert to ThingAsset if it's a different type
                ThingAsset thingAsset = new ThingAsset(asset.getName());
                thingAsset.setId(asset.getId());
                thingAsset.setVersion(asset.getVersion());
                thingAsset.setCreatedOn(asset.getCreatedOn());
                thingAsset.setAccessPublicRead(asset.isAccessPublicRead());
                thingAsset.setParentId(asset.getParentId());
                thingAsset.setRealm(asset.getRealm());
                thingAsset.setAttributes(asset.getAttributes());
                return thingAsset;
            })
            .toList();
    }

    @GraphQLQuery(name = "asset")
    public ThingAsset getAsset(
            @GraphQLNonNull @GraphQLArgument(name = "id") String id,
            @GraphQLArgument(name = "loadComplete") Boolean loadComplete
    ) {
        // Apply security context
        AssetQuery query = new AssetQuery()
            .ids(id)
            .access(AssetQuery.Access.PRIVATE);

        if (loadComplete != null && !loadComplete) {
            query.select(new AssetQuery.Select().excludeAttributes());
        }

        Asset<?> asset = storage.find(query);
        if (asset == null) {
            return null;
        }

        if (asset instanceof ThingAsset) {
            return (ThingAsset) asset;
        }

        // Convert to ThingAsset if it's a different type
        ThingAsset thingAsset = new ThingAsset(asset.getName());
        thingAsset.setId(asset.getId());
        thingAsset.setVersion(asset.getVersion());
        thingAsset.setCreatedOn(asset.getCreatedOn());
        thingAsset.setAccessPublicRead(asset.isAccessPublicRead());
        thingAsset.setParentId(asset.getParentId());
        thingAsset.setRealm(asset.getRealm());
        thingAsset.setAttributes(asset.getAttributes());
        return thingAsset;
    }

    @GraphQLQuery(name = "assetNames")
    public List<String> getAssetNames(
            @GraphQLNonNull @GraphQLArgument(name = "ids") String[] ids
    ) {
        // Apply security context
        AssetQuery query = new AssetQuery()
            .ids(ids)
            .access(AssetQuery.Access.PRIVATE);

        // First verify all assets are accessible
        List<ThingAsset> accessibleAssets = getAssets(query);
        if (accessibleAssets.size() != ids.length) {
            throw new IllegalArgumentException("Not all requested assets are accessible");
        }

        return storage.findNames(ids);
    }
}
