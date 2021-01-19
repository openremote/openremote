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
package org.openremote.model.flow.catalog.virtual;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.openremote.model.flow.Node;
import org.openremote.model.flow.NodeColor;
import org.openremote.model.flow.catalog.CatalogCategory;
import org.openremote.model.flow.catalog.WidgetNodeDescriptor;

import java.util.function.Supplier;

public class SubflowNodeDescriptor extends WidgetNodeDescriptor {

    public static final String WIDGET_COMPONENT = "or-console-widget-composite";

    @Override
    public NodeColor getColor() {
        return NodeColor.VIRTUAL;
    }

    @Override
    public CatalogCategory getCatalogCategory() {
        return null;
    }

    @Override
    public String getType() {
        return Node.TYPE_SUBFLOW;
    }

    @Override
    public String getTypeLabel() {
        return Node.TYPE_SUBFLOW_LABEL;
    }

    @Override
    public Node initialize(Node node, Supplier<String> idGenerator) {
        node = super.initialize(node, idGenerator);
        node.setClientAccess(true);
        return node;
    }

    @Override
    protected ObjectNode getInitialProperties() {
        return WIDGET_INITIAL_PROPERTIES.deepCopy()
            .put(PROPERTY_COMPONENT, WIDGET_COMPONENT);
    }
}
