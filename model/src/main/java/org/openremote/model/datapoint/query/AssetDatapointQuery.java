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
import org.openremote.model.attribute.AttributeRef;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;

@JsonSubTypes({
        @JsonSubTypes.Type(value = AssetDatapointAllQuery.class, name = "all"),
        @JsonSubTypes.Type(value = AssetDatapointLTTBQuery.class, name = "lttb"),
        @JsonSubTypes.Type(value = AssetDatapointIntervalQuery.class, name = "interval")
})
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        defaultImpl = AssetDatapointAllQuery.class
)
public abstract class AssetDatapointQuery implements Serializable {

    public long fromTimestamp;
    public long toTimestamp;
    public LocalDateTime fromTime;
    public LocalDateTime toTime;

    public String getSQLQuery(String tableName, Class<?> attributeType) throws IllegalStateException {
        return null;
    }

    public HashMap<Integer, Object> getSQLParameters(AttributeRef attributeRef) {
        return null;
    }
}
