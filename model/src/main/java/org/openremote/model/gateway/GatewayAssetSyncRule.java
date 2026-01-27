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
package org.openremote.model.gateway;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openremote.model.attribute.MetaMap;

/**
 * A sync rule lists any {@link org.openremote.model.attribute.Attribute}s and/or {@link
 * org.openremote.model.attribute.MetaItem}s that should be removed and/or added from/to a given
 * asset type before it is sent to the central instance
 */
public class GatewayAssetSyncRule {
  /**
   * List of {@link org.openremote.model.attribute.Attribute} names to be stripped from the asset
   * before syncing.
   */
  public List<String> excludeAttributes;

  /**
   * A map where keys should be the name of an {@link org.openremote.model.attribute.Attribute} to
   * which the exclusions should be applied; to apply to all attributes use the * wildcard. The list
   * of {@link org.openremote.model.attribute.MetaItem} will then be stripped from these attributes
   * before syncing.
   */
  public Map<String, List<String>> excludeAttributeMeta;

  /**
   * A map where keys should be the name of an {@link org.openremote.model.attribute.Attribute} to
   * which the additions should be applied; to apply to all attributes use the * wildcard. The
   * {@link MetaMap} will then be added to each matching attribute before syncing.
   */
  public Map<String, MetaMap> addAttributeMeta;

  public List<String> getExcludeAttributes() {
    return excludeAttributes;
  }

  public GatewayAssetSyncRule setExcludeAttributes(List<String> excludeAttributes) {
    this.excludeAttributes = excludeAttributes;
    return this;
  }

  public Map<String, List<String>> getExcludeAttributeMeta() {
    return excludeAttributeMeta;
  }

  public GatewayAssetSyncRule setExcludeAttributeMeta(
      Map<String, List<String>> excludeAttributeMeta) {
    this.excludeAttributeMeta = excludeAttributeMeta;
    return this;
  }

  public Map<String, MetaMap> getAddAttributeMeta() {
    return addAttributeMeta;
  }

  public GatewayAssetSyncRule setAddAttributeMeta(Map<String, MetaMap> addAttributeMeta) {
    this.addAttributeMeta = addAttributeMeta;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GatewayAssetSyncRule that = (GatewayAssetSyncRule) o;
    return Objects.equals(excludeAttributes, that.excludeAttributes)
        && Objects.equals(excludeAttributeMeta, that.excludeAttributeMeta)
        && Objects.equals(addAttributeMeta, that.addAttributeMeta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(excludeAttributes, excludeAttributeMeta, addAttributeMeta);
  }
}
