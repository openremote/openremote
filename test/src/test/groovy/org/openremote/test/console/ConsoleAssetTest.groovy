package org.openremote.test.console

import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.asset.console.ConsoleResourceImpl
import org.openremote.manager.rules.geofence.ORConsoleGeofenceAssetAdapter
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.Asset
import org.openremote.model.attribute.AttributeType
import org.openremote.model.console.ConsoleConfiguration
import org.openremote.model.console.ConsoleProvider
import org.openremote.model.console.ConsoleResource
import org.openremote.model.util.TextUtil
import org.openremote.model.value.ObjectValue
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import javax.ws.rs.WebApplicationException

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.Constants.MASTER_REALM
import static org.openremote.model.asset.AssetType.CONSOLE
import static org.openremote.model.value.Values.parse

class ConsoleAssetTest extends Specification implements ManagerContainerTrait {

    def "Console registration"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def assetStorageService = container.getService(AssetStorageService.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)

        and: "an authenticated admin user"
        def accessToken = authenticate(
            container,
            MASTER_REALM,
            KEYCLOAK_CLIENT_ID,
            "testuser1",
            "testuser1"
        ).token

        and: "the console resource"
        def consoleResource = getClientTarget(serverUri(serverPort), MASTER_REALM, accessToken).proxy(ConsoleResource.class)

