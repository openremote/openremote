package org.openremote.test.rules.residence


import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesService
import org.openremote.manager.rules.RulesetStorageService
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetResource
import org.openremote.model.console.ConsoleProvider
import org.openremote.model.console.ConsoleRegistration
import org.openremote.model.console.ConsoleResource
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.rules.Ruleset
import org.openremote.model.rules.TenantRuleset
import org.openremote.model.value.ObjectValue
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import static org.openremote.manager.setup.builtin.ManagerDemoSetup.DEMO_RULE_STATES_CUSTOMER_A
import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.attribute.AttributeType.LOCATION
import static org.openremote.model.value.Values.parse

class ResidenceLightsOnGeofenceTest extends Specification implements ManagerContainerTrait {

    def "Turn all lights off when console exits the residence geofence"() {

        given: "the container environment is started"
        def conditions = new PollingConditions(timeout: 10, delay: 1)
        def serverPort = findEphemeralPort()
        def container = startContainerWithPseudoClock(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def rulesService = container.getService(RulesService.class)
        def rulesetStorageService = container.getService(RulesetStorageService.class)
        def assetProcessingService = container.getService(AssetProcessingService.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        RulesEngine customerAEngine

        and: "some rules"
        Ruleset ruleset = new TenantRuleset(
                "Demo Apartment - All Lights Off",
                keycloakDemoSetup.customerATenant.id,
                getClass().getResource("/org/openremote/test/rules/BasicLocationPredicates.json").text,
                Ruleset.Lang.JSON
        )
        rulesetStorageService.merge(ruleset)

        expect: "the rule engines to become available and be running with asset states inserted"
        conditions.eventually {
            customerAEngine = rulesService.tenantEngines.get(keycloakDemoSetup.customerATenant.id)
            assert customerAEngine != null
            assert customerAEngine.isRunning()
            assert customerAEngine.assetStates.size() == DEMO_RULE_STATES_CUSTOMER_A
        }

        and: "the room lights in an apartment to be on"
        conditions.eventually {
            def livingroomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
            assert livingroomAsset.getAttribute("lightSwitch").get().valueAsBoolean.get()
            def bathRoomAsset = assetStorageService.find(managerDemoSetup.apartment2BathroomId, true)
            assert bathRoomAsset.getAttribute("lightSwitch").get().valueAsBoolean.get()
        }

        when: "a user authenticates"
        def accessToken = authenticate(
                container,
                keycloakDemoSetup.customerATenant.realm,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        ).token

        and: "a console is registered by that user"
        def authenticatedConsoleResource = getClientTarget(serverUri(serverPort), keycloakDemoSetup.customerATenant.realm, accessToken).proxy(ConsoleResource.class)
        def authenticatedAssetResource = getClientTarget(serverUri(serverPort), keycloakDemoSetup.customerATenant.realm, accessToken).proxy(AssetResource.class)
        def consoleRegistration = new ConsoleRegistration(null,
                "Test Console",
                "1.0",
                "Android 7.0",
                new HashMap<String, ConsoleProvider>() {
                    {
                        put("geofence", new ConsoleProvider(
                                ORConsoleGeofenceAssetAdapter.NAME,
                                true,
                                false,
                                false,
                                null
                        ))
                        put("push", new ConsoleProvider(
                                "fcm",
                                true,
                                true,
                                false,
                                (ObjectValue) parse("{token: \"23123213ad2313b0897efd\"}").orElse(null)
                        ))
                    }
                })
        consoleRegistration = authenticatedConsoleResource.register(null, consoleRegistration)

        then: "the console should have been registered"
        assert consoleRegistration.id != null

        when: "the console location is set to the apartment"
        authenticatedAssetResource.writeAttributeValue(null, consoleRegistration.id, LOCATION.name, ManagerDemoSetup.SMART_HOME_LOCATION.toValue().toJson())

        then: "the consoles location should have been updated"
        conditions.eventually {
            def testUser3Console = assetStorageService.find(consoleRegistration.id, true)
            assert testUser3Console != null
            def assetLocation = testUser3Console.getAttribute(LOCATION).flatMap { it.value }.flatMap {
                GeoJSONPoint.fromValue(it)
            }.orElse(null)
            assert assetLocation != null
            assert assetLocation.x == ManagerDemoSetup.SMART_HOME_LOCATION.x
            assert assetLocation.y == ManagerDemoSetup.SMART_HOME_LOCATION.y
            assert assetLocation.z == ManagerDemoSetup.SMART_HOME_LOCATION.z
        }

        then: "the console location asset state should be in the rule engine"
        conditions.eventually {
            assert customerAEngine.assetStates.find {
                it.id == consoleRegistration.id && it.value.flatMap { GeoJSONPoint.fromValue(it) }.map {
                    it.x == ManagerDemoSetup.SMART_HOME_LOCATION.x && it.y == ManagerDemoSetup.SMART_HOME_LOCATION.y ? it : null
                }.isPresent()
            } != null
        }

        when: "the console moves more than 50m away from the apartment"
        authenticatedAssetResource.writeAttributeValue(null, consoleRegistration.id, LOCATION.name, new GeoJSONPoint(0d,0d).toValue().toJson())

        then: ""

//        when: "the ALL LIGHTS OFF push-button is pressed for an apartment"
//        def lightsOffEvent = new AttributeEvent(
//                managerDemoSetup.apartment2Id, "allLightsOffSwitch", Values.create(true), getClockTimeOf(container)
//        )
//        assetProcessingService.sendAttributeEvent(lightsOffEvent)
//
//        then: "the room lights in the apartment should be off"
//        conditions.eventually {
//            assert apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
//            assert apartment2Engine.assetEvents.size() == 1
//            def livingroomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
//            assert !livingroomAsset.getAttribute("lightSwitch").get().valueAsBoolean.get()
//            def bathRoomAsset = assetStorageService.find(managerDemoSetup.apartment2BathroomId, true)
//            assert !bathRoomAsset.getAttribute("lightSwitch").get().valueAsBoolean.get()
//        }
//
//        when: "time advanced by 15 seconds"
//        advancePseudoClock(15, SECONDS, container)
//
//        and: "we turn the light on again in a room"
//        assetProcessingService.sendAttributeEvent(
//            new AttributeEvent(managerDemoSetup.apartment2LivingroomId, "lightSwitch", Values.create(true))
//        )
//
//        then: "the light should still be on after a few seconds (the all lights off event expires after 3 seconds)"
//        new PollingConditions(initialDelay: 3).eventually {
//            assert apartment2Engine.assetStates.size() == DEMO_RULE_STATES_APARTMENT_2
//            assert apartment2Engine.assetEvents.size() == 0
//            def livingroomAsset = assetStorageService.find(managerDemoSetup.apartment2LivingroomId, true)
//            assert livingroomAsset.getAttribute("lightSwitch").get().valueAsBoolean.get()
//        }

        cleanup: "stop the container"
        stopContainer(container)
    }
}
