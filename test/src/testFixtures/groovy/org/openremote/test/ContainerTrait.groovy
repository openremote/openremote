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

import jakarta.persistence.TypedQuery
import jakarta.ws.rs.core.UriBuilder
import org.apache.camel.ProducerTemplate
import org.jboss.resteasy.client.jaxrs.ResteasyClient
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl
import org.keycloak.representations.AccessTokenResponse
import org.openremote.container.Container
import org.openremote.container.message.MessageBrokerService
import org.openremote.container.persistence.PersistenceService
import org.openremote.container.security.IdentityService
import org.openremote.container.security.PasswordAuthForm
import org.openremote.container.security.keycloak.KeycloakIdentityProvider
import org.openremote.container.timer.TimerService
import org.openremote.container.util.LogUtil
import org.openremote.container.util.MapAccess
import org.openremote.container.web.WebClient
import org.openremote.manager.agent.AgentService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.gateway.GatewayClientService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.security.ManagerIdentityService
import org.openremote.manager.web.ManagerWebService
import org.openremote.model.ContainerService
import org.openremote.model.asset.Asset
import org.openremote.model.asset.UserAssetLink
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.agent.Protocol
import org.openremote.model.gateway.GatewayConnection
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.RulesetQuery
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.RealmRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.model.security.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import java.util.logging.Handler
import java.util.stream.Collectors
import java.util.stream.IntStream

import static java.util.concurrent.TimeUnit.SECONDS
import static org.openremote.container.web.WebService.OR_WEBSERVER_LISTEN_PORT
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.Constants.MASTER_REALM_ADMIN_USER

trait ContainerTrait {

    static {
        LogUtil.initialiseJUL()
    }

