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
package org.openremote.model;

import java.util.LinkedList;

/**
 * A sequence of <code>[{@link AttributeState}, Seconds]</code> items, each lasting for the given duration.
 * <p>
 * Optionally, repeat the states when the last item has expired.
 */
public class AttributeStates extends LinkedList<AttributeStates.Item> {

    public class Item {
        final AttributeState attributeState;
        final int delayNextSeconds;

        public Item(AttributeState attributeState, int delayNextSeconds) {
            this.attributeState = attributeState;
            this.delayNextSeconds = delayNextSeconds;
        }

        public AttributeState getAttributeState() {
            return attributeState;
        }

        public int getDelayNextSeconds() {
            return delayNextSeconds;
        }
    }

    final protected boolean repeat;

    public AttributeStates(boolean repeat) {
        this.repeat = repeat;
    }

    public boolean isRepeat() {
        return repeat;
    }
}
