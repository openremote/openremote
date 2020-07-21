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
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.test

import org.apache.camel.ProducerTemplate
import org.apache.camel.builder.RouteBuilder
import org.glassfish.tyrus.client.ClientManager
import org.jboss.resteasy.client.jaxrs.ResteasyClient
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.keycloak.representations.AccessTokenResponse
import org.openremote.agent.protocol.AbstractProtocol
import org.openremote.container.Container
import org.openremote.container.ContainerService
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.persistence.PersistenceService
import org.openremote.container.security.IdentityService
import org.openremote.container.security.PasswordAuthForm
import org.openremote.container.security.keycloak.KeycloakIdentityProvider
import org.openremote.container.util.MapAccess
import org.openremote.container.web.DefaultWebsocketComponent
import org.openremote.container.web.WebClient
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.web.ManagerWebService
import org.openremote.model.Constants
import org.openremote.model.asset.Asset
import org.openremote.model.asset.UserAsset
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.RulesetQuery
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.TenantRuleset

import javax.websocket.ClientEndpointConfig
import javax.websocket.Endpoint
import javax.websocket.Session
import javax.websocket.WebSocketContainer
import javax.ws.rs.core.UriBuilder
import java.util.logging.Handler
import java.util.stream.Collectors
import java.util.stream.IntStream

import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.container.security.keycloak.KeycloakIdentityProvider.IDENTITY_NETWORK_WEBSERVER_PORT
import static org.openremote.container.web.WebService.WEBSERVER_LISTEN_PORT

trait ContainerTrait {

