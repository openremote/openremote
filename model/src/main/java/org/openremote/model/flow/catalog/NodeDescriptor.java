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

import static org.openremote.model.util.ValueUtil.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openremote.model.flow.Node;
import org.openremote.model.flow.NodeColor;
import org.openremote.model.flow.Slot;

public abstract class NodeDescriptor {

  public abstract String getType();

  public abstract String getTypeLabel();

  public CatalogCategory getCatalogCategory() {
    return CatalogCategory.PROCESSORS;
  }

  public NodeColor getColor() {
    return NodeColor.DEFAULT;
  }

  public Node initialize(Node node, Supplier<String> idGenerator) {

    List<Slot> slots = new ArrayList<>();
    addSlots(slots, idGenerator);
    node.setSlots(slots.toArray(new Slot[0]));

    node.getEditorSettings().setTypeLabel(getTypeLabel());
    node.getEditorSettings().setNodeColor(getColor());

    List<String> editorComponents = new ArrayList<>();
    addEditorComponents(editorComponents);
    node.getEditorSettings().setComponents(editorComponents.toArray(new String[0]));

    ObjectNode initialProperties = getInitialProperties();
    try {
      if (initialProperties != null) {
        node.setProperties(JSON.writeValueAsString(initialProperties));
      } else {
        ObjectNode properties = JSON.createObjectNode();
        configureInitialProperties(properties);
        if (!properties.isEmpty()) {
          node.setProperties(JSON.writeValueAsString(properties));
        }
      }
    } catch (Exception ex) {
      throw new RuntimeException("Error writing initial properties of: " + getType(), ex);
    }

    List<String> persistentPaths = new ArrayList<>();
    addPersistentPropertyPaths(persistentPaths);
    if (persistentPaths.size() > 0) {
      node.setPersistentPropertyPaths(persistentPaths.toArray(new String[persistentPaths.size()]));
    }

    return node;
  }

  public List<String> getPersistentPropertyPaths() {
    List<String> persistentPaths = new ArrayList<>();
    addPersistentPropertyPaths(persistentPaths);
    return persistentPaths;
  }

  public void addSlots(List<Slot> slots, Supplier<String> idGenerator) {
    // Subclass
  }

  public void addEditorComponents(List<String> editorComponents) {
    // Subclass
  }

  protected void configureInitialProperties(ObjectNode properties) {
    // Subclass
  }

  protected void addPersistentPropertyPaths(List<String> propertyPaths) {
    // Subclass
  }

  protected ObjectNode getInitialProperties() {
    return null;
  }
}
