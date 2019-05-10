package org.openremote.test.assets

import org.openremote.container.Container
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.setup.builtin.KeycloakDemoSetup
import org.openremote.manager.setup.builtin.ManagerDemoSetup
import org.openremote.model.asset.*
import org.openremote.model.attribute.MetaItemType
import org.openremote.model.calendar.CalendarEvent
import org.openremote.model.calendar.RecurrenceRule
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.*
import org.openremote.model.value.Values
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.persistence.EntityManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.function.Function

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import static org.openremote.model.asset.AssetType.THING
import static org.openremote.model.attribute.AttributeValueType.DATETIME
import static org.openremote.model.query.BaseAssetQuery.*
import static org.openremote.model.query.BaseAssetQuery.Access.PRIVATE_READ
import static org.openremote.model.query.BaseAssetQuery.Access.RESTRICTED_READ
import static org.openremote.model.query.BaseAssetQuery.OrderBy.Property.CREATED_ON
import static org.openremote.model.query.BaseAssetQuery.OrderBy.Property.NAME

class AssetQueryTest extends Specification implements ManagerContainerTrait {

    @Shared
    static Container container
    @Shared
    static ManagerDemoSetup managerDemoSetup
    @Shared
    static KeycloakDemoSetup keycloakDemoSetup
    @Shared
    static AssetStorageService assetStorageService
    @Shared
    static AssetProcessingService assetProcessingService
    @Shared
    static PersistenceService persistenceService

    def setupSpec() {
        given: "the server container is started"
        def serverPort = findEphemeralPort()
        container = startContainer(defaultConfig(serverPort), defaultServices())
        managerDemoSetup = container.getService(SetupService.class).getTaskOfType(ManagerDemoSetup.class)
        keycloakDemoSetup = container.getService(SetupService.class).getTaskOfType(KeycloakDemoSetup.class)
        assetStorageService = container.getService(AssetStorageService.class)
        assetProcessingService = container.getService(AssetProcessingService.class)
        persistenceService = container.getService(PersistenceService.class)
    }

    def cleanupSpec() {
        given: "the server should be stopped"
        stopContainer(container)
    }

    def "Query assets 1"() {

        when: "an agent filtering query is executed"
        def assets = assetStorageService.findAll(
                new AssetQuery()
                        .select(new Select(Include.ONLY_ID_AND_NAME_AND_ATTRIBUTES))
                        .type(AssetType.AGENT)
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
        )

        then: "agent assets should be retrieved"
        assets.size() == 1
        assets[0].id == managerDemoSetup.agentId
        assets[0].name == "Demo Agent"
        assets[0].type == AssetType.AGENT.getType()
        assets[0].parentId == null
        assets[0].parentName == null
        assets[0].parentType == null
        assets[0].realm == null
        assets[0].path == null

        when: "a user filtering query is executed that returns only IDs, names and attribute names"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .select(new Select(Include.ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES))
                        .userId(keycloakDemoSetup.testuser3Id)
        )

        then: "only the users assets should be retrieved"
        assets.size() == 6
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment1LivingroomId
        assets.get(1).getAttributesList().size() == 11
        assets.get(1).getAttribute("motionSensor").isPresent()
        !assets.get(1).getAttribute("currentTemperature").get().getValue().isPresent()
        assets.get(1).getAttribute("currentTemperature").get().meta.size() == 1
        !assets.get(1).getAttribute("targetTemperature").get().getValue().isPresent()
        assets.get(1).getAttribute("targetTemperature").get().meta.size() == 1
        assets.get(2).id == managerDemoSetup.apartment1KitchenId
        assets.get(3).id == managerDemoSetup.apartment1HallwayId
        assets.get(4).id == managerDemoSetup.apartment1Bedroom1Id
        assets.get(5).id == managerDemoSetup.apartment1BathroomId

