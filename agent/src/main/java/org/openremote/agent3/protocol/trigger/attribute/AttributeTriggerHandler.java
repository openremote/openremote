/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent3.protocol.trigger.attribute;

import elemental.json.JsonValue;
import org.openremote.agent3.protocol.trigger.AbstractTriggerHandler;
import org.openremote.model.AttributeEvent;
import org.openremote.model.AttributeRef;

public class AttributeTriggerHandler extends AbstractTriggerHandler {
    public static final String ATTRIBUTE_TRIGGER_HANDLER_NAME = "Attribute Event Trigger Handler";

    @Override
    protected String getName() {
        return ATTRIBUTE_TRIGGER_HANDLER_NAME;
    }

    @Override
    protected boolean isValidValue(JsonValue triggerValue) {
        return false;
    }

    @Override
    protected void registerTrigger(AttributeRef triggerRef, JsonValue value, boolean isEnabled) {

    }

    @Override
    protected void updateTrigger(AttributeRef triggerRef, JsonValue value, boolean isEnabled) {

    }

    @Override
    protected void unregisterTrigger(AttributeRef triggerRef) {

    }

    @Override
    protected void registerAttribute(AttributeRef attributeRef, AttributeRef triggerRef, String propertyName) {

    }

    @Override
    protected void updateAttribute(AttributeRef attributeRef, AttributeRef triggerRef, String propertyName) {

    }

    @Override
    protected void unregisterAttribute(AttributeRef attributeRef, AttributeRef triggerRef) {

    }

    @Override
    protected void processAttributeWrite(AttributeRef attributeRef, AttributeRef triggerRef, String propertyName, AttributeEvent event) {
        // This trigger handler doesn't support updates for now
    }
}
