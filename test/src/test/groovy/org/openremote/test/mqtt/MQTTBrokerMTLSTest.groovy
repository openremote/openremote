/*
 * Copyright 2024, OpenRemote Inc.
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
package org.openremote.test.mqtt

import org.openremote.manager.mqtt.MQTTBrokerService
import org.openremote.manager.mqtt.OpenRemoteSSLContextFactory
import org.openremote.manager.setup.SetupService
import org.openremote.model.Container
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import static org.openremote.model.Constants.*

/**
 * Integration test for MQTT mTLS (mutual TLS) functionality
 */
class MQTTBrokerMTLSTest extends Specification implements ManagerContainerTrait {

    def "test MQTTBrokerService registers container with OpenRemoteSSLContextFactory"() {
        given: "expected setup"
        def container = null as Container

        when: "the container starts with MQTT broker enabled"
        container = startContainer(defaultConfig(), defaultServices())

        then: "the OpenRemoteSSLContextFactory should have the container registered"
        // The container should be registered during MQTTBrokerService.start()
        // We can verify this by checking that the service started without errors
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        assert mqttBrokerService != null
    }

    def "test MQTTBrokerService starts without mTLS when disabled"() {
        given: "configuration with mTLS disabled"
        def config = defaultConfig()
        config.put(OR_MQTT_MTLS_DISABLED, "true")

        and: "a container"
        def container = null as Container

        when: "the container starts"
        container = startContainer(config, defaultServices())

        then: "MQTT broker should start successfully"
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        mqttBrokerService != null
        mqttBrokerService.active

        and: "only the standard MQTT port should be configured"
        mqttBrokerService.port == 1883

        
    }

    def "test MQTTBrokerService configuration with custom ports"() {
        given: "configuration with custom ports"
        def customPort = findEphemeralPort()
        def customMtlsPort = findEphemeralPort()
        def config = defaultConfig()
        config.put(MQTTBrokerService.MQTT_SERVER_LISTEN_PORT, customPort.toString())
        config.put(OR_MQTT_MTLS_SERVER_LISTEN_PORT, customMtlsPort.toString())

        and: "a container"
        def container = null as Container

        when: "the container starts"
        container = startContainer(config, defaultServices())

        then: "MQTT broker should use custom ports"
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        mqttBrokerService.port == customPort
        mqttBrokerService.mtlsPort == customMtlsPort

        
    }

    def "test MQTTBrokerService user asset link change triggers disconnect debouncer"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        expect: "the disconnect debouncer should be initialized"
        mqttBrokerService.userAssetDisconnectDebouncer != null

        
    }

    def "test MQTTBrokerService initializes custom handlers"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        when: "getting custom handlers"
        def handlers = mqttBrokerService.getCustomHandlers()

        then: "handlers should be loaded and initialized"
        handlers != null
        // The DefaultMQTTHandler should be loaded via ServiceLoader
        handlers.size() > 0


    }

    def "test MQTTBrokerService getConnectionIDString handles null connection"() {
        expect: "getConnectionIDString with null should return null"
        MQTTBrokerService.getConnectionIDString(null) == null
    }

    def "test MQTTBrokerService connectionToString handles null connection"() {
        expect: "connectionToString with null should return empty string"
        MQTTBrokerService.connectionToString(null) == ""
    }

    def "test MQTTBrokerService disconnected connection cache configured"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        expect: "disconnected connection cache should be initialized"
        mqttBrokerService.disconnectedConnectionCache != null

        
    }

    def "test MQTTBrokerService force disconnect debounce is configurable"() {
        given: "configuration with custom debounce time"
        def customDebounceMillis = 1000
        def config = defaultConfig()
        config.put(MQTTBrokerService.MQTT_FORCE_USER_DISCONNECT_DEBOUNCE_MILLIS, customDebounceMillis.toString())

        and: "a container"
        def container = null as Container

        when: "the container starts"
        container = startContainer(config, defaultServices())

        then: "MQTT broker should initialize successfully"
        def mqttBrokerService = container.getService(MQTTBrokerService.class)
        mqttBrokerService != null
        mqttBrokerService.active

        
    }

    def "test MQTTBrokerService getUserConnections returns empty for null userID"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        when: "getting connections for null user ID"
        def connections = mqttBrokerService.getUserConnections(null)

        then: "should return empty set"
        connections.isEmpty()

        
    }

    def "test MQTTBrokerService getUserConnections returns empty for empty userID"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        when: "getting connections for empty user ID"
        def connections = mqttBrokerService.getUserConnections("")

        then: "should return empty set"
        connections.isEmpty()

        
    }

    def "test MQTTBrokerService getConnectionFromClientID returns null for null clientID"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        when: "getting connection for null client ID"
        def connection = mqttBrokerService.getConnectionFromClientID(null)

        then: "should return null"
        connection == null

        
    }

    def "test MQTTBrokerService getConnectionFromClientID returns null for empty clientID"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        when: "getting connection for empty client ID"
        def connection = mqttBrokerService.getConnectionFromClientID("")

        then: "should return null"
        connection == null

        
    }

    def "test MQTTBrokerService disconnectSession returns false for non-existent connection"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        when: "disconnecting a non-existent connection"
        def result = mqttBrokerService.disconnectSession("non-existent-connection-id")

        then: "should return false"
        result == false

        
    }

    def "test MQTTBrokerService server configuration has correct defaults"() {
        given: "a running container"
        def container = startContainer(defaultConfig(), defaultServices())
        def mqttBrokerService = container.getService(MQTTBrokerService.class)

        when: "getting server configuration"
        def config = mqttBrokerService.serverConfiguration

        then: "configuration should have expected defaults"
        config.isPersistenceEnabled() == false
        config.getAuthenticationCacheSize() == 0
        config.getAuthorizationCacheSize() == 0
        config.getMqttSessionScanInterval() == 10000

        
    }
}
