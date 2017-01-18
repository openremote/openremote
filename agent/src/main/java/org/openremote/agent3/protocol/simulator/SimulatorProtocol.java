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

import elemental.json.Json;
import elemental.json.JsonNumber;
import elemental.json.JsonString;
import elemental.json.JsonValue;
import org.openremote.agent3.protocol.AbstractProtocol;
import org.openremote.agent3.protocol.simulator.element.*;
import org.openremote.model.AttributeRef;
import org.openremote.model.AttributeValueChange;
import org.openremote.model.asset.ThingAttribute;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import static org.openremote.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.asset.AssetAttributeMeta.RANGE_MAX;
import static org.openremote.model.asset.AssetAttributeMeta.RANGE_MIN;

public class SimulatorProtocol extends AbstractProtocol {

    private static final Logger LOG = Logger.getLogger(org.openremote.agent3.protocol.simulator.SimulatorProtocol.class.getName());

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":simulator";
    public static final String META_NAME_ELEMENT = PROTOCOL_NAME + ":element";

    static final protected Map<AttributeRef, SimulatorElement> elements = new HashMap<>();

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    protected void onAttributeAdded(ThingAttribute attribute) {
        JsonString elementType = attribute.firstMetaItemOrThrow(JsonString.class, META_NAME_ELEMENT);
        SimulatorElement element = createElement(elementType.asString(), attribute);
        JsonValue initialState = attribute.getValue();
        element.setState(initialState);
        LOG.fine("Putting element '" + element + "' for: " + attribute);
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
    protected void sendToActuator(AttributeValueChange attributeValueChange) {
        putState(attributeValueChange.getAttributeRef(), attributeValueChange.getValue(), false);
    }

    public void putState(String entityId, String attributeName, JsonValue value) {
        putState(new AttributeRef(entityId, attributeName), value, true);
    }

    public void putState(AttributeRef attributeRef, JsonValue value) {
        putState(attributeRef, value, true);
    }

    /**
     * @param isSensorUpdate <code>true</code> if an {@link AttributeValueChange} message should be produced
     */
    public void putState(AttributeRef attributeRef, JsonValue value, boolean isSensorUpdate) {
        synchronized (elements) {
            LOG.fine("Put state '" + attributeRef + "': " + (value != null ? value.asString() : "null"));

            SimulatorElement element = elements.get(attributeRef);
            if (element == null)
                throw new IllegalArgumentException("No simulated element for: " + attributeRef);

            element.setState(value);

            if (isSensorUpdate) {
                LOG.fine("Propagating state change as sensor update: " + element);
                onSensorUpdate(new AttributeValueChange(attributeRef, value));
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

    protected SimulatorElement createElement(String elementType, ThingAttribute attribute) {
        switch (elementType.toLowerCase(Locale.ROOT)) {
            case "switch":
                return new SwitchSimulatorElement();
            case "integer":
                return new IntegerSimulatorElement();
            case "decimal":
                return new DecimalSimulatorElement();
            case "range":
                JsonNumber min = attribute.firstMetaItem(JsonNumber.class, RANGE_MIN.getName(), Json.create(0));
                JsonNumber max = attribute.firstMetaItem(JsonNumber.class, RANGE_MAX.getName(), Json.create(100));
                return new IntegerSimulatorElement(min.asNumber(), max.asNumber());
            case "color":
                return new ColorSimulatorElement();
            default:
                throw new UnsupportedOperationException("Can't simulate element '" + elementType + "': " + attribute);
        }
    }
}
