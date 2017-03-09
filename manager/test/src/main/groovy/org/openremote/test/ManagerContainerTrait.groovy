package org.openremote.test

import com.google.common.collect.Lists
import org.openremote.agent3.protocol.Protocol
import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.message.MessageBrokerSetupService
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.server.attribute.AttributeStateRouterService
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.agent.AgentService
import org.openremote.manager.server.asset.AssetService
import org.openremote.manager.server.asset.datapoint.DatapointService
import org.openremote.manager.server.event.EventService
import org.openremote.manager.server.i18n.I18NService
import org.openremote.manager.server.map.MapService
import org.openremote.manager.server.security.ManagerIdentityService
import org.openremote.manager.server.web.ManagerWebService

trait ManagerContainerTrait extends ContainerTrait {

    static Iterable<ContainerService> defaultServices(Iterable<ContainerService> additionalServices) {
        [
                new I18NService(),
                new MessageBrokerSetupService(),
                new ManagerIdentityService(),
                new PersistenceService(),
                new SetupService(),
                new EventService(),
                new AssetService(),
                new DatapointService(),
                new AgentService(),
                new RulesService(),
                new AttributeStateRouterService(),
                *Lists.newArrayList(ServiceLoader.load(Protocol.class)),
                new MapService(),
                new MessageBrokerService(),
                new ManagerWebService(),
                *additionalServices

        ] as Iterable<ContainerService>
    }

    static Iterable<ContainerService> defaultServices(ContainerService... additionalServices) {
        defaultServices(Arrays.asList(additionalServices))
    }
}
