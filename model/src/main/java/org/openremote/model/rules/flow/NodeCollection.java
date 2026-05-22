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

public class NodeCollection {
  private String name;
  private String description;
  private Node[] nodes;
  private NodeConnection[] connections;

  public NodeCollection(
      String name, String description, Node[] nodes, NodeConnection[] connections) {
    this.name = name;
    this.description = description;
    this.nodes = nodes;
    this.connections = connections;
  }

  public NodeCollection() {
    name = "Unnamed node collection";
    description = "No description provided";
    nodes = new Node[] {};
    connections = new NodeConnection[] {};
  }

  public Node getNodeById(String id) throws IllegalArgumentException {
    for (Node node : nodes) {
      if (node.getId().equals(id)) return node;
    }
    throw new IllegalArgumentException("Invalid node ID");
  }

  public NodeSocket getSocketById(String id) throws IllegalArgumentException {
    for (Node node : nodes) {
      for (NodeSocket socket : node.getInputs()) if (socket.getId().equals(id)) return socket;

      for (NodeSocket socket : node.getOutputs()) if (socket.getId().equals(id)) return socket;
    }
    throw new IllegalArgumentException("Invalid socket ID");
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Node[] getNodes() {
    return nodes;
  }

  public void setNodes(Node[] nodes) {
    this.nodes = nodes;
  }

  public NodeConnection[] getConnections() {
    return connections;
  }

  public void setConnections(NodeConnection[] connections) {
    this.connections = connections;
  }
}
