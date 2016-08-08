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
package org.openremote.agent.controller2;

import org.jboss.marshalling.Pair;
import org.openremote.agent.Connector;
import org.openremote.agent.ConnectorFactory;
import org.openremote.manager.shared.agent.Agent;

public class Controller2ConnectorFactory implements ConnectorFactory {
    @Override
    public Connector createConnector(Agent agent) {
        return new Controller2Connector();
    }

    @Override
    public Pair<Boolean, String> validateAgent(Agent agent) {
        return null;
    }

    @Override
    public void disposeConnector(Connector connector) {
        // No cleanup needed just let it be destroyed
    }

    @Override
    public String getType() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }
}