        when: "a new console registers"
        def console = ConsoleConfiguration.initConsoleConfiguration(
            new Asset("Test Android Console", CONSOLE),
            "Test Android Console",
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
                    put("push", new ConsoleProvider(
                        "ORConsole",
                        true,
                        true,
                        false,
                        (ObjectValue)parse("{token: \"23123213ad2313b0897efd\"}").orElse(null)
                    ))
                }
            })
        def returnedConsole = consoleResource.register(null, console)
        def consoleId = returnedConsole.getId()

        then: "the returned console should have an id and match what was sent"
        assert ConsoleConfiguration.getConsoleName(console).orElse(null) == "Test Android Console"
        assert ConsoleConfiguration.getConsoleVersion(console).orElse(null) == "1.0"
        assert ConsoleConfiguration.getConsolePlatform(console).orElse(null) == "Android 7.1.2"
        def consoleGeofenceProvider = ConsoleConfiguration.getConsoleProvider(console, "geofence").orElse(null)
        def consolePushProvider = ConsoleConfiguration.getConsoleProvider(console, "push").orElse(null)
        assert consoleGeofenceProvider != null
        assert consoleGeofenceProvider.version == ORConsoleGeofenceAssetAdapter.NAME
        assert consoleGeofenceProvider.requiresPermission
        assert !consoleGeofenceProvider.hasPermission
        assert !consoleGeofenceProvider.disabled
        assert consoleGeofenceProvider.data == null
        assert consolePushProvider != null
        assert consolePushProvider.version == "ORConsole"
        assert consolePushProvider.requiresPermission
        assert consolePushProvider.hasPermission
        assert !consolePushProvider.disabled
        assert consolePushProvider.data.get("token").flatMap{Values.getString(it)}.orElse(null) == "23123213ad2313b0897efd"

        assert !TextUtil.isNullOrEmpty(returnedConsole.getId())
        assert returnedConsole.getParentId() == ConsoleResourceImpl.consoleParentAssetIdGenerator(MASTER_REALM)
        assert returnedConsole.name == console.name
        assert ConsoleConfiguration.getConsoleName(returnedConsole).orElse(null) == ConsoleConfiguration.getConsoleName(console).orElse(null)
        assert ConsoleConfiguration.getConsoleVersion(returnedConsole).orElse(null) == ConsoleConfiguration.getConsoleVersion(console).orElse(null)
        assert ConsoleConfiguration.getConsolePlatform(returnedConsole).orElse(null) == ConsoleConfiguration.getConsolePlatform(console).orElse(null)
        def returnedConsoleGeofenceProvider = ConsoleConfiguration.getConsoleProvider(returnedConsole, "geofence").orElse(null)
        def returnedConsolePushProvider = ConsoleConfiguration.getConsoleProvider(returnedConsole, "push").orElse(null)
        assert returnedConsoleGeofenceProvider != null
        assert returnedConsoleGeofenceProvider.version == ORConsoleGeofenceAssetAdapter.NAME
        assert returnedConsoleGeofenceProvider.requiresPermission
        assert !returnedConsoleGeofenceProvider.hasPermission
        assert !returnedConsoleGeofenceProvider.disabled
        assert returnedConsoleGeofenceProvider.data == null
        assert returnedConsolePushProvider != null
        assert returnedConsolePushProvider.version == "ORConsole"
        assert returnedConsolePushProvider.requiresPermission
        assert returnedConsolePushProvider.hasPermission
        assert !returnedConsolePushProvider.disabled
        assert returnedConsolePushProvider.data.get("token").flatMap{Values.getString(it)}.orElse(null) == "23123213ad2313b0897efd"

        and: "the console should have been linked to the authenticated user"
        def userAssets = assetStorageService.findUserAssets(MASTER_REALM, keycloakDemoSetup.testuser1Id, consoleId)
        assert userAssets.size() == 1
        assert userAssets.get(0).assetName == "Test Android Console"

        when: "the console is modified and registration is updated"
        returnedConsoleGeofenceProvider.hasPermission = true
        ConsoleConfiguration.addOrReplaceConsoleProvider(returnedConsole, "geofence", returnedConsoleGeofenceProvider)
        ConsoleConfiguration.addOrReplaceConsoleProvider(returnedConsole, "test", new ConsoleProvider(
            "Test 1.0",
            false,
            false,
            true,
            null
        ))
        returnedConsole = consoleResource.register(null, returnedConsole)

        then: "the returned console should contain the updated data and have the same id"
        assert returnedConsole.getId() == consoleId
        def returnedConsoleTestProvider = ConsoleConfiguration.getConsoleProvider(returnedConsole, "test").orElse(null)
        assert returnedConsoleTestProvider != null
        assert returnedConsoleTestProvider.version == "Test 1.0"
        assert !returnedConsoleTestProvider.requiresPermission
        assert !returnedConsoleTestProvider.hasPermission
        assert returnedConsoleTestProvider.disabled
        assert returnedConsoleTestProvider.data == null

        when: "a console registers with the id of another existing asset"
        returnedConsole.setId(managerDemoSetup.thingId)
        returnedConsole = consoleResource.register(null, returnedConsole)

        then: "the result should be bad request"
        WebApplicationException ex = thrown()
        ex.response.status == 400

        when: "a console registers with an id that doesn't exist"
        def unusedId = UniqueIdentifierGenerator.generateId("UnusedConsoleId")
        returnedConsole.setId(unusedId)
        returnedConsole = consoleResource.register(null, returnedConsole)
        returnedConsoleGeofenceProvider = ConsoleConfiguration.getConsoleProvider(returnedConsole, "geofence").orElse(null)
        returnedConsolePushProvider = ConsoleConfiguration.getConsoleProvider(returnedConsole, "push").orElse(null)
        returnedConsoleTestProvider = ConsoleConfiguration.getConsoleProvider(returnedConsole, "test").orElse(null)

        then: "the console should be registered successfully and the returned console should match the supplied console"
        assert unusedId.equals(returnedConsole.getId())
        assert returnedConsole.name == console.name
        assert ConsoleConfiguration.getConsoleName(returnedConsole).orElse(null) == ConsoleConfiguration.getConsoleName(console).orElse(null)
        assert ConsoleConfiguration.getConsoleVersion(returnedConsole).orElse(null) == ConsoleConfiguration.getConsoleVersion(console).orElse(null)
        assert ConsoleConfiguration.getConsolePlatform(returnedConsole).orElse(null) == ConsoleConfiguration.getConsolePlatform(console).orElse(null)
        assert returnedConsoleGeofenceProvider != null
        assert returnedConsoleGeofenceProvider.version == ORConsoleGeofenceAssetAdapter.NAME
        assert returnedConsoleGeofenceProvider.requiresPermission
        assert returnedConsoleGeofenceProvider.hasPermission
        assert !returnedConsoleGeofenceProvider.disabled
        assert returnedConsoleGeofenceProvider.data == null
        assert returnedConsolePushProvider != null
        assert returnedConsolePushProvider.version == "ORConsole"
        assert returnedConsolePushProvider.requiresPermission
        assert returnedConsolePushProvider.hasPermission
        assert !returnedConsolePushProvider.disabled
        assert returnedConsolePushProvider.data.get("token").flatMap{Values.getString(it)}.orElse(null) == "23123213ad2313b0897efd"
        assert returnedConsoleTestProvider != null
        assert returnedConsoleTestProvider.version == "Test 1.0"
        assert !returnedConsoleTestProvider.requiresPermission
        assert !returnedConsoleTestProvider.hasPermission
        assert returnedConsoleTestProvider.disabled
        assert returnedConsoleTestProvider.data == null

        when: "an invalid console is registered"
        returnedConsole.removeAttribute(AttributeType.CONSOLE_NAME)
        returnedConsole.setId(null)
        returnedConsole = consoleResource.register(null, returnedConsole)

        then: "the result should be bad request"
        ex = thrown()
        ex.response.status == 400

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
