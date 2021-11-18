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
import org.openremote.container.Container
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.security.IdentityService
import org.openremote.container.security.PasswordAuthForm
import org.openremote.container.security.keycloak.KeycloakIdentityProvider
import org.openremote.container.timer.TimerService
import org.openremote.container.util.MapAccess
import org.openremote.container.web.DefaultWebsocketComponent
import org.openremote.container.web.WebClient
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.gateway.GatewayClientService
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.web.ManagerWebService
import org.openremote.model.Constants
import org.openremote.model.ContainerService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.agent.Agent
import org.openremote.model.gateway.GatewayConnection
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.RulesetQuery
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.TenantRuleset
import org.openremote.model.security.Role
import org.openremote.model.security.User
import org.openremote.model.util.Pair

import javax.websocket.ClientEndpointConfig
import javax.websocket.Endpoint
import javax.websocket.Session
import javax.websocket.WebSocketContainer
import javax.ws.rs.core.UriBuilder
import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.stream.Collectors
import java.util.stream.IntStream

import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.container.web.WebService.WEBSERVER_LISTEN_PORT

trait ContainerTrait {

    Container startContainer(Map<String, String> config, Iterable<ContainerService> services) {

        // Reset and start clock in case any previous tests stopped/modified it (pseudo clock is static so shared between tests)
        TimerService.Clock.PSEUDO.reset()
        TimerService.Clock.PSEUDO.advanceTime(-1, TimeUnit.MILLISECONDS) // Put time back so attribute events with the same timestamp as provisioned assets don't get rejected

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
                        if (entry.key == WEBSERVER_LISTEN_PORT) {
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
                try {
                    println("Services and config matches already running container so checking state")
                    def counter = 0

                    // Reset users - just delete ones that weren't there before (if tests change user roles etc. then the test should force stop the container in cleanup)
                    if (container.hasService(ManagerIdentityService.class)) {
                        def identityProvider = container.getService(ManagerIdentityService.class).getIdentityProvider()
                        def users = identityProvider.queryUsers()
                        def newUsers = users.findAll {user -> !TestFixture.users.any {previousUser -> previousUser.id == user.id}}
                        if (!TestFixture.users.every {previousUser -> users.any {user -> user.id == previousUser.id}}) {
                            throw new IllegalStateException("Users have been modified so cannot do simple purge")
                        }

                        println("Purging ${newUsers.size()} new user(s)")
                        newUsers.forEach {user -> identityProvider.deleteUser(user.realm, user.id)}
                    }

                    // Reset gateway connections
                    if (container.hasService(GatewayClientService.class)) {
                        def gatewayClientService = container.getService(GatewayClientService.class)
                        def gatewayConnections = getGatewayConnections()
                        println("Purging ${gatewayConnections.size()} gateway connection(s)")
                        gatewayClientService.deleteConnections(gatewayConnections.stream().map { it.localRealm }.collect(Collectors.toList()))
                    }

                    // Reset rulesets
                    if (container.hasService(RulesetStorageService.class) && container.hasService(RulesService.class)) {
                        def globalRulesets = getRulesets(GlobalRuleset.class)
                        def tenantRulesets = getRulesets(TenantRuleset.class)
                        def assetRulesets = getRulesets(AssetRuleset.class)
                        def rulesService = container.getService(RulesService.class)
                        def rulesetStorageService = container.getService(RulesetStorageService.class)

                        if (!assetRulesets.isEmpty()) {
                            println("Purging ${assetRulesets.size()} asset ruleset(s)")
                            assetRulesets.forEach {
                                rulesetStorageService.delete(AssetRuleset.class, it.id)
                                def assetEngine = rulesService.assetEngines.get(((AssetRuleset) it).assetId)
                                def rulesStopped = false
                                while (assetEngine != null && !rulesStopped) {
                                    rulesStopped = assetEngine.deployments.containsKey(it.id)
                                    if (counter++ > 100) {
                                        throw new IllegalStateException("Failed to purge ruleset: " + it.name)
                                    }
                                    Thread.sleep(100)
                                }
                            }
                        }
                        if (!tenantRulesets.isEmpty()) {
                            println("Purging ${tenantRulesets.size()} tenant ruleset(s)")
                            tenantRulesets.forEach {
                                rulesetStorageService.delete(TenantRuleset.class, it.id)
                                def tenantEngine = rulesService.tenantEngines.get(((TenantRuleset) it).realm)
                                def rulesStopped = false
                                while (tenantEngine != null && !rulesStopped) {
                                    rulesStopped = !tenantEngine.deployments.containsKey(it.id)
                                    if (counter++ > 100) {
                                        throw new IllegalStateException("Failed to purge ruleset: " + it.name)
                                    }
                                    Thread.sleep(100)
                                }
                            }
                        }
                        if (!globalRulesets.isEmpty()) {
                            println("Purging ${globalRulesets.size()} global ruleset(s)")
                            globalRulesets.forEach {
                                rulesetStorageService.delete(GlobalRuleset.class, it.id)
                                def rulesStopped = false
                                while (!rulesStopped) {
                                    def engine = rulesService.globalEngine
                                    rulesStopped = engine == null || !engine.deployments.containsKey(it.id)
                                    if (counter++ > 100) {
                                        throw new IllegalStateException("Failed to purge ruleset: " + it.name)
                                    }
                                    Thread.sleep(100)
                                }
                            }
                        }

                        // Wait for all rule engines to stop and be removed
                        counter = 0
                        def enginesStopped = false
                        while (!enginesStopped) {
                            enginesStopped = (rulesService.tenantEngines == null || rulesService.tenantEngines.isEmpty()) && (rulesService.assetEngines == null || rulesService.assetEngines.isEmpty())
                            if (counter++ > 100) {
                                throw new IllegalStateException("Rule engines have failed to stop")
                            }
                            Thread.sleep(100)
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
                            if (counter++ > 100) {
                                throw new IllegalStateException("Failed to purge agents")
                            }
                            agentsRemoved = container.getService(AgentService.class).agentMap.isEmpty()
                            Thread.sleep(100)
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
                            TestFixture.userAssets.groupBy {it.id.userId}.values().forEach { ua ->
                                assetStorageService.storeUserAssetLinks(ua)
                            }
                        }
                    }

                    // Re-insert rulesets (after assets have been re-inserted)
                    if (container.hasService(RulesetStorageService.class) && container.hasService(RulesService.class)) {
                        def rulesetStorageService = container.getService(RulesetStorageService.class)

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
                } catch (IllegalStateException e) {
                    System.out.println("Failed to clean the existing container so creating a new one")
                    stopContainer()
                    TestFixture.container = null
                }
            } else {
                System.out.println("Request to start container with different config and/or services as already running container so restarting")
                stopContainer()
                TestFixture.container = null
            }
        }

        if (container == null) {
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

            // Track gateway connections
            TestFixture.gatewayConnections = getGatewayConnections()

            // Track users and their roles
            TestFixture.users = getUsers()
        }

        // Wait for agents and rulesets to be deployed - Just a very basic way of waiting for the system to be 'initialised'
        def agentService = container.hasService(AgentService.class) ? container.getService(AgentService.class) : null
        def rulesService = container.hasService(RulesService.class) ? container.getService(RulesService.class) : null
        def assetProcessingService = container.hasService(AssetProcessingService.class) ? container.getService(AssetProcessingService.class) : null
        int i=0

        if (agentService != null) {
            println("Waiting for agents to be deployed")
            i=0
            while (i < 100 && TestFixture.assets.stream().filter { it instanceof Agent }.any { !agentService.agentMap.containsKey(it.id) }) {
                Thread.sleep(100)
                i++
            }
            println("Agents are deployed")
        }

        if (rulesService != null) {
            println("Waiting for global rulesets to be deployed")
            i=0
            while (i < 100 && TestFixture.globalRulesets.stream().filter { it.enabled }.any { rulesService.globalEngine == null || !rulesService.globalEngine.deployments.containsKey(it.id) }) {
                Thread.sleep(100)
                i++
            }
            println("Global rulesets are deployed")

            println("Waiting for tenant rulesets to be deployed")
            i=0
            while (i < 100 && TestFixture.tenantRulesets.stream().filter { it.enabled }.any { !rulesService.tenantEngines.containsKey(it.realm) || !rulesService.tenantEngines.get(it.realm).deployments.containsKey(it.id) }) {
                Thread.sleep(100)
                i++
            }
            println("Tenant rulesets are deployed")

            println("Waiting for asset rulesets to be deployed")
            i=0
            while (i < 100 && TestFixture.assetRulesets.stream().filter { it.enabled }.any { !rulesService.assetEngines.containsKey(it.assetId) || !rulesService.assetEngines.get(it.assetId).deployments.containsKey(it.id) }) {
                Thread.sleep(100)
                i++
            }
            println("Asset rulesets are deployed")
        }

        if (assetProcessingService != null) {
            println("Waiting for the system to settle down")
            def j=0
            while (j < 100 && !noEventProcessedIn(assetProcessingService, 300)) {
                Thread.sleep(100)
                j++
            }
        }

        // Reset and start clock in case any previous tests stopped/modified it (pseudo clock is static so shared between tests)
        TimerService.Clock.PSEUDO.reset()
        TimerService.Clock.PSEUDO.advanceTime(10, TimeUnit.MILLISECONDS) // Advance clock so attribute events from tests will succeed (even if actual clock hasn't moved more than a millisecond)

        return container
    }

