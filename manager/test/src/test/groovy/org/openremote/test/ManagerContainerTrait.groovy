package org.openremote.test

import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.server.DemoDataService
import org.openremote.manager.server.agent.AgentService
import org.openremote.manager.server.agent.ConnectorService
import org.openremote.manager.server.asset.AssetService
import org.openremote.manager.server.event.EventService
import org.openremote.manager.server.i18n.I18NService
import org.openremote.manager.server.map.MapService
import org.openremote.manager.server.security.ManagerIdentityService
import org.openremote.manager.server.web.ManagerWebService

import java.util.stream.Stream

trait ManagerContainerTrait extends ContainerTrait {

    static Stream<ContainerService> defaultServices(ContainerService... additionalServices) {
        Stream.concat(
                Arrays.stream(additionalServices),
                Stream.of(
                        new I18NService(),
                        new ManagerWebService(),
                        new ManagerIdentityService(),
                        new MessageBrokerService(),
                        new PersistenceService(),
                        new EventService(),
                        new ConnectorService(),
                        new AgentService(),
                        new AssetService(),
                        new MapService(),
                        new DemoDataService()
                )
        )
    }
}
