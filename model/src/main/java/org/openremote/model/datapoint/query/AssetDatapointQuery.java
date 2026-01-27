/*
 * Copyright 2023, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.model.datapoint.query;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.openremote.model.attribute.AttributeRef;

@JsonSubTypes({
  @JsonSubTypes.Type(value = AssetDatapointAllQuery.class, name = "all"),
  @JsonSubTypes.Type(value = AssetDatapointLTTBQuery.class, name = "lttb"),
  @JsonSubTypes.Type(value = AssetDatapointIntervalQuery.class, name = "interval"),
  @JsonSubTypes.Type(value = AssetDatapointNearestQuery.class, name = "nearest")
})
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type",
    defaultImpl = AssetDatapointAllQuery.class)
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
