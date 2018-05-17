package org.openremote.test.rules.geofence

import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.Asset
import org.openremote.model.asset.AssetAttribute
import org.openremote.model.asset.AssetMeta
import org.openremote.model.attribute.AttributeType
import org.openremote.model.attribute.MetaItem
import org.openremote.model.console.ConsoleConfiguration
import org.openremote.model.console.ConsoleProvider
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.rules.BasicRulesImport
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.rules.RulesetDeployment.Status.DEPLOYED
import static org.openremote.manager.setup.builtin.ManagerDemoSetup.*
import static org.openremote.model.asset.AssetMeta.RULE_STATE
import static org.openremote.model.asset.AssetType.CONSOLE

class BasicGeofenceProcessingTest extends Specification implements ManagerContainerTrait {

    def "Check tracking of location predicate rules"() {
        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, initialDelay: 0.5, delay: 0.5)

        and: "the container is started"
        def serverPort = findEphemeralPort()
        RulesService.GEOFENCE_PROCESSING_DEBOUNCE_MILLIS = 50
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def geofenceAdapter = (ORConsoleGeofenceAssetAdapter) rulesService.geofenceAssetAdapters.find {it.name == "ORConsole" }

        and: "some test rulesets have been imported"
        def rulesImport = new BasicRulesImport(rulesetStorageService, keycloakDemoSetup, managerDemoSetup)

        expect: "the rules engines to be ready"
        conditions.eventually {
            rulesImport.assertEnginesReady(rulesService, keycloakDemoSetup, managerDemoSetup)
        }

        and: "the demo attributes marked with RULE_STATE = true meta should be inserted into the engines"
        conditions.eventually {
            assert rulesService.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.globalEngine.assetStates.size() == DEMO_RULE_STATES_GLOBAL
            assert rulesImport.masterEngine.assetStates.size() == DEMO_RULE_STATES_SMART_OFFICE
            assert rulesImport.customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
            assert rulesImport.apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
            assert rulesImport.apartment3Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_3
        }

        and: "the geofence adapter should have been initialised with the demo consoles location predicate"
        conditions.eventually {
            def geofences = rulesService.getAssetGeofences(managerDemoSetup.consoleId)
            assert geofences.size() == 1
            assert geofences[0].radius == 100
            assert geofences[0].lat == 50
            assert geofences[0].lng == 100
            assert geofences[0].id == managerDemoSetup.consoleId + "_-174661203"
            assert geofences[0].postUrl == "/master/asset/public/${managerDemoSetup.consoleId}/updateLocation"
        }

        when: "another console asset is added with its location attribute marked as RULE_STATE and it has a CONSOLE_PROVIDER_GEOFENCE attribute"
        Asset console2 = ConsoleConfiguration.initConsoleConfiguration(
            new Asset("Demo Android Console 2", CONSOLE),
            "Demo Android Console",
            "1.0",
            "Android 7.1.2",
            new HashMap<String, ConsoleProvider>() {

                {
                    put("geofence", new ConsoleProvider(
                        ORConsoleGeofenceAssetAdapter.NAME,
                        true,
                        false,
                        false,
                        null
                    ))
                }
            })
                                             .addAttributes(
            new AssetAttribute(AttributeType.LOCATION.getName(),
                               AttributeType.LOCATION.getType())
                .setMeta(new MetaItem(RULE_STATE))
        )
        console2.setParentId(managerDemoSetup.lobbyId)
        console2 = assetStorageService.merge(console2)
        def console2Id = console2.getId()

        then: "the geofence adapter should be notified of the new console asset with the existing location predicate"
        conditions.eventually {
            def geofences = rulesService.getAssetGeofences(console2Id)
            assert geofences.size() == 1
            assert geofences[0].radius == 100
            assert geofences[0].lat == 50
            assert geofences[0].lng == 100
            assert geofences[0].id == console2Id + "_-174661203"
            assert geofences[0].postUrl == "/master/asset/public/${console2Id}/updateLocation"
        }

