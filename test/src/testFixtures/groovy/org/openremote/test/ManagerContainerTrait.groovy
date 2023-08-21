package org.openremote.test

import com.google.common.collect.Lists
import org.apache.camel.spi.BrowsableEndpoint
import org.openremote.container.Container
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.timer.TimerService
import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.model.ContainerService
import spock.util.concurrent.PollingConditions

import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

import static org.openremote.container.timer.TimerService.Clock.PSEUDO
import static org.openremote.container.timer.TimerService.TIMER_CLOCK_TYPE
import static org.openremote.container.web.WebService.OR_WEBSERVER_LISTEN_PORT
import static org.openremote.manager.mqtt.MQTTBrokerService.MQTT_SERVER_LISTEN_HOST
import static org.openremote.manager.rules.RulesService.OR_RULES_MIN_TEMP_FACT_EXPIRATION_MILLIS
import static org.openremote.manager.rules.RulesService.OR_RULES_QUICK_FIRE_MILLIS
import static org.openremote.model.Container.OR_METRICS_ENABLED

trait ManagerContainerTrait extends ContainerTrait {

    Map<String, String> defaultConfig(Integer serverPort) {
        if (serverPort == null) {
            serverPort = findEphemeralPort()
        }
        [
                (OR_WEBSERVER_LISTEN_PORT): Integer.toString(serverPort),
                (MQTT_SERVER_LISTEN_HOST) : "127.0.0.1", // Works best for cross platform test running,
                (MQTTBrokerService.MQTT_FORCE_USER_DISCONNECT_DEBOUNCE_MILLIS): "10",
                (OR_RULES_QUICK_FIRE_MILLIS): "500",
                (OR_RULES_MIN_TEMP_FACT_EXPIRATION_MILLIS): "500",
                (TIMER_CLOCK_TYPE)        : PSEUDO.name(),
                (OR_METRICS_ENABLED):  "false"
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

    void stopPseudoClock() {
        withClockOf(container) { it.stop() }
    }

    void startPseudoClock() {
        withClockOf(container) { it.start() }
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
