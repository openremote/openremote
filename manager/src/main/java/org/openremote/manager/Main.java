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
package org.openremote.manager;

import org.openremote.agent.protocol.Protocol;
import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.container.message.MessageBrokerService;
import org.openremote.container.message.MessageBrokerSetupService;
import org.openremote.container.timer.TimerService;
import org.openremote.manager.agent.AgentService;
import org.openremote.manager.apps.ConsoleAppService;
import org.openremote.manager.asset.AssetAttributeLinkingService;
import org.openremote.manager.asset.AssetProcessingService;
import org.openremote.manager.asset.AssetStorageService;
import org.openremote.manager.concurrent.ManagerExecutorService;
import org.openremote.manager.datapoint.AssetDatapointService;
import org.openremote.manager.event.ClientEventService;
import org.openremote.manager.i18n.I18NService;
import org.openremote.manager.map.MapService;
import org.openremote.manager.notification.NotificationService;
import org.openremote.manager.persistence.ManagerPersistenceService;
import org.openremote.manager.rules.RulesService;
import org.openremote.manager.rules.RulesetStorageService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.setup.SetupService;
import org.openremote.manager.simulator.SimulatorService;
import org.openremote.manager.web.ManagerWebService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ServiceLoader;

public class Main {

    public static void main(String[] args) throws Exception {
        List<ContainerService> services = new ArrayList<ContainerService>() {
            {
                addAll(Arrays.asList(
                    new TimerService(),
                    new ManagerExecutorService(),
                    new I18NService(),
                    new ManagerPersistenceService(),
                    new MessageBrokerSetupService(),
                    new ManagerIdentityService(),
                    new SetupService(),
                    new ClientEventService(),
                    new RulesetStorageService(),
                    new RulesService(),
                    new AssetStorageService(),
                    new AssetDatapointService(),
                    new AssetAttributeLinkingService(),
                    new AssetProcessingService(),
                    new MessageBrokerService()
                ));
                ServiceLoader.load(Protocol.class).forEach(this::add);
                addAll(Arrays.asList(
                    new AgentService(),
                    new SimulatorService(),
                    new MapService(),
                    new NotificationService(),
                    new ConsoleAppService(),
                    new ManagerWebService()
                ));
            }
        };
        new Container(services).startBackground();
    }
}
