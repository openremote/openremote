/*
 * Copyright 2023, OpenRemote Inc.
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
package org.openremote.model.datapoint.query;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.openremote.model.attribute.AttributeRef;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;

@JsonSubTypes({
        @JsonSubTypes.Type(value = AssetDatapointAllQuery.class, name = "all"),
        @JsonSubTypes.Type(value = AssetDatapointLTTBQuery.class, name = "lttb"),
        @JsonSubTypes.Type(value = AssetDatapointIntervalQuery.class, name = "interval"),
        @JsonSubTypes.Type(value = AssetDatapointNearestQuery.class, name = "nearest")

})
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        defaultImpl = AssetDatapointAllQuery.class
)
@Schema(
        description = "Polymorphic historical-datapoint query. Select `all` to return every value, "
                + "`interval` to aggregate values into time buckets, `lttb` to downsample a numeric "
                + "or boolean series, or `nearest` to retrieve the last value at or before a time.",
        discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "all", schema = AssetDatapointAllQuery.class),
                @DiscriminatorMapping(value = "interval", schema = AssetDatapointIntervalQuery.class),
                @DiscriminatorMapping(value = "lttb", schema = AssetDatapointLTTBQuery.class),
                @DiscriminatorMapping(value = "nearest", schema = AssetDatapointNearestQuery.class)
        }
)
public abstract class AssetDatapointQuery implements Serializable {

    @Schema(description = "Inclusive lower range bound as Unix time in milliseconds for `all`, `interval`, "
            + "and `lttb`; ignored when `fromTime` is supplied. For `nearest` this is instead the requested "
            + "time in Unix seconds and is the only time field used.",
            example = "1767225600000")
    public long fromTimestamp;

    @Schema(description = "Inclusive upper range bound as Unix time in milliseconds for `all`, `interval`, "
            + "and `lttb`; ignored when `toTime` is supplied and not used by `nearest`.",
            example = "1767312000000")
    public long toTimestamp;

    @JsonDeserialize(using = AssetDatapointQueryLocalDateTimeDeserializer.class)
    @Schema(description = "Inclusive lower range bound for `all`, `interval`, and `lttb` as an ISO local or "
            + "offset date-time. A local value is interpreted in the server time zone; an offset value is "
            + "converted to the server time zone. Takes precedence over `fromTimestamp` and is ignored by "
            + "`nearest`.",
            example = "2026-01-01T00:00:00Z")
    public LocalDateTime fromTime;

    @JsonDeserialize(using = AssetDatapointQueryLocalDateTimeDeserializer.class)
    @Schema(description = "Inclusive upper range bound for `all`, `interval`, and `lttb` as an ISO local or "
            + "offset date-time. A local value is interpreted in the server time zone; an offset value is "
            + "converted to the server time zone. Takes precedence over `toTimestamp` and is ignored by "
            + "`nearest`.",
            example = "2026-01-02T00:00:00Z")
    public LocalDateTime toTime;

    public String getSQLQuery(String tableName, Class<?> attributeType) throws IllegalStateException {
        return null;
    }

    public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
        return null;
    }
}