    Container startContainer(Map<String, String> config, Iterable<ContainerService> services) {

        if (container != null) {
            // Compare config and services
            def configsMatch = false
            def servicesMatch = false
            def currentConfig = container.getConfig()
            if (currentConfig == config) {
                configsMatch = true
            } else {
                if (currentConfig.size() == config.size()) {
                    configsMatch = currentConfig.entrySet().stream().allMatch{entry ->
                        // ignore webserver port config
                        if (entry.key == WEBSERVER_LISTEN_PORT || entry.key == IDENTITY_NETWORK_WEBSERVER_PORT) {
                            return true
                        }
                        entry.value == config.get(entry.key)
                    }
                }
            }

            def currentServiceList = new ArrayList<ContainerService>()
            currentServiceList.addAll(container.getServices())
            currentServiceList.removeIf{it instanceof Handler} // Exclude log handlers as they are loaded by JUL
            def serviceList = services.asList()
            if (serviceList.size() == currentServiceList.size()) {
                servicesMatch = IntStream.range(0, serviceList.size()).allMatch { i ->
                    serviceList.get(i).class == currentServiceList.get(i).class
                }
            }

            if (configsMatch && servicesMatch) {

                println("Services and config matches already running container so checking state")

//                    // Crude way is to restart some services to reset the system but this isn't super fast
//                    currentServiceList.find {it instanceof PersistenceService},
//                    currentServiceList.find {it instanceof SetupService},
//                    currentServiceList.find {it instanceof IdentityService},
//                    currentServiceList.find {it instanceof AgentService},
//                    currentServiceList.find {it instanceof RulesService}

                // Reset rulesets
                if (container.hasService(RulesetStorageService.class) && container.hasService(RulesService.class)) {
                    def globalRulesets = getRulesets(GlobalRuleset.class)
                    def tenantRulesets = getRulesets(TenantRuleset.class)
                    def assetRulesets = getRulesets(AssetRuleset.class)
                    def rulesService = container.getService(RulesService.class)
                    def rulesetStorageService = container.getService(RulesetStorageService.class)

                    if (!globalRulesets.isEmpty()) {
                        println("Purging ${globalRulesets.size()} global ruleset(s)")
                        globalRulesets.forEach { rulesetStorageService.delete(GlobalRuleset.class, it.id) }
                    }
                    if (!tenantRulesets.isEmpty()) {
                        println("Purging ${tenantRulesets.size()} tenant ruleset(s)")
                        tenantRulesets.forEach { rulesetStorageService.delete(TenantRuleset.class, it.id) }
                    }
                    if (!assetRulesets.isEmpty()) {
                        println("Purging ${assetRulesets.size()} asset ruleset(s)")
                        assetRulesets.forEach { rulesetStorageService.delete(AssetRuleset.class, it.id) }
                    }
                    // Wait for all rule engines to stop and be removed
                    def enginesStopped = false
                    while (!enginesStopped) {
                        enginesStopped = rulesService.globalEngine == null && rulesService.tenantEngines.isEmpty() && rulesService.assetEngines.isEmpty()
                        Thread.sleep(100)
                    }
                    if (!TestFixture.assetRulesets.isEmpty()) {
                        println("Re-inserting ${TestFixture.assetRulesets.size()} asset ruleset(s)")
                        TestFixture.assetRulesets.forEach { rulesetStorageService.merge(it) }
                    }
                    if (!TestFixture.tenantRulesets.isEmpty()) {
                        println("Re-inserting ${TestFixture.tenantRulesets.size()} tenant ruleset(s)")
                        TestFixture.tenantRulesets.forEach { rulesetStorageService.merge(it) }
                    }
                    if (!TestFixture.globalRulesets.isEmpty()) {
                        println("Re-inserting ${TestFixture.globalRulesets.size()} global ruleset(s)")
                        TestFixture.globalRulesets.forEach { rulesetStorageService.merge(it) }
                    }
                }

                // Reset assets
                if (container.hasService(AgentService.class) && container.hasService(AssetStorageService.class)) {
                    def currentAssets = getAssets().collect { it.getId() }
                    def assetStorageService = container.getService(AssetStorageService.class)

                    if (!currentAssets.isEmpty()) {
                        println("Purging ${currentAssets.size()} asset(s)")
                        assetStorageService.delete(currentAssets, true)
                    }

                    // Wait for all assets to be unlinked from protocols
                    def agentsRemoved = false
                    while (!agentsRemoved) {
                        agentsRemoved = container.getService(AgentService.class).agentMap.isEmpty()
                    }
                    if (!TestFixture.assets.isEmpty()) {
                        println("Re-inserting ${TestFixture.assets.size()} asset(s)")
                        TestFixture.assets.forEach { a ->
                            a.version = 0
                            assetStorageService.merge(a, true)
                        }
                    }
                    if (!TestFixture.userAssets.isEmpty()) {
                        println("Re-linking ${TestFixture.userAssets.size()} user asset(s)")
                        TestFixture.userAssets.forEach { ua ->
                            assetStorageService.storeUserAsset(ua)
                        }
                    }
                }

                return container
            } else {
                System.out.println("Request to start container with different config and/or services as already running container so restarting")
                stopContainer()
            }
        }
        TestFixture.container = new Container(config, services)
        container.startBackground()

        // Block until container is actually running to aid in testing but also to record some state info
        while (!container.isRunning()) {
            Thread.sleep(100)
        }

        // Track rules (very basic)
        TestFixture.globalRulesets = getRulesets(GlobalRuleset.class)
        TestFixture.tenantRulesets = getRulesets(TenantRuleset.class)
        TestFixture.assetRulesets = getRulesets(AssetRuleset.class)

        // Track assets
        TestFixture.assets = getAssets()
        TestFixture.userAssets = getUserAssets()

        container
    }

    List<Ruleset> getRulesets(Class<? extends Ruleset> clazz) {
        if (!container.hasService(RulesetStorageService.class)) {
            return Collections.emptyList()
        }
        RulesetStorageService rulesetStorageService = container.getService(RulesetStorageService.class)
        return rulesetStorageService.findAll(clazz, new RulesetQuery().setFullyPopulate(true))
    }

    List<TenantRuleset> getTenantRulesets() {
        if (!container.hasService(RulesService.class)) {
            return Collections.emptyList()
        }
        RulesService rulesService = container.getService(RulesService.class)
        return rulesService.tenantEngines.values().collect {it.deployments.values()}.flatten {it.ruleset}
    }

    List<AssetRuleset> getAssetRulesets() {
        if (!container.hasService(RulesService.class)) {
            return Collections.emptyList()
        }
        RulesService rulesService = container.getService(RulesService.class)
        return rulesService.tenantEngines.values().collect {it.deployments.values()}.flatten {it.ruleset}
    }

    List<Asset> getAssets() {
        if (!container.hasService(AssetStorageService.class)) {
            return Collections.emptyList()
        }
        container.getService(AssetStorageService.class).findAll(new AssetQuery())
    }

