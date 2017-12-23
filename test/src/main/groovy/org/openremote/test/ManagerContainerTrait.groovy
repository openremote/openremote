package org.openremote.test

import com.google.common.collect.Lists
import org.drools.core.time.impl.PseudoClockScheduler
import org.openremote.agent.protocol.Protocol
import org.openremote.container.Container
import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.message.MessageBrokerSetupService
import org.openremote.container.security.keycloak.KeycloakIdentityProvider
import org.openremote.container.timer.TimerService
import org.openremote.manager.server.agent.AgentService
import org.openremote.manager.server.apps.ConsoleAppService
import org.openremote.manager.server.asset.AssetAttributeLinkingService
import org.openremote.manager.server.asset.AssetProcessingService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.concurrent.ManagerExecutorService
import org.openremote.manager.server.datapoint.AssetDatapointService
import org.openremote.manager.server.event.ClientEventService
import org.openremote.manager.server.i18n.I18NService
import org.openremote.manager.server.map.MapService
import org.openremote.manager.server.notification.NotificationService
import org.openremote.manager.server.persistence.ManagerPersistenceService
import org.openremote.manager.server.rules.RulesEngine
import org.openremote.manager.server.rules.RulesService
import org.openremote.manager.server.rules.RulesetStorageService
import org.openremote.manager.server.security.ManagerIdentityService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.simulator.SimulatorService
import org.openremote.manager.server.web.ManagerWebService

import java.util.concurrent.TimeUnit

import static org.openremote.container.security.IdentityService.IDENTITY_NETWORK_HOST
import static org.openremote.container.security.IdentityService.IDENTITY_NETWORK_WEBSERVER_PORT
import static org.openremote.container.timer.TimerService.Clock.PSEUDO
import static org.openremote.container.timer.TimerService.TIMER_CLOCK_TYPE
import static org.openremote.container.web.WebService.WEBSERVER_LISTEN_PORT
import static org.openremote.manager.server.setup.SetupTasks.*

trait ManagerContainerTrait extends ContainerTrait {

    static Map<String, String> defaultConfig(int serverPort) {
        [
                (WEBSERVER_LISTEN_PORT)             : Integer.toString(serverPort),
                (IDENTITY_NETWORK_HOST)             : KeycloakIdentityProvider.KEYCLOAK_HOST_DEFAULT,
                (IDENTITY_NETWORK_WEBSERVER_PORT)   : Integer.toString(KeycloakIdentityProvider.KEYCLOAK_PORT_DEFAULT),
                (SETUP_IMPORT_DEMO_SCENES)          : "false",
                (SETUP_IMPORT_DEMO_RULES)           : "false"
        ]
    }

    static Iterable<ContainerService> defaultServices(Iterable<ContainerService> additionalServices) {
        [
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
                new MessageBrokerService(),
                *Lists.newArrayList(ServiceLoader.load(Protocol.class)),
                new AgentService(),
                new SimulatorService(),
                new MapService(),
                new NotificationService(),
                new ConsoleAppService(),
                new ManagerWebService(),
                *additionalServices

        ] as Iterable<ContainerService>
    }

    static Iterable<ContainerService> defaultServices(ContainerService... additionalServices) {
        defaultServices(Arrays.asList(additionalServices))
    }

    static Container startContainerNoDemoAssets(Map<String, String> config, Iterable<ContainerService> services) {
        config << [(SETUP_IMPORT_DEMO_ASSETS): "false"]
        startContainer(config, services)
    }

    static Container startContainerWithDemoScenesAndRules(Map<String, String> config, Iterable<ContainerService> services) {
        config << [(SETUP_IMPORT_DEMO_SCENES): "true"]
        config << [(SETUP_IMPORT_DEMO_RULES): "true"]
        startContainer(config, services)
    }

    static Container startContainerNoDemoImport(Map<String, String> config, Iterable<ContainerService> services) {
        config << [(SETUP_IMPORT_DEMO_ASSETS): "false"]
        config << [(SETUP_IMPORT_DEMO_USERS): "false"]
        startContainer(config, services)
    }

    /**
     * Use pseudo clock instead of wall clock.
     */
    static Container startContainerWithPseudoClock(Map<String, String> config, Iterable<ContainerService> services) {
        startContainer(config << [(TIMER_CLOCK_TYPE): PSEUDO.name()], services)
    }

    static boolean noEventProcessedIn(AssetProcessingService assetProcessingService, int milliseconds) {
        return (assetProcessingService.lastProcessedEventTimestamp > 0
                && assetProcessingService.lastProcessedEventTimestamp + milliseconds < System.currentTimeMillis())
    }

    /**
     * Execute pseudo clock operations in Rules engine.
     */
    static void withClockOf(RulesEngine engine, Closure<PseudoClockScheduler> clockConsumer) {
        clockConsumer.call(engine.sessionClock as PseudoClockScheduler)
    }

    static long getClockTimeOf(RulesEngine engine) {
        (engine.sessionClock as PseudoClockScheduler).currentTime
    }

    /**
     * Execute pseudo clock operations in Container.
     */
    static void withClockOf(Container container, Closure<TimerService.Clock> clockConsumer) {
        clockConsumer.call(container.getService(TimerService.class).getClock())
    }

    static long getClockTimeOf(Container container) {
        container.getService(TimerService.class).getClock().getCurrentTimeMillis()
    }

    static void advancePseudoClocks(long amount, TimeUnit unit, Container container, RulesEngine[] engine) {
        withClockOf(container) { it.advanceTime(amount, unit) }
        engine.each { withClockOf(it) { it.advanceTime(amount, unit) } }
    }

    static void setPseudoClocksToRealTime(Container container, RulesEngine[] engine) {
        withClockOf(container) {
            it.advanceTime(System.currentTimeMillis() - it.currentTimeMillis, TimeUnit.MILLISECONDS)
        }
        engine.each {
            withClockOf(it) { it.advanceTime(System.currentTimeMillis() - it.currentTime, TimeUnit.MILLISECONDS) }
        }
    }

}