        when: "a new ruleset is deployed with multiple location predicate rules (including a duplicate and a rectangular predicate) on the lobby asset"
        def lobbyRuleset = new AssetRuleset(
            "Lobby location predicates",
            managerDemoSetup.lobbyId,
            getClass().getResource("/org/openremote/test/rules/BasicLocationPredicate2.groovy").text,
            Ruleset.Lang.GROOVY
        )
        rulesetStorageService.merge(lobbyRuleset)
        RulesEngine lobbyEngine = null

        then: "the new rule engine should be created and be running"
        conditions.eventually {
            lobbyEngine = rulesService.assetEngines.get(managerDemoSetup.lobbyId)
            assert lobbyEngine != null
            assert lobbyEngine.isRunning()
            assert lobbyEngine.deployments.size() == 1
            assert lobbyEngine.deployments.values().any({
                it.name == "Lobby location predicates" && it.status == DEPLOYED
            })
        }

        then: "the geofence adapter should have been notified of the new radial location predicates for the console assets (but shouldn't contain the rectangular or duplicate predicates)"
        conditions.eventually {
            def console1Geofences = rulesService.getAssetGeofences(managerDemoSetup.consoleId)
            def console2Geofences = rulesService.getAssetGeofences(console2Id)
            assert console1Geofences.size() == 1
            assert console2Geofences.size() == 2
            assert console1Geofences[0].radius == 100
            assert console1Geofences[0].lat == 50
            assert console1Geofences[0].lng == 100
            assert console1Geofences[0].id == managerDemoSetup.consoleId + "_-174661203"
            assert console1Geofences[0].postUrl == "/master/asset/public/${managerDemoSetup.consoleId}/updateLocation"

            def console2Geofence1 = console2Geofences.find {
                it.radius == 100 &&
                    it.lat == 50 &&
                    it.lng == 100 &&
                    it.id == console2Id + "_-174661203" &&
                    it.postUrl == "/master/asset/public/${console2Id}/updateLocation"
            }
            assert console2Geofence1 != null

            def console2Geofence2 = console2Geofences.find {
                it.radius == 50 &&
                    it.lat == 0 &&
                    it.lng == -60 &&
                    it.id == console2Id + "_-1397479941" &&
                    it.postUrl == "/master/asset/public/${console2Id}/updateLocation"
            }
            assert console2Geofence2 != null
        }

        when: "a location predicate ruleset is removed"
        rulesetStorageService.delete(GlobalRuleset.class, rulesImport.globalRuleset3Id)

        then: "the geofence adapter should be notified and update to reflect the changes"
        conditions.eventually {
            def console1Geofences = rulesService.getAssetGeofences(managerDemoSetup.consoleId)
            def console2Geofences = rulesService.getAssetGeofences(console2Id)
            assert console1Geofences.size() == 0
            assert console2Geofences.size() == 2

            def console2Geofence1 = console2Geofences.find {
                it.radius == 100 &&
                    it.lat == 50 &&
                    it.lng == 100 &&
                    it.id == console2Id + "_-174661203" &&
                    it.postUrl == "/master/asset/public/${console2Id}/updateLocation"
            }
            assert console2Geofence1 != null

            def console2Geofence2 = console2Geofences.find {
                it.radius == 50 &&
                    it.lat == 0 &&
                    it.lng == -60 &&
                    it.id == console2Id + "_-1397479941" &&
                    it.postUrl == "/master/asset/public/${console2Id}/updateLocation"
            }
            assert console2Geofence2 != null
        }

        when: "the RULE_STATE meta is removed from the second console asset's location attribute"
        console2.getAttribute(AttributeType.LOCATION.getName()).get().getMeta().removeIf({it.name.orElse(null) == AssetMeta.RULE_STATE.urn})
        console2 = assetStorageService.merge(console2)

        then: "the geofence adapter should be notified that the console asset no longer has any rules with location predicates"
        conditions.eventually {
            def console2Geofences = rulesService.getAssetGeofences(console2Id)
            assert console2Geofences.size() == 0
            assert geofenceAdapter.assetLocationPredicatesMap.isEmpty()
        }

        // TODO: Verify the RulesResource code path

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
