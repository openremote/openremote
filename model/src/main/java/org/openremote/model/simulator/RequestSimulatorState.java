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
package org.openremote.model.simulator;

import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Arrays;

/**
 * A client sends this event to the server to request {@link SimulatorState}, expecting
 * the server to answer "soon". If the server decides that the client doesn't have the
 * right permissions, or if anything else is not in order (e.g. the protocol configuration
 * doesn't exist), the server might not react at all.
 * <p>
 * If no attribute names and only an asset identifier are provided, all attributes
 * of the asset, accessible by the client, will be read/returned.
 */
public class RequestSimulatorState extends SharedEvent {

    protected AttributeRef[] configurations;

    protected RequestSimulatorState() {
    }

    public RequestSimulatorState(AttributeRef... configurations) {
        this.configurations = configurations;
    }

    public AttributeRef[] getConfigurations() {
        return configurations;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "simulatorConfigurations=" + Arrays.toString(configurations) +
            '}';
    }
}
