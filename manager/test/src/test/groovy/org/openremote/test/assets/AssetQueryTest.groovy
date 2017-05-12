package org.openremote.test.assets

import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.server.asset.AssetStorageService
import org.openremote.manager.server.asset.ServerAsset
import org.openremote.manager.server.setup.SetupService
import org.openremote.manager.server.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.server.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.AssetMeta
import org.openremote.model.asset.AssetQuery
import org.openremote.model.asset.AssetType
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification

import javax.persistence.EntityManager
import java.util.function.Function

import static org.openremote.model.asset.AssetQuery.*
import static org.openremote.model.asset.AssetQuery.OrderBy.Property.NAME
import static org.openremote.model.asset.AssetType.THING

class AssetQueryTest extends Specification implements ManagerContainerTrait {

    def "Query assets"() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        def container = startContainerNoDemoScenesOrRules(defaultConfig(serverPort), defaultServices())
        def managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        def keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def persistenceService = container.getService(PersistenceService.class)

        when: "an asset is loaded by identifier through JPA"
        def asset = persistenceService.doReturningTransaction(new Function<EntityManager, ServerAsset>() {
            @Override
            ServerAsset apply(EntityManager em) {
                em.find(ServerAsset.class, managerDemoSetup.apartment1Id)
            }
        })

        then: "result should match (and some values should be empty because we need an AssetQuery to get them)"
        asset.id == managerDemoSetup.apartment1Id
        asset.version == 1
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Apartment 1"
        asset.wellKnownType == AssetType.RESIDENCE
        asset.parentId == managerDemoSetup.smartHomeId
        asset.parentName == null
        asset.parentType == null
        asset.realmId == keycloakDemoSetup.customerATenant.id
        asset.tenantRealm == null
        asset.tenantDisplayName == null
        asset.coordinates.length == 2
        asset.path.length == 2
        asset.path[0] == managerDemoSetup.apartment1Id
        asset.path[1] == managerDemoSetup.smartHomeId
        asset.attributesList.size() > 0

        when: "a query is executed"
        asset = assetStorageService.find(
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
        asset.attributesList.size() == 0

        when: "a query is executed"
        asset = assetStorageService.find(
                new AssetQuery()
                        .select(new Select(true, false))
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
        asset.getAttribute("geoStreet").get().getValueAsString().get() == "Torenallee 20"

        when: "a query is executed"
        def assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new ParentPredicate(true))
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.id))
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
        assets.get(0).attributesList.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .select(new Select(true, false))
                        .parent(new ParentPredicate(true))
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.id))
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
        assets.get(0).getAttribute("geoStreet").get().getValueAsString().get() == "Torenallee 20"

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new ParentPredicate(managerDemoSetup.smartHomeId))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(0).version == 1
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
        assets.get(0).attributesList.size() == 0
        assets.get(1).id == managerDemoSetup.apartment2Id
        assets.get(2).id == managerDemoSetup.apartment3Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new ParentPredicate(managerDemoSetup.smartHomeId))
                        .tenant(new TenantPredicate(keycloakDemoSetup.customerATenant.id))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment2Id
        assets.get(2).id == managerDemoSetup.apartment3Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new ParentPredicate(managerDemoSetup.smartHomeId))
                        .tenant(new TenantPredicate(keycloakDemoSetup.customerATenant.id))
                        .orderBy(new OrderBy(NAME, true))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.apartment3Id
        assets.get(1).id == managerDemoSetup.apartment2Id
        assets.get(2).id == managerDemoSetup.apartment1Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new ParentPredicate(managerDemoSetup.smartHomeId))
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.id))
        )

        then: "result should match"
        assets.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .path(new PathPredicate(assetStorageService.find(managerDemoSetup.apartment1Id, true).getPath()))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment1LivingroomId
        assets.get(2).id == managerDemoSetup.apartment1ServiceAgentId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .path(new PathPredicate(assetStorageService.find(managerDemoSetup.apartment1Id, true).getPath()))
                        .type(AssetType.ROOM)
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.apartment1LivingroomId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().select(new Select(true, true)).userId(keycloakDemoSetup.testuser3Id)
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment1LivingroomId
        assets.get(1).getAttributesList().size() == 6
        !assets.get(1).getAttribute("currentTemperature").get().getValue().isPresent()
        assets.get(1).getAttribute("currentTemperature").get().meta.size() == 3
        !assets.get(1).getAttribute("targetTemperature").get().getValue().isPresent()
        assets.get(1).getAttribute("targetTemperature").get().meta.size() == 2
        assets.get(2).id == managerDemoSetup.apartment2Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().select(new Select(true, true)).userId(keycloakDemoSetup.testuser2Id)
        )

        then: "result should match"
        assets.size() == 0

        expect: "a result to match the executed query "
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1Id)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1LivingroomId)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment2Id)
        !assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment3Id)
        !assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.smartOfficeId)

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().type(AssetType.AGENT)
        )

        then: "result should match"
        assets.size() == 2
        assets.get(0).id == managerDemoSetup.agentId
        assets.get(1).id == managerDemoSetup.apartment1ServiceAgentId

        when: "a query is executed"
        assets = assetStorageService.findAll(new AssetQuery()
                .type(THING)
                .parent(new ParentPredicate(managerDemoSetup.agentId))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(new AssetQuery()
                .type(THING)
                .parent(new ParentPredicate().type(AssetType.AGENT))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId


        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.id))
                        .attributeMeta(new AttributeMetaPredicate(AssetMeta.STORE_DATA_POINTS, new BooleanPredicate(true)))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .attributeMeta(new AttributeMetaPredicate().itemValue(new StringPredicate(Match.END, "kWh")))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().attributeMeta(
                        new AttributeRefPredicate(
                                AssetMeta.AGENT_LINK,
                                managerDemoSetup.agentId,
                                managerDemoSetup.agentProtocolConfigName
                        )
                )
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().attributeMeta(
                        new AttributeRefPredicate(
                                AssetMeta.AGENT_LINK,
                                managerDemoSetup.agentId,
                                managerDemoSetup.agentProtocolConfigName
                        )
                ).name(new StringPredicate(Match.CONTAINS, false, "thing"))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().attributeMeta(
                        new AttributeRefPredicate(
                                AssetMeta.AGENT_LINK,
                                managerDemoSetup.agentId,
                                managerDemoSetup.agentProtocolConfigName
                        )
                ).name(new StringPredicate(Match.CONTAINS, true, "thing"))
        )

        then: "result should match"
        assets.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().attributeMeta(
                        new AttributeRefPredicate(
                                AssetMeta.AGENT_LINK,
                                managerDemoSetup.agentId,
                                managerDemoSetup.agentProtocolConfigName
                        )
                ).parent(new ParentPredicate(managerDemoSetup.agentId))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().attributeMeta(
                        new AttributeRefPredicate(
                                AssetMeta.AGENT_LINK,
                                managerDemoSetup.agentId,
                                managerDemoSetup.agentProtocolConfigName
                        )
                ).parent(new ParentPredicate(managerDemoSetup.apartment1LivingroomId))
        )

        then: "result should match"
        assets.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().attributeMeta(
                        new AttributeRefPredicate(
                                managerDemoSetup.agentId,
                                managerDemoSetup.agentProtocolConfigName
                        )
                ).tenant(new TenantPredicate().realm(keycloakDemoSetup.masterTenant.realm))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId

        cleanup: "the server should be stopped"
        stopContainer(container)
    }
}
