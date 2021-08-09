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


import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.flow.Node;
import org.openremote.model.flow.Slot;
import org.openremote.model.util.ValueUtil;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

/**
 * TODO Deploy this as a JSON file + some HTML files to create a widget
 */
public abstract class WidgetNodeDescriptor extends ConsoleNodeDescriptor {

    public static final String PROPERTY_COMPONENT = "component";
    public static final String WIDGET_EDITOR_COMPONENT = "or-node-editor-widget";

    public static final ObjectNode WIDGET_INITIAL_PROPERTIES = ValueUtil.JSON.createObjectNode()
        .put("positionX", 25)
        .put("positionY", 25)
        .put("positionZ", 0)
        .put("opacity", 1.0);

    public static final String[] WIDGET_PERSISTENT_PROPERTY_PATHS = new String[]{
        PROPERTY_COMPONENT,
        "positionX",
        "positionY",
        "positionZ",
        "opacity"
    };

    @Override
    public CatalogCategory getCatalogCategory() {
        return CatalogCategory.WIDGETS;
    }

    @Override
    public Node initialize(Node node, Supplier<String> idGenerator) {
        node = super.initialize(node, idGenerator);
        node.setClientWidget(true);
        return node;
    }

    @Override
    public void addSlots(List<Slot> slots, Supplier<String> idGenerator) {
        super.addSlots(slots, idGenerator);
        slots.add(new Slot("Position X", idGenerator.get(), Slot.TYPE_SINK, "positionX"));
        slots.add(new Slot("Position Y", idGenerator.get(), Slot.TYPE_SINK, "positionY"));
        slots.add(new Slot("Position Z", idGenerator.get(), Slot.TYPE_SINK, "positionZ"));
        slots.add(new Slot("Opacity", idGenerator.get(), Slot.TYPE_SINK, "opacity"));
        slots.add(new Slot("Position X", idGenerator.get(), Slot.TYPE_SOURCE, "positionX"));
        slots.add(new Slot("Position Y", idGenerator.get(), Slot.TYPE_SOURCE, "positionY"));
        slots.add(new Slot("Position Z", idGenerator.get(), Slot.TYPE_SOURCE, "positionZ"));
        slots.add(new Slot("Opacity", idGenerator.get(), Slot.TYPE_SOURCE, "opacity"));
    }

    @Override
    public void addEditorComponents(List<String> editorComponents) {
        super.addEditorComponents(editorComponents);
        editorComponents.add(WIDGET_EDITOR_COMPONENT);
    }

    @Override
    protected ObjectNode getInitialProperties() {
        return WIDGET_INITIAL_PROPERTIES;
    }

    @Override
    protected void addPersistentPropertyPaths(List<String> propertyPaths) {
        super.addPersistentPropertyPaths(propertyPaths);
        propertyPaths.addAll(Arrays.asList(WIDGET_PERSISTENT_PROPERTY_PATHS));
    }
}
