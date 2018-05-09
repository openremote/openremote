package org.openremote.test.rules.geofence

import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.BaseAssetQuery
import org.openremote.model.attribute.MetaItem
import org.openremote.model.rules.AssetRuleset
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import org.openremote.test.ManagerContainerTrait
import org.openremote.test.rules.BasicRulesImport
import spock.lang.Ignore
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.rules.RulesetDeployment.Status.DEPLOYED
import static org.openremote.manager.setup.builtin.ManagerDemoSetup.*

class BasicGeofenceProcessingTest extends Specification implements ManagerContainerTrait {

    @Ignore
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
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def geofenceAdapter = (ORConsoleGeofenceAssetAdapter) rulesService.geofenceAssetAdapters.get("ORConsole")

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
            def geofences = geofenceAdapter.getAssetGeofences(managerDemoSetup.consoleId)
            assert geofences.size() == 1
            assert geofences[0].radius == 100
            assert geofences[0].lat == 50
            assert geofences[0].lng == 100
            assert geofences[0].id == managerDemoSetup.consoleId + "_-174661203"
            assert geofences[0].postUrl == "/master/asset/location/" + managerDemoSetup.consoleId
        }

        when: "another asset's location attribute is marked as RULE_STATE"
        def lobby = assetStorageService.find(managerDemoSetup.lobbyId, true)
        lobby.getAttribute("location").get().addMeta(new MetaItem(AssetMeta.RULE_STATE))
        lobby = assetStorageService.merge(lobby)

        then: "the geofence adapter should be notified of the new lobby asset with a location predicate"
        conditions.eventually {
            assert geofenceChanges != null
            assert !geofenceInit
            assert geofenceChanges.size() == 1
            def lobbyGeofences = geofenceChanges.find({it.assetState.id == managerDemoSetup.lobbyId}).locationPredicates
            assert lobbyGeofences.size() == 1
            BaseAssetQuery.RadialLocationPredicate locPredicate = lobbyGeofences.find {
                it instanceof BaseAssetQuery.RadialLocationPredicate && it.centrePoint[0] == 100 && it.centrePoint[1] == 50 && it.radius == 100
            }
            assert locPredicate != null
        }

        when: "a new ruleset is deployed with multiple location predicate rules on the lobby asset"
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

        then: "the mock geofence adapter should have been notified of the new location predicates for the demo thing and lobby assets"
        conditions.eventually {
            assert geofenceChanges != null
            assert !geofenceInit
            assert geofenceChanges.size() == 2
            Set<BaseAssetQuery.LocationPredicate> thingGeofences = geofenceChanges.find {
                it.assetState.id == managerDemoSetup.thingId
            }.locationPredicates
            assert thingGeofences.size() == 3
            BaseAssetQuery.RadialLocationPredicate rad1Predicate = thingGeofences.find {
                it instanceof BaseAssetQuery.RadialLocationPredicate && it.centrePoint[0] == 100 && it.centrePoint[1] == 50 && it.radius == 100
            }
            BaseAssetQuery.RadialLocationPredicate rad2Predicate = thingGeofences.find {
                it instanceof BaseAssetQuery.RadialLocationPredicate && it.centrePoint[0] == 50 && it.centrePoint[1] == 0 && it.radius == 200
            }
            BaseAssetQuery.RectangularLocationPredicate rect1Predicate = thingGeofences.find {
                it instanceof BaseAssetQuery.RectangularLocationPredicate && it.centrePoint[0] == 75 && it.centrePoint[1] == 25 && it.lngMax == 100 && it.latMax == 50
            }
            assert rad1Predicate != null
            assert rad2Predicate != null
            assert rect1Predicate != null
            def lobbyGeofences = geofenceChanges.find({it.assetState.id == managerDemoSetup.lobbyId}).locationPredicates
            assert lobbyGeofences.size() == 3
            assert lobbyGeofences.any {it == rad1Predicate}
            assert lobbyGeofences.any {it == rad2Predicate}
            assert lobbyGeofences.any {it == rect1Predicate}
        }

        when: "a location predicate ruleset is removed"
        rulesetStorageService.delete(GlobalRuleset.class, rulesImport.globalRuleset3Id)

        then: "the mock geofence adapter should be notified that the demo thing and lobby asset's rules with location predicates have changed"
        conditions.eventually {
            assert geofenceChanges != null
            assert geofenceChanges.size() == 2
            Set<BaseAssetQuery.LocationPredicate> thingGeofences = geofenceChanges.find {
                it.assetState.id == managerDemoSetup.thingId
            }.locationPredicates
            assert thingGeofences.size() == 2
            BaseAssetQuery.RadialLocationPredicate rad2Predicate = thingGeofences.find {
                it instanceof BaseAssetQuery.RadialLocationPredicate && it.centrePoint[0] == 50 && it.centrePoint[1] == 0 && it.radius == 200
            }
            BaseAssetQuery.RectangularLocationPredicate rect1Predicate = thingGeofences.find {
                it instanceof BaseAssetQuery.RectangularLocationPredicate && it.centrePoint[0] == 75 && it.centrePoint[1] == 25 && it.lngMax == 100 && it.latMax == 50
            }
            assert rad2Predicate != null
            assert rect1Predicate != null
            def lobbyGeofences = geofenceChanges.find({it.assetState.id == managerDemoSetup.lobbyId}).locationPredicates
            assert lobbyGeofences.size() == 2
            assert lobbyGeofences.any {it == rad2Predicate}
            assert lobbyGeofences.any {it == rect1Predicate}
        }

        when: "the RULE_STATE meta is removed from the lobby asset's location attribute"
        lobby.getAttribute("location").get().getMeta().removeIf({it.name.orElse(null) == AssetMeta.RULE_STATE.urn})
        lobby = assetStorageService.merge(lobby)

        then: "the mock geofence adapter should be notified that the lobby asset no longer has any rules with location predicates"
        conditions.eventually {
            assert geofenceChanges != null
            assert !geofenceInit
            assert geofenceChanges.size() == 1
            def lobbyGeofences = geofenceChanges.find({it.assetState.id == managerDemoSetup.lobbyId}).locationPredicates
            assert lobbyGeofences.size() == 0
        }

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
