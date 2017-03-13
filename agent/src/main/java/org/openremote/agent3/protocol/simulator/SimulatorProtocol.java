/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.agent3.protocol.simulator;

import elemental.json.JsonValue;
import org.openremote.agent3.protocol.AbstractProtocol;
import org.openremote.agent3.protocol.simulator.element.*;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeState;
import org.openremote.model.AttributeEvent;
import org.openremote.model.MetaItem;
import org.openremote.model.asset.ThingAttribute;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.asset.AssetMeta.RANGE_MAX;
import static org.openremote.model.asset.AssetMeta.RANGE_MIN;

public class SimulatorProtocol extends AbstractProtocol {

    private static final Logger LOG = Logger.getLogger(org.openremote.agent3.protocol.simulator.SimulatorProtocol.class.getName());

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":simulator";

    /**
     * Required meta item, the simulator element that should be used, see subclasses of {@link SimulatorElement}.
     */
    public static final String SIMULATOR_ELEMENT = PROTOCOL_NAME + ":element";

    /**
     * Optional (defaults to false) meta item, whether actuator writes should immediately be reflected as sensor reads.
     */
    public static final String SIMULATOR_REFLECT_ACTUATOR_WRITES = PROTOCOL_NAME + ":reflectActuatorWrites";

    static final protected Map<AttributeRef, SimulatorElement> elements = new HashMap<>();

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    protected void onAttributeAdded(ThingAttribute attribute) {
        String elementType = attribute.firstMetaItemOrThrow(SIMULATOR_ELEMENT).getValueAsString();

        boolean reflectActuatorWrites =
            attribute.hasMetaItem(SIMULATOR_REFLECT_ACTUATOR_WRITES)
                && attribute.firstMetaItem(SIMULATOR_REFLECT_ACTUATOR_WRITES).isValueTrue();

        SimulatorElement element = createElement(elementType, reflectActuatorWrites, attribute);
        try {
            element.setState(attribute.getValue());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Error setting initial state of: " + attribute, ex);
        }

        LOG.info("Putting element '" + element + "' for: " + attribute);
        elements.put(attribute.getAttributeRef(), element);
    }

    @Override
    protected void onAttributeUpdated(ThingAttribute attribute) {
        onAttributeAdded(attribute);
    }

    @Override
    protected void onAttributeRemoved(ThingAttribute attribute) {
        elements.remove(attribute.getAttributeRef());
    }

    @Override
    protected void sendToActuator(AttributeEvent event) {
        putState(event.getAttributeState(), false);
    }

    /**
     * Call this to simulate a sensor read.
     */
    public void putState(String entityId, String attributeName, JsonValue value) {
        putState(new AttributeState(new AttributeRef(entityId, attributeName), value), true);
    }

    /**
     * Call this to simulate a sensor read.
     */
    public void putState(AttributeRef attributeRef, JsonValue value) {
        putState(new AttributeState(attributeRef, value), true);
    }

    /**
     * Call this to simulate a sensor read or actuator write.
     *
     * @param simulateSensorRead <code>true</code> if an {@link AttributeEvent} sensor update should be produced
     */
    public void putState(AttributeState attributeState, boolean simulateSensorRead) {
        synchronized (elements) {
            LOG.info("Put simulator state: " + attributeState);

            AttributeRef attributeRef = attributeState.getAttributeRef();
            SimulatorElement element = elements.get(attributeRef);
            if (element == null)
                throw new IllegalArgumentException("No simulated element for: " + attributeRef);

            element.setState(attributeState.getValue());

            if (simulateSensorRead) {
                LOG.info("Propagating state change as sensor read: " + element);
                onSensorUpdate(new AttributeEvent(attributeState));
            } else if (element.isReflectActuatorWrites()) {
                LOG.info("Reflecting actuator write as sensor read: " + element);
                onSensorUpdate(new AttributeEvent(attributeState));
            }
        }
    }

    public JsonValue getState(String entityId, String attributeName) {
        return getState(new AttributeRef(entityId, attributeName));
    }

    public JsonValue getState(AttributeRef attributeRef) {
        synchronized (elements) {
            SimulatorElement element = elements.get(attributeRef);
            if (element == null)
                throw new IllegalArgumentException("No simulated element for: " + attributeRef);
            return element.getState();
        }
    }

    protected SimulatorElement createElement(String elementType, boolean reflectActuatorWrites, ThingAttribute attribute) {
        switch (elementType.toLowerCase(Locale.ROOT)) {
            case SwitchSimulatorElement.ELEMENT_NAME:
                return new SwitchSimulatorElement(reflectActuatorWrites);
            case IntegerSimulatorElement.ELEMENT_NAME_INTEGER:
                return new IntegerSimulatorElement(reflectActuatorWrites);
            case DecimalSimulatorElement.ELEMENT_NAME:
                return new DecimalSimulatorElement(reflectActuatorWrites);
            case IntegerSimulatorElement.ELEMENT_NAME_RANGE:
                MetaItem minItem = attribute.firstMetaItem(RANGE_MIN);
                MetaItem maxItem = attribute.firstMetaItem(RANGE_MAX);
                double min = minItem != null ? minItem.getValueAsInteger() : 0;
                double max = maxItem != null ? maxItem.getValueAsInteger() : 100;
                return new IntegerSimulatorElement(reflectActuatorWrites, min, max);
            case ColorSimulatorElement.ELEMENT_NAME:
                return new ColorSimulatorElement(reflectActuatorWrites);
            default:
                throw new UnsupportedOperationException("Can't simulate element '" + elementType + "': " + attribute);
        }
    }
}
