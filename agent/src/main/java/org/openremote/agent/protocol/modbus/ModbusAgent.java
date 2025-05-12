/*
 * Copyright 2025, OpenRemote Inc.
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
package org.openremote.agent.protocol.modbus;

import jakarta.persistence.Entity;
import org.openremote.model.asset.agent.Agent;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueConstraint;
import org.openremote.model.value.ValueType;

@Entity
public abstract class ModbusAgent<T extends ModbusAgent<T, U>, U extends AbstractModbusProtocol<U, T>> extends Agent<T, U, ModbusAgentLink> {

    public static final AttributeDescriptor<Integer> UNIT_ID = new AttributeDescriptor<>("unitId", ValueType.INTEGER, new MetaItem<>(MetaItemType.CONSTRAINTS, ValueConstraint.constraints(new ValueConstraint.Min(1), new ValueConstraint.Max(255))));

    // For Hydrators
    protected ModbusAgent() {}

    protected ModbusAgent(String name) {
        super(name);
    }

    public Integer getUnitId(){
        return getAttributes().getValue(UNIT_ID).get();
    }

    public void setUnitId(Integer unitId) {
        getAttributes().getOrCreate(UNIT_ID).setValue(unitId);
    }
}
