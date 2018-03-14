package org.openremote.test

import com.google.common.collect.Lists
import org.apache.camel.spi.BrowsableEndpoint
import org.openremote.agent.protocol.Protocol
import org.openremote.container.Container
import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.message.MessageBrokerSetupService
import org.openremote.container.security.keycloak.KeycloakIdentityProvider
import org.openremote.container.timer.TimerService
import org.openremote.manager.agent.AgentService
import org.openremote.manager.apps.ConsoleAppService
import org.openremote.manager.asset.AssetAttributeLinkingService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.concurrent.ManagerExecutorService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.event.ClientEventService
import org.openremote.manager.i18n.I18NService
import org.openremote.manager.map.MapService
import org.openremote.manager.notification.NotificationService
import org.openremote.manager.persistence.ManagerPersistenceService
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.simulator.SimulatorService
import org.openremote.manager.web.ManagerWebService
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit

import static org.openremote.container.security.IdentityService.IDENTITY_NETWORK_HOST
import static org.openremote.container.security.IdentityService.IDENTITY_NETWORK_WEBSERVER_PORT
import static org.openremote.container.timer.TimerService.Clock.PSEUDO
import static org.openremote.container.timer.TimerService.TIMER_CLOCK_TYPE
import static org.openremote.container.web.WebService.WEBSERVER_LISTEN_PORT
import static org.openremote.manager.setup.SetupTasks.*

trait ManagerContainerTrait extends ContainerTrait {

    static Map<String, String> defaultConfig(int serverPort) {
        [
                (WEBSERVER_LISTEN_PORT)          : Integer.toString(serverPort),
                (IDENTITY_NETWORK_HOST)          : KeycloakIdentityProvider.KEYCLOAK_HOST_DEFAULT,
                (IDENTITY_NETWORK_WEBSERVER_PORT): Integer.toString(KeycloakIdentityProvider.KEYCLOAK_PORT_DEFAULT),
                (SETUP_IMPORT_DEMO_SCENES)       : "false",
                (SETUP_IMPORT_DEMO_RULES)        : "false"
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
     * Execute pseudo clock operations in Container.
     */
    static void withClockOf(Container container, Closure<TimerService.Clock> clockConsumer) {
        clockConsumer.call(container.getService(TimerService.class).getClock())
    }

    static long getClockTimeOf(Container container) {
        container.getService(TimerService.class).getClock().getCurrentTimeMillis()
    }

    static void advancePseudoClock(long amount, TimeUnit unit, Container container) {
        withClockOf(container) { it.advanceTime(amount, unit) }
    }

    static void noPendingExchangesOnMessageEndpoint(Container container, String... endpointName) {
        for (String name : endpointName) {
            def endpoint = container.getService(MessageBrokerSetupService.class).context.getEndpoint(name)
            if (!endpoint) {
                throw new IllegalArgumentException("Messaging endpoint not found: " + name)
            }
            new PollingConditions(initialDelay: 0.1, delay: 0.05).eventually {
                assert ((BrowsableEndpoint)endpoint).exchanges.size() == 0
            }
        }
    }
}
