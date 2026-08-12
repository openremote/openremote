/*
 * Copyright 2026, OpenRemote Inc.
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
package org.openremote.model.datapoint;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatapointPeriod {

  protected String assetId;

  protected String attributeName;

  protected Long oldest;

  protected Long latest;

  protected DatapointPeriod() {}

  @JsonCreator
  public DatapointPeriod(
      @JsonProperty("assetId") String assetId,
      @JsonProperty("attributeName") String attributeName,
      @JsonProperty("oldestTimestamp") Long oldest,
      @JsonProperty("latestTimestamp") Long latest) {
    this.assetId = assetId;
    this.attributeName = attributeName;
    this.oldest = oldest;
    this.latest = latest;
  }

  @JsonProperty("assetId")
  public String getAssetId() {
    return assetId;
  }

  @JsonProperty("attributeName")
  public String getAttributeName() {
    return attributeName;
  }

  @JsonProperty("oldestTimestamp")
  public Long getOldest() {
    return oldest;
  }

  @JsonProperty("latestTimestamp")
  public Long getLatest() {
    return latest;
  }
}
