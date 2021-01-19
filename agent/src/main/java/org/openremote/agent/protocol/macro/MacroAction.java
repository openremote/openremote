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
package org.openremote.agent.protocol.macro;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.attribute.AttributeState;

import java.io.Serializable;
import java.util.Objects;

/**
 * A desired {@link AttributeState} and a delay in milli seconds before that state
 * is applied when the macro executes.
 */
public class MacroAction implements Serializable {

    protected AttributeState attributeState;
    @JsonProperty("delay")
    protected int delayMilliseconds;

    public MacroAction(AttributeState attributeState) {
        Objects.requireNonNull(attributeState);
        this.attributeState = attributeState;
    }

    @JsonCreator
    public MacroAction(@JsonProperty("attributeState") AttributeState attributeState, @JsonProperty("delay") int delayMilliseconds) {
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "attributeState='" + attributeState + '\'' +
            ", delay='" + delayMilliseconds + '\'' +
            '}';
    }
}
