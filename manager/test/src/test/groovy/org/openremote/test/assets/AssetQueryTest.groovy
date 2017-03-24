package org.openremote.test.assets

import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.model.Attributes
import org.openremote.model.asset.AssetQuery
import org.openremote.model.asset.AssetMeta
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import org.openremote.model.asset.AssetType

import static org.openremote.model.asset.AssetType.THING

class AssetQueryTest extends Specification implements ManagerContainerTrait {

    def "Query assets"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainer(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)

        when: "a query is executed"
        def asset = assetStorageService.find(
                new AssetQuery()
                        .id(managerDemoSetup.smartOfficeId)
        )

        then: "result should match"
        asset.id == managerDemoSetup.smartOfficeId
        asset.version == 0
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Smart Office"
        asset.wellKnownType == AssetType.BUILDING
        asset.parentId == null
        asset.parentName == null
        asset.parentType == null
        asset.realmId == keycloakDemoSetup.masterTenant.id
        asset.tenantRealm == keycloakDemoSetup.masterTenant.realm
        asset.tenantDisplayName == keycloakDemoSetup.masterTenant.displayName
        asset.coordinates.length == 2
        asset.path == null
        asset.attributes == null

        when: "a query is executed"
        asset = assetStorageService.find(
                new AssetQuery()
                        .select(new AssetQuery.Select(true, false))
                        .id(managerDemoSetup.smartOfficeId)
        )

        then: "result should match"
        asset.id == managerDemoSetup.smartOfficeId
        asset.version == 0
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Smart Office"
        asset.wellKnownType == AssetType.BUILDING
        asset.parentId == null
        asset.parentName == null
        asset.parentType == null
        asset.realmId == keycloakDemoSetup.masterTenant.id
        asset.tenantRealm == keycloakDemoSetup.masterTenant.realm
        asset.tenantDisplayName == keycloakDemoSetup.masterTenant.displayName
        asset.coordinates.length == 2
        asset.path.length == 1
        asset.path[0] == managerDemoSetup.smartOfficeId
        new Attributes(asset.attributes).get("geoStreet").valueAsString == "Torenallee 20"

        when: "a query is executed"
        def assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new AssetQuery.Parent(true))
                        .realm(new AssetQuery.Realm(keycloakDemoSetup.masterTenant.id))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.smartOfficeId
        assets.get(0).version == 0
        assets.get(0).createdOn.time < System.currentTimeMillis()
        assets.get(0).name == "Smart Office"
        assets.get(0).wellKnownType == AssetType.BUILDING
        assets.get(0).parentId == null
        assets.get(0).parentName == null
        assets.get(0).parentType == null
        assets.get(0).realmId == keycloakDemoSetup.masterTenant.id
        assets.get(0).tenantRealm == keycloakDemoSetup.masterTenant.realm
        assets.get(0).tenantDisplayName == keycloakDemoSetup.masterTenant.displayName
        assets.get(0).coordinates.length == 2
        assets.get(0).path == null
        assets.get(0).attributes == null

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .select(new AssetQuery.Select(true, false))
                        .parent(new AssetQuery.Parent(true))
                        .realm(new AssetQuery.Realm(keycloakDemoSetup.masterTenant.id))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.smartOfficeId
        assets.get(0).version == 0
        assets.get(0).createdOn.time < System.currentTimeMillis()
        assets.get(0).name == "Smart Office"
        assets.get(0).wellKnownType == AssetType.BUILDING
        assets.get(0).parentId == null
        assets.get(0).parentName == null
        assets.get(0).parentType == null
        assets.get(0).realmId == keycloakDemoSetup.masterTenant.id
        assets.get(0).tenantRealm == keycloakDemoSetup.masterTenant.realm
        assets.get(0).tenantDisplayName == keycloakDemoSetup.masterTenant.displayName
        assets.get(0).coordinates.length == 2
        assets.get(0).path.length == 1
        assets.get(0).path[0] == managerDemoSetup.smartOfficeId
        new Attributes(assets.get(0).attributes).get("geoStreet").valueAsString == "Torenallee 20"

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new AssetQuery.Parent(managerDemoSetup.smartHomeId))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(0).version == 0
        assets.get(0).createdOn.time < System.currentTimeMillis()
        assets.get(0).name == "Apartment 1"
        assets.get(0).wellKnownType == AssetType.RESIDENCE
        assets.get(0).parentId == managerDemoSetup.smartHomeId
        assets.get(0).parentName == "Smart Home"
        assets.get(0).parentType == AssetType.BUILDING.value
        assets.get(0).realmId == keycloakDemoSetup.customerATenant.id
        assets.get(0).tenantRealm == keycloakDemoSetup.customerATenant.realm
        assets.get(0).tenantDisplayName == keycloakDemoSetup.customerATenant.displayName
        assets.get(0).coordinates.length == 2
        assets.get(0).path == null
        assets.get(0).attributes == null
        assets.get(1).id == managerDemoSetup.apartment2Id
        assets.get(2).id == managerDemoSetup.apartment3Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new AssetQuery.Parent(managerDemoSetup.smartHomeId))
                        .realm(new AssetQuery.Realm(keycloakDemoSetup.customerATenant.id))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment2Id
        assets.get(2).id == managerDemoSetup.apartment3Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new AssetQuery.Parent(managerDemoSetup.smartHomeId))
                        .realm(new AssetQuery.Realm(keycloakDemoSetup.masterTenant.id))
        )

        then: "result should match"
        assets.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().select(new AssetQuery.Select(true, true)).userId(keycloakDemoSetup.testuser3Id)
        )

        then: "result should match"
        assets.size() == 4
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment1LivingroomId
        assets.get(2).id == managerDemoSetup.apartment1LivingroomThermostatId
        new Attributes(assets.get(2).attributes).size() == 1
        new Attributes(assets.get(2).attributes).get("currentTemperature").valueAsDecimal == 19.2d
        new Attributes(assets.get(2).attributes).get("currentTemperature").meta.size() == 2
        assets.get(3).id == managerDemoSetup.apartment2Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().select(new AssetQuery.Select(true, true)).userId(keycloakDemoSetup.testuser2Id)
        )

        then: "result should match"
        assets.size() == 0

        expect: "a result to match the executed query "
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1Id)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1LivingroomId)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1LivingroomThermostatId)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment2Id)
        !assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment3Id)
        !assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.smartOfficeId)

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().type(AssetType.AGENT)
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.demoAgentId

        when: "a query is executed"
        assets = assetStorageService.findAll(new AssetQuery()
                .type(THING)
                .parent(new AssetQuery.Parent(managerDemoSetup.demoAgentId))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId


        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .realm(new AssetQuery.Realm(keycloakDemoSetup.masterTenant.id))
                        .attributeMeta(new AssetQuery.AttributeMeta(AssetMeta.STORE_DATA_POINTS, new AssetQuery.BooleanSearch(true)))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
