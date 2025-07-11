/*
 * Copyright 2015, OpenRemote Inc.
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
package org.openremote.model.flow.catalog;

import org.openremote.model.flow.NodeColor;

public class CatalogItem {

  public String label;

  public CatalogCategory category;

  public String nodeType;

  public NodeColor nodeColor;

  protected CatalogItem() {}

  public CatalogItem(String label, CatalogCategory category, String nodeType, NodeColor nodeColor) {
    this.label = label;
    this.category = category;
    this.nodeType = nodeType;
    this.nodeColor = nodeColor;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public CatalogCategory getCategory() {
    return category;
  }

  public void setCategory(CatalogCategory category) {
    this.category = category;
  }

  public String getNodeType() {
    return nodeType;
  }

  public void setNodeType(String nodeType) {
    this.nodeType = nodeType;
  }

  public NodeColor getNodeColor() {
    return nodeColor;
  }

  public void setNodeColor(NodeColor nodeColor) {
    this.nodeColor = nodeColor;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "label='"
        + label
        + '\''
        + ", category="
        + category
        + ", nodeType="
        + nodeType
        + '}';
  }
}
