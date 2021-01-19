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

package org.openremote.model.flow.catalog.console;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.flow.Slot;
import org.openremote.model.flow.catalog.WidgetNodeDescriptor;

import java.util.List;
import java.util.function.Supplier;

public class TextLabelNodeDescriptor extends WidgetNodeDescriptor {

    public static final String TYPE = "urn:openremote:widget:textlabel";
    public static final String TYPE_LABEL = "Text Label";

    public static final String COMPONENT = "or-console-widget-textlabel";

    public static final ObjectNode TEXT_LABEL_INITIAL_PROPERTIES = WIDGET_INITIAL_PROPERTIES.deepCopy()
        .put(PROPERTY_COMPONENT, COMPONENT)
        .put("emptyValue", "(EMPTY LABEL)")
        .put("fontSizePixels", 15)
        .put("textColor", "#ddd");

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getTypeLabel() {
        return TYPE_LABEL;
    }

    @Override
    public void addSlots(List<Slot> slots, Supplier<String> idGenerator) {
        slots.add(new Slot("Text", idGenerator.get(), Slot.TYPE_SINK, "text"));
        slots.add(new Slot("Text", idGenerator.get(), Slot.TYPE_SOURCE, "text"));
        slots.add(new Slot("Color", idGenerator.get(), Slot.TYPE_SINK, "textColor"));
        slots.add(new Slot("Font Size", idGenerator.get(), Slot.TYPE_SINK, "fontSizePixels"));
        super.addSlots(slots, idGenerator);
    }

    @Override
    public void addEditorComponents(List<String> editorComponents) {
        super.addEditorComponents(editorComponents);
        editorComponents.add("or-node-editor-textlabel");
    }

    @Override
    protected ObjectNode getInitialProperties() {
        return TEXT_LABEL_INITIAL_PROPERTIES;
    }

    @Override
    protected void addPersistentPropertyPaths(List<String> propertyPaths) {
        super.addPersistentPropertyPaths(propertyPaths);
        propertyPaths.add("emptyValue");
        propertyPaths.add("fontSizePixels");
        propertyPaths.add("textColor");
    }
}
