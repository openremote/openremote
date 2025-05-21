package org.openremote.manager.graphql;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.model.datapoint.DatapointPeriod;
import org.openremote.model.datapoint.ValueDatapoint;
import org.openremote.model.datapoint.query.AssetDatapointAllQuery;

import java.util.List;
import java.util.stream.Collectors;

public class AssetDatapointApi {
    private final AssetDatapointService dpService;
    private final AssetStorageService storage;
    private final ManagerIdentityService identity;

    public AssetDatapointApi(AssetDatapointService dpService,
                             AssetStorageService storage,
                             ManagerIdentityService identity) {
        this.dpService = dpService;
        this.storage = storage;
        this.identity = identity;
    }

    @GraphQLQuery(name = "datapoints")
    public List<ValueDatapoint<Number>> getDatapoints(
            @GraphQLNonNull @GraphQLArgument(name = "assetId") String assetId,
            @GraphQLNonNull @GraphQLArgument(name = "attributeName") String attributeName,
            @GraphQLArgument(name = "from") long from,
            @GraphQLArgument(name = "to") long to,
            @GraphQLArgument(name = "limit") Integer limit
    ) {
        // TODO: reuse your auth/permission checks here
        AssetDatapointAllQuery query = new AssetDatapointAllQuery(from, to);
        List<ValueDatapoint<?>> rawDatapoints = dpService.queryDatapoints(assetId, attributeName, query);
        
        // Convert the datapoints to use Number as the value type
        return rawDatapoints.stream()
            .map(dp -> new ValueDatapoint<>(dp.getTimestamp(), 
                dp.getValue() instanceof Number ? (Number)dp.getValue() : 
                dp.getValue() instanceof Boolean ? ((Boolean)dp.getValue() ? 1 : 0) :
                dp.getValue() instanceof String ? Double.parseDouble((String)dp.getValue()) : 0))
            .collect(Collectors.toList());
    }

    @GraphQLQuery(name = "datapointPeriod")
    public DatapointPeriod getDatapointPeriod(
            @GraphQLNonNull @GraphQLArgument(name = "assetId") String assetId,
            @GraphQLNonNull @GraphQLArgument(name = "attributeName") String attributeName
    ) {
        // TODO: apply auth checks
        return dpService.getDatapointPeriod(assetId, attributeName);
    }
}
