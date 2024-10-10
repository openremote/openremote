/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.agent.protocol.tradfri.device.event;

import org.openremote.agent.protocol.tradfri.device.Plug;
import org.openremote.agent.protocol.tradfri.device.PlugProperties;

/**
 * The class that represents a change event that occurred to an IKEA TRÅDFRI plug
 */
public class PlugChangeEvent extends PlugEvent {

    /**
     * The old properties of the plug (from before the event occurred)
     */
    private PlugProperties oldProperties;

    /**
     * The new properties of the plug (from after the event occurred)
     */
    private PlugProperties newProperties;

    /**
     * Construct the PlugChangeEvent class
     * @param plug The plug for which the event occurred
     * @param oldProperties The old properties of the plug (from before the event occurred)
     * @param newProperties The new properties of the plug (from after the event occurred)
     */
    public PlugChangeEvent(Plug plug, PlugProperties oldProperties, PlugProperties newProperties) {
        super(plug);
        this.oldProperties = oldProperties;
        this.newProperties = newProperties;
    }

    /**
     * Get the old properties of the plug (from before the event occurred)
     * @return The old properties of the plug
     */
    public PlugProperties getOldProperties(){
        return oldProperties;
    }

    /**
     * Get the new properties of the plug (from after the event occurred)
     * @return The new properties of the plug
     */
    public PlugProperties getNewProperties(){
        return newProperties;
    }

}
