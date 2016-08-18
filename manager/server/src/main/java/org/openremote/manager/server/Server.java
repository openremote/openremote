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
package org.openremote.manager.server;

import org.openremote.container.Container;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.manager.server.agent.AgentService;
import org.openremote.manager.server.agent.ConnectorService;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.server.i18n.I18NService;
import org.openremote.manager.server.map.MapService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.web.ManagerWebService;

public class Server {

    public static void main(String[] args) throws Exception {
        new Container(
            new I18NService(),
            new ManagerWebService(),
            new ManagerIdentityService(),
            new MessageBrokerService(),
            new EventService(),
            new ConnectorService(),
            new AgentService(),
            new MapService(),
            new SampleDataService()
        ).startBackground();
    }
}