    Logger LOG = LoggerFactory.getLogger(ContainerTrait.class)

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
                        if (entry.key == OR_WEBSERVER_LISTEN_PORT) {
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
                    long startTime = System.currentTimeMillis()
                    LOG.info("Services and config matches already running container so checking state")
                    def counter = 0

                    // Reset users - just delete ones that weren't there before (if tests change user roles etc. then the test should force stop the container in cleanup)
                    if (container.hasService(ManagerIdentityService.class)) {
                        def identityProvider = container.getService(ManagerIdentityService.class).getIdentityProvider()
                        def users = identityProvider.queryUsers()
                        def newUsers = users.findAll {user -> !TestFixture.users.any {previousUser -> previousUser.id == user.id}}
                        if (!TestFixture.users.every {previousUser -> users.any {user -> user.id == previousUser.id}}) {
                            throw new IllegalStateException("Users have been modified so cannot do simple purge")
                        }

                        LOG.info("Purging ${newUsers.size()} new user(s)")
                        newUsers.forEach {user ->
                            // Never delete the master admin user
                            if (user.getUsername() != MASTER_REALM_ADMIN_USER && user.getRealm() != MASTER_REALM) {
                                identityProvider.deleteUser(user.realm, user.id)
                            }
                        }
                    }

                    // Reset gateway connections
                    if (container.hasService(GatewayClientService.class)) {
                        def gatewayClientService = container.getService(GatewayClientService.class)
                        def gatewayConnections = getGatewayConnections()
                        LOG.info("Purging ${gatewayConnections.size()} gateway connection(s)")
                        gatewayClientService.deleteConnections(gatewayConnections.stream().map { it.localRealm }.collect(Collectors.toList()))
                    }

                    // Reset rulesets
                    if (container.hasService(RulesetStorageService.class) && container.hasService(RulesService.class)) {
                        def rulesService = container.getService(RulesService.class)
                        def rulesetStorageService = container.getService(RulesetStorageService.class)

                        def assetEngines = new HashSet<RulesEngine>(rulesService.assetEngines.values())
                        LOG.info("Purging ${assetEngines.size()} asset engine(s)")
                        assetEngines.forEach {
                            it.stop()
                            rulesService.assetEngines.values().remove(it)
                        }
                        def assetRulesets = getRulesets(AssetRuleset.class)
                        assetRulesets.forEach{
                            rulesetStorageService.delete(AssetRuleset.class, it.id)
                        }

                        def realmEngines = new HashSet<RulesEngine>(rulesService.realmEngines.values())
                        LOG.info("Purging ${realmEngines.size()} realm engine(s)")
                        realmEngines.forEach {
                            it.stop()
                            rulesService.realmEngines.values().remove(it)
                        }
                        def realmRulesets = getRulesets(RealmRuleset.class)
                        realmRulesets.forEach{
                            rulesetStorageService.delete(RealmRuleset.class, it.id)
                        }

                        def globalEngine = rulesService.globalEngine
                        if (globalEngine != null) {
                            LOG.info("Purging global engine")
                            globalEngine.stop()
                            rulesService.globalEngine = null
                        }
                        def globalRulesets = getRulesets(GlobalRuleset.class)
                        globalRulesets.forEach{
                            rulesetStorageService.delete(GlobalRuleset.class, it.id)
                        }
                    }

                    // Wait for system to settle down
                    def assetProcessingService = container.hasService(AssetProcessingService.class) ? container.getService(AssetProcessingService.class) : null
                    if (assetProcessingService != null) {
                        LOG.info("Waiting for the system to settle down")
                        def j=0
                        while (j < 100 && !noEventProcessedIn(assetProcessingService, 300)) {
                            Thread.sleep(100)
                            j++
                        }
                    }

                    // Reset assets
                    if (container.hasService(AgentService.class) && container.hasService(AssetStorageService.class)) {
                        def currentAssets = getAssets().collect { it.getId() }
                        def assetStorageService = container.getService(AssetStorageService.class)

                        if (!currentAssets.isEmpty()) {
                            LOG.info("Purging ${currentAssets.size()} asset(s)")
                            assetStorageService.delete(currentAssets, true)
                        }

                        // Wait for all assets to be unlinked from protocols
                        def agentsRemoved = false
                        while (!agentsRemoved) {
                            if (counter++ > 100) {
                                throw new IllegalStateException("Failed to purge agents")
                            }
                            agentsRemoved = container.getService(AgentService.class).agents.isEmpty()
                            Thread.sleep(100)
                        }

                        if (!TestFixture.assets.isEmpty()) {
                            LOG.info("Re-inserting ${TestFixture.assets.size()} asset(s)")
                            TestFixture.assets.forEach { a ->
                                a.version = 0
                                assetStorageService.merge(a, true)
                            }
                        }
                        if (!TestFixture.userAssets.isEmpty()) {
                            LOG.info("Re-linking ${TestFixture.userAssets.size()} user asset(s)")
                            TestFixture.userAssets.groupBy {it.id.userId}.values().forEach { ua ->
                                assetStorageService.storeUserAssetLinks(ua)
                            }
                        }
                    }

                    // Re-insert rulesets (after assets have been re-inserted)
                    if (container.hasService(RulesetStorageService.class) && container.hasService(RulesService.class)) {
                        def rulesetStorageService = container.getService(RulesetStorageService.class)

                        if (!TestFixture.assetRulesets.isEmpty()) {
                            LOG.info("Re-inserting ${TestFixture.assetRulesets.size()} asset ruleset(s)")
                            TestFixture.assetRulesets.forEach { rulesetStorageService.merge(it) }
                        }
                        if (!TestFixture.realmRulesets.isEmpty()) {
                            LOG.info("Re-inserting ${TestFixture.realmRulesets.size()} realm ruleset(s)")
                            TestFixture.realmRulesets.forEach { rulesetStorageService.merge(it) }
                        }
                        if (!TestFixture.globalRulesets.isEmpty()) {
                            LOG.info("Re-inserting ${TestFixture.globalRulesets.size()} global ruleset(s)")
                            TestFixture.globalRulesets.forEach { rulesetStorageService.merge(it) }
                        }
                    }

                    long endTime = System.currentTimeMillis()
                    LOG.info("Container reuse took: " + (startTime - endTime) + "ms")
                } catch (IllegalStateException e) {
                    LOG.info("Failed to clean the existing container so creating a new one", e)
                    stopContainer()
                    TestFixture.container = null
                }
            } else {
                LOG.info("Request to start container with different config and/or services as already running container so restarting")
                stopContainer()
                TestFixture.container = null
            }
        }

        if (container == null) {
            try {
                TestFixture.container = new Container(config, services)
                container.startBackground()
            } catch (Exception e) {
                LOG.warn("Failed to start the container")
                stopContainer()
                throw e
            }

            // Block until container is actually running to aid in testing but also to record some state info
            while (!container.isRunning()) {
                Thread.sleep(100)
            }

            // Track rules (very basic)
            TestFixture.globalRulesets = getRulesets(GlobalRuleset.class)
            TestFixture.realmRulesets = getRulesets(RealmRuleset.class)
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
            LOG.info("Waiting for agents to be deployed")
            i=0
            while (i < 100 && TestFixture.assets.stream().filter { it instanceof Agent }.any {
                def agent = agentService.agents.get(it.id)
                if (agent == null) {
                    return true
                }
                Protocol<?> protocolInstance = agentService.protocolInstanceMap.get(it.id)
                if (protocolInstance == null) {
                    return true
                }
                return !protocolInstance.agent.is(agent)
            }) {
                Thread.sleep(100)
                i++
            }
            if (i >= 100) {
                LOG.info("Agents didn't load correctly so stopping and starting the container")
                stopContainer()
                startContainer(config, services)
            } else {
                LOG.info("Agents are deployed")
            }
        }

        if (rulesService != null) {
            LOG.info("Waiting for global rulesets to be deployed")
            i=0
            while (i < 100 && TestFixture.globalRulesets.stream().filter { it.enabled }.any { rulesService.globalEngine == null || !rulesService.globalEngine.deployments.containsKey(it.id) }) {
                Thread.sleep(100)
                i++
            }
            LOG.info("Global rulesets are deployed")

            LOG.info("Waiting for realm rulesets to be deployed")
            i=0
            while (i < 100 && TestFixture.realmRulesets.stream().filter { it.enabled }.any { !rulesService.realmEngines.containsKey(it.realm) || !rulesService.realmEngines.get(it.realm).deployments.containsKey(it.id) }) {
                Thread.sleep(100)
                i++
            }
            LOG.info("Realm rulesets are deployed")

            LOG.info("Waiting for asset rulesets to be deployed")
            i=0
            while (i < 100 && TestFixture.assetRulesets.stream().filter { it.enabled }.any { !rulesService.assetEngines.containsKey(it.assetId) || !rulesService.assetEngines.get(it.assetId).deployments.containsKey(it.id) }) {
                Thread.sleep(100)
                i++
            }
            LOG.info("Asset rulesets are deployed")
        }

        if (assetProcessingService != null) {
            LOG.info("Waiting for the system to settle down")
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

    List<RealmRuleset> getRealmRulesets() {
        if (!container.hasService(RulesService.class)) {
            return Collections.emptyList()
        }
        RulesService rulesService = container.getService(RulesService.class)
        return rulesService.realmEngines.values().collect {it.deployments.values()}.flatten {it.ruleset}
    }

    List<AssetRuleset> getAssetRulesets() {
        if (!container.hasService(RulesService.class)) {
            return Collections.emptyList()
        }
        RulesService rulesService = container.getService(RulesService.class)
        return rulesService.realmEngines.values().collect {it.deployments.values()}.flatten {it.ruleset}
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
        return container.getService(PersistenceService.class).doReturningTransaction {
            TypedQuery<UserAssetLink> query = it.createQuery("select ua from UserAssetLink ua", UserAssetLink.class)
            return query.getResultList()
        }
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
            return MapAccess.getInteger(container.getConfig(), OR_WEBSERVER_LISTEN_PORT, 0)
        }
        return 0
    }

    boolean isContainerRunning() {
        container.running
    }

    ResteasyClientBuilder createClient() {
        ResteasyClientBuilder clientBuilder =
                new ResteasyClientBuilderImpl()
                        .connectTimeout(2, SECONDS)
                        .readTimeout(15, SECONDS)
                        .connectionPoolSize(10)
        return WebClient.registerDefaults(clientBuilder)
    }

    UriBuilder serverUri(int serverPort) {
        UriBuilder.fromUri("")
                .scheme("http").host("127.0.0.1").port(serverPort)
    }

    void stopContainer() {
        try {
            if (container != null) {
                container.stop()
            }
        } catch (Exception e) {
            LOG.warn("Failed to stop container", e)
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
}
