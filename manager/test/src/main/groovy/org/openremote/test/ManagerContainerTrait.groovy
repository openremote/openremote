package org.openremote.test

import com.google.common.collect.Lists
import org.drools.core.time.impl.PseudoClockScheduler
import org.kie.api.runtime.conf.ClockTypeOption
import org.openremote.agent3.protocol.Protocol
import org.openremote.container.Container
import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.message.MessageBrokerSetupService
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.server.agent.AgentService
import org.openremote.manager.server.apps.ConsoleAppService
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.datapoint.AssetDatapointService
import org.openremote.manager.server.event.EventService
import org.openremote.manager.server.i18n.I18NService
import org.openremote.manager.server.map.MapService
import org.openremote.manager.server.notification.NotificationService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.security.ManagerIdentityService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.concurrent.ManagerExecutorService
import org.openremote.manager.server.web.ManagerWebService

import static org.openremote.manager.server.setup.builtin.BuiltinSetupTasks.SETUP_IMPORT_DEMO_RULES

trait ManagerContainerTrait extends ContainerTrait {

    static Iterable<ContainerService> defaultServices(Iterable<ContainerService> additionalServices) {
        [
                new ManagerExecutorService(),
                new I18NService(),
                new PersistenceService(),
                new MessageBrokerSetupService(),
                new ManagerIdentityService(),
                new SetupService(),
                new EventService(),
                new RulesetStorageService(),
                new RulesService(),
                new AssetStorageService(),
                new AssetDatapointService(),
                new AssetProcessingService(),
                *Lists.newArrayList(ServiceLoader.load(Protocol.class)),
                new AgentService(),
                new MapService(),
                new NotificationService(),
                new MessageBrokerService(),
                new ConsoleAppService(),
                new ManagerWebService(),
                *additionalServices

        ] as Iterable<ContainerService>
    }

    static Iterable<ContainerService> defaultServices(ContainerService... additionalServices) {
        defaultServices(Arrays.asList(additionalServices))
    }

    /**
     * Faster startup if you don't need to test rules.
     */
    static Container startContainerWithoutDemoRules(Map<String, String> config, Iterable<ContainerService> services) {
        startContainer(config << [(SETUP_IMPORT_DEMO_RULES): "false"], services)
    }

    static void enablePseudoClock() {
        RulesEngine.DefaultClockType = ClockTypeOption.get("pseudo")
    }

    static void disablePseudoClock() {
        RulesEngine.DefaultClockType = null
    }

    /**
     * Execute pseudo clock operations in Rules engine.
     */
    static void withClockOf(RulesEngine engine, Closure<PseudoClockScheduler> clockConsumer) {
        clockConsumer.call(engine.sessionClock as PseudoClockScheduler)
    }

}
