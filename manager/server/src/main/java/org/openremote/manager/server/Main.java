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

import org.openremote.agent3.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.persistence.PersistenceService;
import org.openremote.manager.server.agent.AgentService;
import org.openremote.manager.server.asset.AssetService;
import org.openremote.manager.server.event.EventService;
import org.openremote.manager.server.i18n.I18NService;
import org.openremote.manager.server.map.MapService;
import org.openremote.manager.server.security.ManagerIdentityService;
import org.openremote.manager.server.setup.SetupService;
import org.openremote.manager.server.web.ManagerWebService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

public class Main {

    public static void main(String[] args) throws Exception {
        List<ContainerService> services = new ArrayList<ContainerService>() {
            {
                addAll(Arrays.asList(
                    new I18NService(),
                    new MessageBrokerSetupService(),
                    new ManagerIdentityService(),
                    new PersistenceService(),
                    new EventService(),
                    new AssetService(),
                    new AgentService()
                ));
                ServiceLoader.load(Protocol.class).forEach(this::add);
                addAll(Arrays.asList(
                    new MapService(),
                    new MessageBrokerService(),
                    new SetupService(),
                    new ManagerWebService()
                ));
            }
        };
        new Container(services).startBackground();
    }
}
