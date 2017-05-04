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
package org.openremote.agent3.protocol.macro;

import org.openremote.model.attribute.AttributeState;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.util.Pair;
import org.openremote.model.value.ObjectValue;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.Objects;
import java.util.Optional;

import static org.openremote.agent3.protocol.macro.MacroConfiguration.META_MACRO_ACTION;

/**
 * A desired {@link AttributeState} and a delay in milli seconds before that state
 * is applied when the macro executes.
 */
public class MacroAction {

    protected AttributeState attributeState;
    protected int delayMilliseconds;

    public MacroAction(AttributeState attributeState) {
        Objects.requireNonNull(attributeState);
        this.attributeState = attributeState;
    }

    public MacroAction(AttributeState attributeState, int delayMilliseconds) {
        this(attributeState);
        setDelayMilliseconds(delayMilliseconds);
    }

    public AttributeState getAttributeState() {
        return attributeState;
    }

    public void setAttributeState(AttributeState attributeState) {
        this.attributeState = Objects.requireNonNull(attributeState);
    }

    public int getDelayMilliseconds() {
        return delayMilliseconds;
    }

    public void setDelayMilliseconds(int delayMilliseconds) {
        this.delayMilliseconds = Math.max(0, delayMilliseconds);
    }

    public ObjectValue toObjectValue() {
        ObjectValue objectValue = Values.createObject();
        objectValue.put("attributeState", attributeState.toObjectValue());
        objectValue.put("delay", Values.create(delayMilliseconds));
        return objectValue;
    }

    public MetaItem toMetaItem() {
        return new MetaItem(META_MACRO_ACTION, toObjectValue());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "attributeState='" + attributeState + '\'' +
            ", delay='" + delayMilliseconds + '\'' +
            '}';
    }

    public static Optional<MacroAction> fromValue(Value value) {
        return Values.getObject(value)
            .map(objectValue -> new Pair<>(
                    AttributeState.fromValue(objectValue.get("attributeState").get()),
                    objectValue.getNumber("delay").orElse(0d)
                )
            ).filter(pair -> pair.key.isPresent())
            .map(pair -> new MacroAction(pair.key.get(), pair.value.intValue()));
    }
}
