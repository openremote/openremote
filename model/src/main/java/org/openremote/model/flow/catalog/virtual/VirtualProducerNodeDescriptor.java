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

import org.openremote.model.flow.Node;
import org.openremote.model.flow.Slot;

import java.util.List;
import java.util.function.Supplier;

public class VirtualProducerNodeDescriptor extends VirtualNodeDescriptor {

    @Override
    public String getType() {
        return Node.TYPE_PRODUCER;
    }

    @Override
    public String getTypeLabel() {
        return Node.TYPE_PRODUCER_LABEL;
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
        slots.add(new Slot(idGenerator.get(), Slot.TYPE_SINK));
        slots.add(new Slot(idGenerator.get(), Slot.TYPE_SOURCE, false));
    }
}
