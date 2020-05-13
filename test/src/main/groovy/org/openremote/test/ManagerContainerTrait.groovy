package org.openremote.test

import org.apache.camel.spi.BrowsableEndpoint
import org.openremote.container.Container
import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetProcessingService
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import static java.util.stream.StreamSupport.stream
import static org.openremote.container.security.IdentityService.IDENTITY_NETWORK_WEBSERVER_PORT
import static org.openremote.container.timer.TimerService.Clock.PSEUDO
import static org.openremote.container.timer.TimerService.TIMER_CLOCK_TYPE
import static org.openremote.container.web.WebService.WEBSERVER_LISTEN_PORT
import static org.openremote.manager.setup.SetupTasks.*

trait ManagerContainerTrait extends ContainerTrait {

    static Map<String, String> defaultConfig(int serverPort) {
        [
                (WEBSERVER_LISTEN_PORT)          : Integer.toString(serverPort),
                (IDENTITY_NETWORK_WEBSERVER_PORT): Integer.toString(serverPort),
                (SETUP_IMPORT_DEMO_SCENES)       : "false",
                (SETUP_IMPORT_DEMO_RULES)        : "false"
        ]
    }

    static Iterable<ContainerService> defaultServices(Iterable<ContainerService> additionalServices) {
        [
                *stream(ServiceLoader.load(ContainerService.class).spliterator(), false)
                        .sorted(Comparator.comparingInt{it.getPriority()})
                        .collect(Collectors.toList()),
                *additionalServices
        ] as Iterable<ContainerService>
//        [
//                new TimerService(),
//                new ManagerExecutorService(),
//                new I18NService(),
//                new ManagerPersistenceService(),
//                new MessageBrokerSetupService(),
//                new ManagerIdentityService(),
//                new SetupService(),
//                new ClientEventService(),
//                new RulesetStorageService(),
//                new RulesService(),
//                new AssetStorageService(),
//                new AssetDatapointService(),
//                new AssetAttributeLinkingService(),
//                new AssetProcessingService(),
//                new MessageBrokerService(),
//                *Lists.newArrayList(ServiceLoader.load(Protocol.class)),
//                new AgentService(),
//                new SimulatorService(),
//                new MapService(),
//                new NotificationService(),
//                new ConsoleAppService(),
//                new ManagerWebService(),
//                new HealthStatusService(),
//                *additionalServices
//
//        ] as Iterable<ContainerService>
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
            def endpoint = container.getService(MessageBrokerService.class).getContext().getEndpoint(name)
            if (!endpoint) {
                throw new IllegalArgumentException("Messaging endpoint not found: " + name)
            }
            new PollingConditions(initialDelay: 0.1, delay: 0.05).eventually {
                assert ((BrowsableEndpoint)endpoint).exchanges.size() == 0
            }
        }
    }
}
