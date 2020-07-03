package org.openremote.test

import com.google.common.collect.Lists
import org.apache.camel.spi.BrowsableEndpoint
import org.openremote.container.Container
import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetProcessingService
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import static org.openremote.container.security.keycloak.KeycloakIdentityProvider.IDENTITY_NETWORK_WEBSERVER_PORT
import static org.openremote.container.timer.TimerService.Clock.PSEUDO
import static org.openremote.container.timer.TimerService.Clock.REAL
import static org.openremote.container.timer.TimerService.TIMER_CLOCK_TYPE
import static org.openremote.container.web.WebService.WEBSERVER_LISTEN_PORT
import static org.openremote.manager.setup.SetupTasks.*

trait ManagerContainerTrait extends ContainerTrait {

    Map<String, String> defaultConfig(Integer serverPort) {
        if (serverPort == null) {
            serverPort = findEphemeralPort()
        }
        [
                (WEBSERVER_LISTEN_PORT)          : Integer.toString(serverPort),
                (IDENTITY_NETWORK_WEBSERVER_PORT): Integer.toString(serverPort),
                (SETUP_IMPORT_DEMO_ASSETS)       : "true",
                (SETUP_IMPORT_DEMO_USERS)        : "true",
                (SETUP_IMPORT_DEMO_SCENES)       : "false",
                (SETUP_IMPORT_DEMO_RULES)        : "false",
                (TIMER_CLOCK_TYPE)               : PSEUDO.name()
        ]
    }

    Iterable<ContainerService> defaultServices(Iterable<ContainerService> additionalServices) {
        [
            *Lists.newArrayList(ServiceLoader.load(ContainerService.class)),
            *additionalServices
        ].stream()
            .sorted(Comparator.comparingInt{it.getPriority()})
            .collect(Collectors.toList()) as Iterable<ContainerService>
    }

    Iterable<ContainerService> defaultServices(ContainerService... additionalServices) {
        defaultServices(Arrays.asList(additionalServices))
    }

    Container startContainerWithDemoScenesAndRules(Map<String, String> config, Iterable<ContainerService> services) {
        config << [(SETUP_IMPORT_DEMO_SCENES): "true"]
        config << [(SETUP_IMPORT_DEMO_RULES): "true"]
        startContainer(config, services)
    }

    /**
     * Use wall clock instead of pseudo clock.
     */
    Container startContainerWithoutPseudoClock(Map<String, String> config, Iterable<ContainerService> services) {
        startContainer(config << [(TIMER_CLOCK_TYPE): REAL.name()], services)
    }

    boolean noEventProcessedIn(AssetProcessingService assetProcessingService, int milliseconds) {
        return (assetProcessingService.lastProcessedEventTimestamp > 0
                && assetProcessingService.lastProcessedEventTimestamp + milliseconds < System.currentTimeMillis())
    }

    /**
     * Execute pseudo clock operations in Container.
     */
    void withClockOf(Container container, Closure<TimerService.Clock> clockConsumer) {
        clockConsumer.call(container.getService(TimerService.class).getClock())
    }

    long getClockTimeOf(Container container) {
        container.getService(TimerService.class).getClock().getCurrentTimeMillis()
    }

    void advancePseudoClock(long amount, TimeUnit unit, Container container) {
        withClockOf(container) { it.advanceTime(amount, unit) }
    }

    void noPendingExchangesOnMessageEndpoint(Container container, String... endpointName) {
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
