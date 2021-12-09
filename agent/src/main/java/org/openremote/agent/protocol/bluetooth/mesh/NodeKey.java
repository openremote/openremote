/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.bluetooth.mesh;

public class NodeKey {
    private final int index;
    private boolean updated;

    /**
     * Constructs a NodeKey
     *
     * @param index Index of the key
     */
    public NodeKey(final int index) {
        this(index, false);
    }

    /**
     * Constructs a NodeKey
     *
     * @param index   Index of the key
     * @param updated If the key has been updated
     */
    public NodeKey(final int index, final boolean updated) {
        this.index = index;
        this.updated = updated;
    }

    /**
     * Returns the index of the added key
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns true if the key has been updated
     */
    public boolean isUpdated() {
        return updated;
    }

    /**
     * Sets the updated state of the network/application key
     *
     * @param updated true if updated and false otherwise
     */
    public void setUpdated(final boolean updated) {
        this.updated = updated;
    }
}