        when: "a user filtering query is executed that returns only IDs, names and attribute names and limits to protected attributes and meta"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .select(new Select(Include.ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES, RESTRICTED_READ))
                        .userId(keycloakDemoSetup.testuser3Id)
        )

        then: "only the users assets should be retrieved"
        assets.size() == 6
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment1LivingroomId
        assets.get(1).getAttributesList().size() == 7
        !assets.get(1).getAttribute("motionSensor").isPresent()
        !assets.get(1).getAttribute("currentTemperature").get().getValue().isPresent()
        assets.get(1).getAttribute("currentTemperature").get().meta.size() == 1
        !assets.get(1).getAttribute("targetTemperature").get().getValue().isPresent()
        assets.get(1).getAttribute("targetTemperature").get().meta.size() == 1
        assets.get(2).id == managerDemoSetup.apartment1KitchenId
        assets.get(3).id == managerDemoSetup.apartment1HallwayId
        assets.get(4).id == managerDemoSetup.apartment1Bedroom1Id
        assets.get(5).id == managerDemoSetup.apartment1BathroomId

        when: "a query is executed that returns a protected attribute without any other meta items"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .select(new Select(Include.ALL_EXCEPT_PATH, RESTRICTED_READ))
                        .id(managerDemoSetup.apartment2LivingroomId)
        )

        then: "only one asset should be retrieved"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.apartment2LivingroomId
        assets.get(0).getAttributesList().size() == 1
        assets.get(0).getAttribute("windowOpen").isPresent()
        !assets.get(0).getAttribute("windowOpen").get().getValueAsBoolean().get()
        !assets.get(0).getAttribute("windowOpen").get().hasMetaItems()

        when: "a recursive query is executed to select asset id, name and attribute names for apartment 1 assets"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .id(managerDemoSetup.smartBuildingId)
                        .select(new Select(Include.ONLY_ID_AND_NAME_AND_ATTRIBUTE_NAMES, true, RESTRICTED_READ))
                        .orderBy(new OrderBy(CREATED_ON))
        )

        then: "result should contain only ids, names and attribute names and label meta"
        assets.size() == 13
        assets[0].id == managerDemoSetup.smartBuildingId
        assets[0].name == "Smart Building"
        assets[1].id == managerDemoSetup.apartment1Id
        assets[1].createdOn == null
        assets[1].name == "Apartment 1"
        assets[1].type == AssetType.RESIDENCE.getType()
        assets[1].parentId == null
        assets[1].parentName == null
        assets[1].parentType == null
        assets[1].realm == null
        assets[1].path == null
        assets[1].getAttributesList().size() == 6
        assets[1].getAttribute("ventilationAuto").isPresent()
        assets[1].getAttribute("ventilationLevel").isPresent()
        assets[1].getAttribute("alarmEnabled").isPresent()
        assets[1].getAttribute("vacationUntil").isPresent()
        assets[1].getAttribute("lastExecutedScene").isPresent()
        assets[1].getAttribute("presenceDetected").isPresent()
        assets[1].getAttribute("alarmEnabled").get().meta.size() == 1
        assets[1].getAttribute("alarmEnabled").get().getMetaItem(MetaItemType.LABEL).isPresent()
        assets[1].getAttribute("alarmEnabled").get().getLabelOrName().get() == "Alarm enabled"
        assets[2].id == managerDemoSetup.apartment1ServiceAgentId
        assets[3].id == managerDemoSetup.apartment1LivingroomId
        assets[4].id == managerDemoSetup.apartment1KitchenId
        assets[5].id == managerDemoSetup.apartment1HallwayId
        assets[6].id == managerDemoSetup.apartment1Bedroom1Id
        assets[7].id == managerDemoSetup.apartment1BathroomId
        assets[8].id == managerDemoSetup.apartment2Id
        assets[9].id == managerDemoSetup.apartment2LivingroomId
        assets[10].id == managerDemoSetup.apartment2BathroomId
        assets[11].id == managerDemoSetup.apartment3Id
        assets[12].id == managerDemoSetup.apartment3LivingroomId

        when: "an asset is loaded by identifier through JPA"
        def asset = persistenceService.doReturningTransaction(new Function<EntityManager, Asset>() {
            @Override
            Asset apply(EntityManager em) {
                em.find(Asset.class, managerDemoSetup.apartment1Id)
            }
        })

        then: "result should match (and some values should be empty because we need an AssetQuery to get them)"
        asset.id == managerDemoSetup.apartment1Id
        asset.version == 1
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Apartment 1"
        asset.wellKnownType == AssetType.RESIDENCE
        asset.parentId == managerDemoSetup.smartBuildingId
        asset.parentName == null
        asset.parentType == null
        asset.realm == keycloakDemoSetup.tenantA.realm
        asset.path.length == 2
        asset.path[0] == managerDemoSetup.apartment1Id
        asset.path[1] == managerDemoSetup.smartBuildingId
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
        asset.realm == keycloakDemoSetup.masterTenant.realm
        asset.path == null
        asset.attributesList.size() == 0

        when: "a query is executed"
        asset = assetStorageService.find(
                new AssetQuery()
                        .select(new Select(Include.ALL))
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
        asset.realm == keycloakDemoSetup.masterTenant.realm
        asset.path.length == 1
        asset.path[0] == managerDemoSetup.smartOfficeId
        asset.getAttribute("geoStreet").get().getValueAsString().get() == "Torenallee 20"

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new ParentPredicate(true))
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
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
        assets.get(0).realm == keycloakDemoSetup.masterTenant.realm
        assets.get(0).path == null
        assets.get(0).attributesList.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .select(new Select(Include.ALL))
                        .parent(new ParentPredicate(true))
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
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
        assets.get(0).realm == keycloakDemoSetup.masterTenant.realm
        assets.get(0).path.length == 1
        assets.get(0).path[0] == managerDemoSetup.smartOfficeId
        assets.get(0).getAttribute("geoStreet").get().getValueAsString().get() == "Torenallee 20"

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new ParentPredicate(managerDemoSetup.smartBuildingId))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(0).createdOn.time < System.currentTimeMillis()
        assets.get(0).name == "Apartment 1"
        assets.get(0).wellKnownType == AssetType.RESIDENCE
        assets.get(0).parentId == managerDemoSetup.smartBuildingId
        assets.get(0).parentName == "Smart Building"
        assets.get(0).parentType == AssetType.BUILDING.type
        assets.get(0).realm == keycloakDemoSetup.tenantA.realm
        assets.get(0).path == null
        assets.get(0).attributesList.size() == 0
        assets.get(1).id == managerDemoSetup.apartment2Id
        assets.get(2).id == managerDemoSetup.apartment3Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new ParentPredicate(managerDemoSetup.smartBuildingId))
                        .tenant(new TenantPredicate(keycloakDemoSetup.tenantA.realm))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment2Id
        assets.get(2).id == managerDemoSetup.apartment3Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .parent(new ParentPredicate(managerDemoSetup.smartBuildingId))
                        .tenant(new TenantPredicate(keycloakDemoSetup.tenantA.realm))
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
                        .parent(new ParentPredicate(managerDemoSetup.smartBuildingId))
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
        )

        then: "result should match"
        assets.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .path(new PathPredicate(assetStorageService.find(managerDemoSetup.apartment1Id, true).getPath()))
        )

        then: "result should match"
        assets.size() == 7
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment1ServiceAgentId
        assets.get(2).id == managerDemoSetup.apartment1LivingroomId
        assets.get(3).id == managerDemoSetup.apartment1KitchenId
        assets.get(4).id == managerDemoSetup.apartment1HallwayId
        assets.get(5).id == managerDemoSetup.apartment1Bedroom1Id
        assets.get(6).id == managerDemoSetup.apartment1BathroomId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .path(new PathPredicate(assetStorageService.find(managerDemoSetup.apartment1Id, true).getPath()))
                        .type(AssetType.ROOM)
        )

        then: "result should match"
        assets.size() == 5
        assets.get(0).id == managerDemoSetup.apartment1LivingroomId
        assets.get(1).id == managerDemoSetup.apartment1KitchenId
        assets.get(2).id == managerDemoSetup.apartment1HallwayId
        assets.get(3).id == managerDemoSetup.apartment1Bedroom1Id
        assets.get(4).id == managerDemoSetup.apartment1BathroomId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().select(new Select(Include.ALL, RESTRICTED_READ)).userId(keycloakDemoSetup.testuser3Id)
        )

        then: "result should match"
        assets.size() == 6
        assets.get(0).id == managerDemoSetup.apartment1Id
        assets.get(1).id == managerDemoSetup.apartment1LivingroomId
        assets.get(1).getAttributesList().size() == 7
        !assets.get(1).getAttribute("currentTemperature").get().getValue().isPresent()
        assets.get(1).getAttribute("currentTemperature").get().meta.size() == 6
        !assets.get(1).getAttribute("targetTemperature").get().getValue().isPresent()
        assets.get(1).getAttribute("targetTemperature").get().meta.size() == 4
        assets.get(2).id == managerDemoSetup.apartment1KitchenId
        assets.get(3).id == managerDemoSetup.apartment1HallwayId
        assets.get(4).id == managerDemoSetup.apartment1Bedroom1Id
        assets.get(5).id == managerDemoSetup.apartment1BathroomId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery().select(new Select(Include.ALL, RESTRICTED_READ)).userId(keycloakDemoSetup.testuser2Id)
        )

        then: "result should match"
        assets.size() == 0

        expect: "a result to match the executed query "
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1Id)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1LivingroomId)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1HallwayId)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1KitchenId)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1Bedroom1Id)
        assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment1BathroomId)
        !assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment2Id)
        !assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.apartment3Id)
        !assetStorageService.isUserAsset(keycloakDemoSetup.testuser3Id, managerDemoSetup.smartOfficeId)
    }

    // Specs too large, split up features
    def "Query assets 2"() {

        when: "a query is executed"
        def assets = assetStorageService.findAll(
                new AssetQuery().type(AssetType.AGENT)
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerDemoSetup.agentId
        assets.get(1).id == managerDemoSetup.apartment1ServiceAgentId
        assets.get(2).id == managerDemoSetup.smartCityServiceAgentId

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
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .attributeMeta(new AttributeMetaPredicate(MetaItemType.STORE_DATA_POINTS, new BooleanPredicate(true)))
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
                                MetaItemType.AGENT_LINK,
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
                                MetaItemType.AGENT_LINK,
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
                                MetaItemType.AGENT_LINK,
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
                                MetaItemType.AGENT_LINK,
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
                                MetaItemType.AGENT_LINK,
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
                ).tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerDemoSetup.thingId

        when: "a query is executed to select a subset of attributes"
        def asset = assetStorageService.find(
                new AssetQuery()
                        .id(managerDemoSetup.apartment1LivingroomId)
                        .select(new Select(Include.ALL, false, PRIVATE_READ, "co2Level", "lastPresenceDetected", "motionSensor"))
        )

        then: "result should match"
        asset.id == managerDemoSetup.apartment1LivingroomId
        asset.version == 0
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Living Room"
        asset.wellKnownType == AssetType.ROOM
        asset.parentId != null
        asset.parentName == "Apartment 1"
        asset.parentType == AssetType.RESIDENCE.getType()
        asset.realm == keycloakDemoSetup.tenantA.realm
        asset.path != null
        asset.getAttributesList().size() == 3
        asset.getAttribute("co2Level").isPresent()
        asset.getAttribute("co2Level").get().meta.size() == 11
        asset.getAttribute("lastPresenceDetected").isPresent()
        asset.getAttribute("lastPresenceDetected").get().meta.size() == 3
        asset.getAttribute("motionSensor").isPresent()
        asset.getAttribute("motionSensor").get().meta.size() == 6

        when: "a query is executed to select a subset of protected attributes"
        asset = assetStorageService.find(
                new AssetQuery()
                        .id(managerDemoSetup.apartment1LivingroomId)
                        .select(new Select(Include.ALL, false, RESTRICTED_READ, "co2Level", "lastPresenceDetected", "motionSensor"))
        )

        then: "result should contain only matches that are protected"
        asset.id == managerDemoSetup.apartment1LivingroomId
        asset.version == 0
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Living Room"
        asset.wellKnownType == AssetType.ROOM
        asset.parentId != null
        asset.parentName == "Apartment 1"
        asset.parentType == AssetType.RESIDENCE.getType()
        asset.realm == keycloakDemoSetup.tenantA.realm
        asset.path != null
        asset.getAttributesList().size() == 1
        asset.getAttribute("co2Level").isPresent()
        asset.getAttribute("co2Level").get().meta.size() == 8
        !asset.getAttribute("lastPresenceDetected").isPresent()
        !asset.getAttribute("motionSensor").isPresent()

        when: "a query is executed to select an asset with an attribute of a certain value"
        asset = assetStorageService.find(
                new AssetQuery().select(new Select(Include.ONLY_ID_AND_NAME_AND_ATTRIBUTES)).attributes(
                        new AttributePredicate(
                                new StringPredicate("windowOpen"), new BooleanPredicate(false)
                        ),
                        new AttributePredicate(
                                new StringPredicate("co2Level"), new NumberPredicate(340, Operator.GREATER_THAN, NumberType.INTEGER)
                        )
                )
        )

        then: "result should contain an Asset with the expected values"
        assert asset != null
        assert asset.getAttribute("windowOpen").isPresent()
        assert !asset.getAttribute("windowOpen").get().valueAsBoolean.get()
        assert asset.getAttribute("co2Level").isPresent()
        assert asset.getAttribute("co2Level").get().valueAsNumber.get() == 350

    }

    def "Location queries"() {

        given: "polling conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        when: "a location filtering query is executed"
        def assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ONLY_ID_AND_NAME))
                .attributes(new LocationAttributePredicate(new RadialGeofencePredicate(10, 51.44541688237109d, 5.460315214821094d)))
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .orderBy(new OrderBy(NAME))
        )

        then: "assets in the queried region should be retrieved"
        assets.size() == 5
        assets[0].id == managerDemoSetup.agentId
        assets[0].name == "Demo Agent"
        assets[1].id == managerDemoSetup.thingId
        assets[1].name == "Demo Thing"
        assets[2].id == managerDemoSetup.groundFloorId
        assets[2].name == "Ground Floor"
        assets[3].id == managerDemoSetup.lobbyId
        assets[3].name == "Lobby"
        assets[4].id == managerDemoSetup.smartOfficeId
        assets[4].name == "Smart Office"

        when: "one of the assets in the region is moved"
        def lobby = assetStorageService.find(managerDemoSetup.lobbyId, true)
        lobby.setCoordinates(new GeoJSONPoint(5.46108d, 51.44593d))
        lobby = assetStorageService.merge(lobby)

        then: "the system should settle down"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 100)
        }

        when: "the same query is run again"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ONLY_ID_AND_NAME))
                .attributes(new LocationAttributePredicate(new RadialGeofencePredicate(10, 51.44541688237109d, 5.460315214821094d)))
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .orderBy(new OrderBy(NAME))
        )

        then: "the moved asset should not be included"
        assets.size() == 4
        !assets.any {it.name == "Lobby"}

        when: "a rectangular region is used that includes previous assets and moved asset"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ONLY_ID_AND_NAME))
                .attributes(new LocationAttributePredicate(new RectangularGeofencePredicate(51.44540d, 5.46031d, 51.44594d, 5.46110d)))
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .orderBy(new OrderBy(NAME))
        )

        then: "all 5 assets should be returned"
        assets.size() == 5
        assets[0].id == managerDemoSetup.agentId
        assets[0].name == "Demo Agent"
        assets[1].id == managerDemoSetup.thingId
        assets[1].name == "Demo Thing"
        assets[2].id == managerDemoSetup.groundFloorId
        assets[2].name == "Ground Floor"
        assets[3].id == managerDemoSetup.lobbyId
        assets[3].name == "Lobby"
        assets[4].id == managerDemoSetup.smartOfficeId
        assets[4].name == "Smart Office"

        when: "a rectangular region is used that doesn't cover any assets"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ONLY_ID_AND_NAME))
                .attributes(new LocationAttributePredicate(new RectangularGeofencePredicate(51.44542d,  5.46032d, 51.44590d, 5.46100d)))
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .orderBy(new OrderBy(NAME))
        )

        then: "no assets should be returned"
        assets.size() == 0

        when: "the same region is used but negated"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ONLY_ID_AND_NAME))
                .attributes(new LocationAttributePredicate(new RectangularGeofencePredicate(51.44542d,  5.46032d, 51.44590d, 5.46100d).negate()))
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .orderBy(new OrderBy(NAME))
        )

        then: "all 5 realm assets should be returned"
        assets.size() == 5
        assets[0].id == managerDemoSetup.agentId
        assets[0].name == "Demo Agent"
        assets[1].id == managerDemoSetup.thingId
        assets[1].name == "Demo Thing"
        assets[2].id == managerDemoSetup.groundFloorId
        assets[2].name == "Ground Floor"
        assets[3].id == managerDemoSetup.lobbyId
        assets[3].name == "Lobby"
        assets[4].id == managerDemoSetup.smartOfficeId
        assets[4].name == "Smart Office"
    }

    def "Calendar queries"() {
        given: "polling conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 1)

        when: "an asset is given a calendar event configuration attribute"
        def lobby = assetStorageService.find(managerDemoSetup.lobbyId, true)
        def calendar = Calendar.getInstance(Locale.ROOT);
        calendar.setTimeInMillis(1517151600000) // 28/01/2018 @ 3:00pm (UTC)
        def start = calendar.getTime()
        calendar.add(Calendar.HOUR, 2)
        def end = calendar.getTime()
        def recur = new RecurrenceRule(RecurrenceRule.Frequency.DAILY, 2, 5)

        lobby.addAttributes(
            CalendarEventConfiguration.toAttribute(new CalendarEvent(start, end, recur))
        )
        lobby = assetStorageService.merge(lobby)

        then: "the system should settle down"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 100)
        }

        when: "a calendar event filtering query is executed for the correct date and time of the event"
        def assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ALL_EXCEPT_PATH)) // Need attributes to do calendar filtering
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .calendarEventActive(1517155200) // 28/01/2018 @ 4:00pm (UTC)
                .orderBy(new OrderBy(NAME))
        )

        then: "all 5 realm assets should be returned"
        assets.size() == 5
        assets[0].id == managerDemoSetup.agentId
        assets[0].name == "Demo Agent"
        assets[1].id == managerDemoSetup.thingId
        assets[1].name == "Demo Thing"
        assets[2].id == managerDemoSetup.groundFloorId
        assets[2].name == "Ground Floor"
        assets[3].id == managerDemoSetup.lobbyId
        assets[3].name == "Lobby"
        assets[4].id == managerDemoSetup.smartOfficeId
        assets[4].name == "Smart Office"

        when: "a calendar event filtering query is executed for future event on a correct day but wrong time"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ALL_EXCEPT_PATH))
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .calendarEventActive(1517335200) // 30/01/2018 @ 6:00pm (UTC)
                .orderBy(new OrderBy(NAME))
        )

        then: "the calendar event asset should not be included"
        assets.size() == 4
        !assets.any {it.name == "Lobby"}

        when: "a calendar event filtering query is executed for a future event on a wrong day but correct time"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ALL_EXCEPT_PATH))
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .calendarEventActive(1517238600) // 29/01/2018 @ 3:10pm (UTC)
                .orderBy(new OrderBy(NAME))
        )

        then: "the calendar event asset should not be included"
        assets.size() == 4
        !assets.any {it.name == "Lobby"}

        when: "a calendar event filtering query is executed inside a valid future event date and time"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ALL_EXCEPT_PATH)) // Need attributes to do calendar filtering
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .calendarEventActive(1517849400) // 05/02/2018 @ 4:50pm (UTC))
                .orderBy(new OrderBy(NAME))
        )

        then: "all 5 realm assets should be returned"
        assets.size() == 5
        assets[0].id == managerDemoSetup.agentId
        assets[0].name == "Demo Agent"
        assets[1].id == managerDemoSetup.thingId
        assets[1].name == "Demo Thing"
        assets[2].id == managerDemoSetup.groundFloorId
        assets[2].name == "Ground Floor"
        assets[3].id == managerDemoSetup.lobbyId
        assets[3].name == "Lobby"
        assets[4].id == managerDemoSetup.smartOfficeId
        assets[4].name == "Smart Office"

        when: "a calendar event filtering query is executed for some time after the last occurrence"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select(Include.ALL_EXCEPT_PATH))
                .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                .calendarEventActive(1518017520) // 02/07/2018 @ 3:32pm (UTC)
                .orderBy(new OrderBy(NAME))
        )

        then: "the calendar event asset should not be included"
        assets.size() == 4
        !assets.any {it.name == "Lobby"}
    }

    def "DateTime queries"() {
        when: "the lobby has an opening date and the date falls is between the filtering date"
        def lobby = assetStorageService.find(managerDemoSetup.lobbyId, true)

        lobby.addAttributes(
                new AssetAttribute("openingDate", DATETIME, Values.create("2018-01-28T15:00:00"))
        )
        lobby = assetStorageService.merge(lobby)

        def rangeStart = LocalDateTime.ofInstant(Instant.ofEpochMilli(1517151600000), ZoneOffset.UTC).minusHours(2) // 28/01/2018 @ 1:00pm (UTC)
        def rangeEnd = LocalDateTime.ofInstant(Instant.ofEpochMilli(1517151600000), ZoneOffset.UTC).plusHours(3) // 28/01/2018 @ 6:00pm (UTC)

        def assets = assetStorageService.findAll(
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .attributeValue(
                            "openingDate",
                            new DateTimePredicate(rangeStart.format(ISO_LOCAL_DATE_TIME), rangeEnd.format(ISO_LOCAL_DATE_TIME))
                                .operator(Operator.BETWEEN))
        )


        then: "the lobby asset should be retrieved"
        assets.size() == 1
        assets[0].id == lobby.id

        when: "the lobby has an opening date and the date is after the filtering date"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .attributeValue(
                            "openingDate",
                            new DateTimePredicate(Operator.GREATER_THAN, rangeStart.format(ISO_LOCAL_DATE_TIME)))
        )


        then: "the lobby asset should be retrieved"
        assets.size() == 1
        assets[0].id == lobby.id

        when: "the lobby has an opening date and the date is before the filtering date"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .attributeValue(
                        "openingDate",
                        new DateTimePredicate(Operator.LESS_THAN, rangeEnd.format(ISO_LOCAL_DATE_TIME)))
        )


        then: "the lobby asset should be retrieved"
        assets.size() == 1
        assets[0].id == lobby.id

        when: "the lobby has an opening date and the date is equal to the filtering date"
        rangeStart = LocalDateTime.ofInstant(Instant.ofEpochMilli(1517151600000), ZoneOffset.UTC) // 28/01/2018 @ 2:00pm (UTC)

        assets = assetStorageService.findAll(
                new AssetQuery()
                        .tenant(new TenantPredicate(keycloakDemoSetup.masterTenant.realm))
                        .attributeValue(
                        "openingDate",
                        new DateTimePredicate(Operator.EQUALS, rangeStart.format(ISO_LOCAL_DATE_TIME)))
        )


        then: "the lobby asset should be retrieved"
        assets.size() == 1
        assets[0].id == lobby.id
    }
}