    List<UserAsset> getUserAssets() {
        if (!container.hasService(AssetStorageService.class)) {
            return Collections.emptyList()
        }
        container.getService(AssetStorageService.class).findUserAssets(null, null, null)
    }

    Container getContainer() {
        return TestFixture.container
    }

    int getServerPort() {
        if (container != null) {
            return MapAccess.getInteger(container.getConfig(), WEBSERVER_LISTEN_PORT, 0)
        }
        return 0
    }

    boolean isContainerRunning() {
        container.running
    }

    ResteasyClientBuilder createClient() {
        ResteasyClientBuilder clientBuilder =
                new ResteasyClientBuilder()
                        .establishConnectionTimeout(2, SECONDS)
                        .socketTimeout(15, SECONDS)
                        .connectionPoolSize(10)
        return WebClient.registerDefaults(clientBuilder)
    }

    UriBuilder serverUri(int serverPort) {
        UriBuilder.fromUri("")
                .scheme("http").host("127.0.0.1").port(serverPort)
    }

    void stopContainer() {
        if (container != null) {
            container.stop()
        }
    }

    ResteasyWebTarget getClientTarget(UriBuilder serverUri, String accessToken) {
        WebClient.getTarget(createClient().build(), serverUri.build(), accessToken, null, null)
    }

    ResteasyWebTarget getClientApiTarget(UriBuilder serverUri, String realm) {
        WebClient.getTarget(createClient().build(), serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).build(), null, null, null)
    }

    ResteasyWebTarget getClientApiTarget(UriBuilder serverUri, String realm, String accessToken) {
        WebClient.getTarget(createClient().build(), serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).build(), accessToken, null, null)
    }

    ResteasyWebTarget getClientApiTarget(UriBuilder serverUri, String realm, String path, String accessToken) {
        WebClient.getTarget(createClient().build(), serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).path(path).build(), accessToken, null, null)
    }

    ResteasyWebTarget getClientApiTarget(ResteasyClient client, UriBuilder serverUri, String realm, String accessToken) {
        WebClient.getTarget(client, serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).build(), accessToken, null, null)
    }

    ResteasyWebTarget getClientApiTarget(ResteasyClient client, UriBuilder serverUri, String realm, String path, String accessToken) {
        WebClient.getTarget(client, serverUri.clone().replacePath(ManagerWebService.API_PATH).path(realm).path(path).build(), accessToken, null, null)
    }

    int findEphemeralPort() {
        ServerSocket socket = new ServerSocket(0, 0, Inet4Address.getLoopbackAddress())
        socket.close()
        int port = socket.getLocalPort()
        return port
    }

    AccessTokenResponse authenticate(Container container, String realm, String clientId, String username, String password) {
        ((KeycloakIdentityProvider)container.getService(IdentityService.class).getIdentityProvider()).getKeycloak()
                .getAccessToken(realm, new PasswordAuthForm(clientId, username, password))
    }

    ProducerTemplate getMessageProducerTemplate(Container container) {
        return container.getService(MessageBrokerService.class).getContext().createProducerTemplate()
    }

    void addRoutes(RouteBuilder routeBuilder) {
        container.getService(MessageBrokerService.class).getContext().addRoutes(routeBuilder)
    }

    WebSocketContainer createWebsocketClient() {
        return ClientManager.createClient()
    }

    UriBuilder getWebsocketServerUrl(UriBuilder uriBuilder, String endpointPath, String realm, String accessToken) {
        uriBuilder.clone()
                .scheme("ws")
                .replacePath(DefaultWebsocketComponent.WEBSOCKET_PATH)
                .path(endpointPath)
                .queryParam(Constants.REQUEST_HEADER_REALM, realm)
                .queryParam("Authorization", "Bearer " + accessToken)
    }

    Session connect(WebSocketContainer websocketContainer, Endpoint endpoint, UriBuilder serverUri, String endpointPath, String realm, String accessToken) {
        def websocketUrl = getWebsocketServerUrl(serverUri, endpointPath, realm, accessToken)
        def config = ClientEndpointConfig.Builder.create().build()
        websocketContainer.connectToServer(endpoint, config, websocketUrl.build())
    }
}
