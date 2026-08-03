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
package org.openremote.model.rules.flow;

public class NodeSocket {
  // Generation of ID is the responsibility of the npm package
  private String id;
  private String name;
  private NodeDataType type;
  // Assignment of Node ID is the responsibility of the npm package
  private String nodeId;
  // Assignment of index is the responsibility of the npm package
  private int index;

  public NodeSocket(String name, NodeDataType type) {
    this.id = "INVALID ID";
    this.name = name;
    this.type = type;
    this.nodeId = "INVALID NODE ID";
    this.index = 0;
  }

  public NodeSocket() {
    id = "INVALID ID";
    name = "Unnamed socket";
    type = NodeDataType.ANY;
    nodeId = "INVALID NODE ID";
    index = -1;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public NodeDataType getType() {
    return type;
  }

  public void setType(NodeDataType type) {
    this.type = type;
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof NodeSocket) return ((NodeSocket) obj).id.equals(id);
    else return false;
  }
}