    boolean noEventProcessedIn(AssetProcessingService assetProcessingService, int milliseconds) {
        return (assetProcessingService.lastProcessedEventTimestamp > 0
            && assetProcessingService.lastProcessedEventTimestamp + milliseconds < System.currentTimeMillis())
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

    List<UserAssetLink> getUserAssets() {
        if (!container.hasService(AssetStorageService.class)) {
            return Collections.emptyList()
        }
        container.getService(AssetStorageService.class).findUserAssetLinks(null, null, null)
    }

    List<GatewayConnection> getGatewayConnections() {
        if (!container.hasService(GatewayClientService.class)) {
            return Collections.emptyList()
        }
        container.getService(GatewayClientService.class).getConnections()
    }

    List<User> getUsers() {
        if (!container.hasService(ManagerIdentityService)) {
            return Collections.emptyList()
        }
        def identityProvider = container.getService(ManagerIdentityService.class).getIdentityProvider()
        List<User> users = identityProvider.queryUsers()
        return users
//        List<Pair<User, Pair<Role[], Role[]>>> results = []
//        users.forEach {user ->
//            def roles = identityProvider.getUserRoles(user.realm, user.id, Constants.KEYCLOAK_CLIENT_ID)
//            def realmRoles = identityProvider.getUserRealmRoles(user.realm, user.id)
//            results.add(new Pair<User, Pair<Role[],Role[]>>(user, new Pair<>(roles, realmRoles)))
//        }
//        return results
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
