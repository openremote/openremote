/*
 * Copyright 2015, OpenRemote Inc.
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
package org.openremote.model.flow.catalog;

import org.openremote.model.flow.Node;
import org.openremote.model.flow.NodeColor;
import org.openremote.model.flow.Slot;

import java.util.List;
import java.util.function.Supplier;

public class AssetNodeDescriptor extends NodeDescriptor {

    public static final String TYPE = "urn:openremote:flow:node:asset";
    public static final String TYPE_LABEL = "Asset";

    @Override
    public CatalogCategory getCatalogCategory() {
        return null;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTypeLabel() {
        return TYPE_LABEL;
    }

    @Override
    public NodeColor getColor() {
        return NodeColor.ASSET;
    }

    @Override
    public Node initialize(Node node, Supplier<String> idGenerator) {
        node = super.initialize(node, idGenerator);
        node.setClientAccess(true);
        return node;
    }

    @Override
    public void addSlots(List<Slot> slots, Supplier<String> idGenerator) {
        super.addSlots(slots, idGenerator);
        slots.add(new Slot(idGenerator.get(), Slot.TYPE_SOURCE));
    }

    @Override
    protected void addPersistentPropertyPaths(List<String> propertyPaths) {
        super.addPersistentPropertyPaths(propertyPaths);
    }

    @Override
    public void addEditorComponents(List<String> editorComponents) {
        super.addEditorComponents(editorComponents);
        editorComponents.add("or-node-editor-asset");
    }
}