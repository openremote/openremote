package org.openremote.test.assets

import net.fortuna.ical4j.model.Recur
import org.openremote.agent.protocol.simulator.SimulatorAgent
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.asset.AssetProcessingService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.setup.SetupService
import org.openremote.model.Constants
import org.openremote.test.setup.KeycloakTestSetup
import org.openremote.test.setup.ManagerTestSetup
import org.openremote.model.asset.agent.Agent
import org.openremote.model.asset.impl.BuildingAsset
import org.openremote.model.asset.impl.RoomAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.calendar.CalendarEvent
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.AssetQuery.OrderBy
import org.openremote.model.query.LogicGroup
import org.openremote.model.query.filter.*
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import static org.openremote.model.query.AssetQuery.*
import static org.openremote.model.query.AssetQuery.Access.PRIVATE
import static org.openremote.model.query.AssetQuery.Access.PROTECTED
import static org.openremote.model.query.AssetQuery.OrderBy.Property.CREATED_ON
import static org.openremote.model.query.AssetQuery.OrderBy.Property.NAME
import static org.openremote.model.value.MetaItemType.*
import static org.openremote.model.value.ValueType.CALENDAR_EVENT
import static org.openremote.model.value.ValueType.TIMESTAMP_ISO8601

class AssetQueryTest extends Specification implements ManagerContainerTrait {

    @Shared
    static ManagerTestSetup managerTestSetup
    @Shared
    static KeycloakTestSetup keycloakTestSetup
    @Shared
    static AssetStorageService assetStorageService
    @Shared
    static AssetProcessingService assetProcessingService
    @Shared
    static PersistenceService persistenceService

    def setupSpec() {
        given: "the server container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        assetStorageService = container.getService(AssetStorageService.class)
        assetProcessingService = container.getService(AssetProcessingService.class)
        persistenceService = container.getService(PersistenceService.class)
    }

    // TODO: Test attribute/meta mustnotexist
    // TODO: Test ID and user ID
    // TODO: Test multiple logic groups with different conditions
    // TODO: Test Array predicate

    def "Query assets 1"() {

        when: "an agent filtering query is executed"
        def assets = assetStorageService.findAll(
                new AssetQuery()
                        .types(Agent.class)
                        .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
        )

        then: "agent assets should be retrieved"
        assets.size() == 1
        assets[0] instanceof SimulatorAgent
        assets[0].id == managerTestSetup.agentId
        assets[0].name == "Demo Agent"
        assets[0].type == SimulatorAgent.DESCRIPTOR.name
        assets[0].parentId == managerTestSetup.lobbyId
        assets[0].realm == managerTestSetup.realmMasterName

        when: "a user filtering query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                        .userIds(keycloakTestSetup.testuser3Id)
        )

        then: "only the users assets should be retrieved"
        assets.size() == 6
        assets.get(0).id == managerTestSetup.apartment1Id
        assets.get(1).id == managerTestSetup.apartment1LivingroomId
        assets.get(1).getAttributes().size() == 14
        assets.get(1).getAttribute("motionSensor").isPresent()
        !assets.get(1).getAttribute("currentTemperature").get().getValue().isPresent()
        !assets.get(1).getAttribute("currentTemperature").get().meta.isEmpty()
        !assets.get(1).getAttribute("targetTemperature").get().getValue().isPresent()
        !assets.get(1).getAttribute("targetTemperature").get().meta.isEmpty()
        assets.get(2).id == managerTestSetup.apartment1KitchenId
        assets.get(3).id == managerTestSetup.apartment1HallwayId
        assets.get(4).id == managerTestSetup.apartment1Bedroom1Id
        assets.get(5).id == managerTestSetup.apartment1BathroomId

        when: "a user filtering query is executed that limits to protected attributes"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .userIds(keycloakTestSetup.testuser3Id)
                    .access(PROTECTED)
        )

        then: "only the users assets should be retrieved"
        assets.size() == 6
        assets.get(0).id == managerTestSetup.apartment1Id
        assets.get(1).id == managerTestSetup.apartment1LivingroomId
        assets.get(1).getAttributes().size() == 7
        !assets.get(1).getAttribute("motionSensor").isPresent()
        !assets.get(1).getAttribute("currentTemperature").get().getValue().isPresent()
        !assets.get(1).getAttribute("currentTemperature").get().meta.isEmpty()
        assets[1].getAttribute("currentTemperature").get().getMetaItem(LABEL).isPresent()
        assets[1].getAttribute("currentTemperature").get().getMetaValue(LABEL).orElse("") == "Current temperature"
        !assets.get(1).getAttribute("targetTemperature").get().getValue().isPresent()
        !assets.get(1).getAttribute("targetTemperature").get().meta.isEmpty()
        assets.get(2).id == managerTestSetup.apartment1KitchenId
        assets.get(3).id == managerTestSetup.apartment1HallwayId
        assets.get(4).id == managerTestSetup.apartment1Bedroom1Id
        assets.get(5).id == managerTestSetup.apartment1BathroomId

        when: "a query is executed that returns a protected attribute without any other meta items"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .ids(managerTestSetup.apartment2LivingroomId)
                    .access(PROTECTED)
        )

        then: "only one asset should be retrieved"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.apartment2LivingroomId
        assets.get(0).getAttributes().size() == 1
        assets.get(0).getAttribute("windowOpen").isPresent()
        !assets.get(0).getAttribute("windowOpen").flatMap{it.value}.orElse(false)
        !assets.get(0).getAttribute("windowOpen").get().getMeta().isEmpty()

        when: "a recursive query is executed for apartment 1 assets"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .ids(managerTestSetup.smartBuildingId)
                    .orderBy(new OrderBy(CREATED_ON))
                    .recursive(true)
                    .access(PROTECTED)
        )

        then: "result should contain only basic info and attribute names"
        assets.size() == 13
        assets.find {it.id == managerTestSetup.smartBuildingId}.name == "Smart building"
        def apartment1 = assets.find {it.id == managerTestSetup.apartment1Id}
        apartment1.name == "Apartment 1"
        apartment1.type == BuildingAsset.DESCRIPTOR.getName()
        apartment1.createdOn != null
        apartment1.parentId == managerTestSetup.smartBuildingId
        apartment1.realm == managerTestSetup.realmBuildingName
        apartment1.getAttributes().size() == 7
        apartment1.getAttribute("ventilationAuto").isPresent()
        apartment1.getAttribute("ventilationLevel").isPresent()
        apartment1.getAttribute("alarmEnabled").isPresent()
        apartment1.getAttribute("vacationUntil").isPresent()
        apartment1.getAttribute("lastExecutedScene").isPresent()
        apartment1.getAttribute("presenceDetected").isPresent()
        apartment1.getAttribute("location").isPresent()
        !apartment1.getAttribute("alarmEnabled").get().meta.isEmpty()
        assets.find {it.id == managerTestSetup.apartment1ServiceAgentId} != null
        assets.find {it.id == managerTestSetup.apartment1LivingroomId} != null
        assets.find {it.id == managerTestSetup.apartment1KitchenId} != null
        assets.find {it.id == managerTestSetup.apartment1HallwayId} != null
        assets.find {it.id == managerTestSetup.apartment1Bedroom1Id} != null
        assets.find {it.id == managerTestSetup.apartment1BathroomId} != null
        assets.find {it.id == managerTestSetup.apartment2Id} != null
        assets.find {it.id == managerTestSetup.apartment2LivingroomId} != null
        assets.find {it.id == managerTestSetup.apartment2BathroomId} != null
        assets.find {it.id == managerTestSetup.apartment3Id} != null
        assets.find {it.id == managerTestSetup.apartment3LivingroomId} != null

        when: "a query is executed that excludes attributes"
        def asset = assetStorageService.find(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .ids(managerTestSetup.smartOfficeId)
        )

        then: "result should match"
        asset.id == managerTestSetup.smartOfficeId
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Smart office"
        asset.type == BuildingAsset.DESCRIPTOR.getName()
        asset.parentId == null
        asset.realm == keycloakTestSetup.realmMaster.name
        asset.attributes.size() == 0

        when: "a query is executed"
        asset = assetStorageService.find(
                new AssetQuery()
                    .ids(managerTestSetup.smartOfficeId)
        )

        then: "result should match"
        asset.id == managerTestSetup.smartOfficeId
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Smart office"
        asset.type == BuildingAsset.DESCRIPTOR.getName()
        asset.parentId == null
        asset.realm == keycloakTestSetup.realmMaster.name
        asset.path.length == 1
        asset.path[0] == managerTestSetup.smartOfficeId
        asset.getAttribute(BuildingAsset.STREET).flatMap{it.value}.orElse("") == "Torenallee 20"

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .parents(new ParentPredicate(null))
                    .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.smartOfficeId
        assets.get(0).createdOn.time < System.currentTimeMillis()
        assets.get(0).name == "Smart office"
        assets.get(0).type == BuildingAsset.DESCRIPTOR.getName()
        assets.get(0).parentId == null
        assets.get(0).realm == keycloakTestSetup.realmMaster.name
        assets.get(0).attributes.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .parents(new ParentPredicate(null))
                    .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.smartOfficeId
        assets.get(0).createdOn.time < System.currentTimeMillis()
        assets.get(0).name == "Smart office"
        assets.get(0).type == BuildingAsset.DESCRIPTOR.getName()
        assets.get(0).parentId == null
        assets.get(0).realm == keycloakTestSetup.realmMaster.name
        assets.get(0).path.length == 1
        assets.get(0).path[0] == managerTestSetup.smartOfficeId
        ((BuildingAsset)assets.get(0)).getStreet().orElse("") == "Torenallee 20"

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .parents(new ParentPredicate(managerTestSetup.smartBuildingId))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerTestSetup.apartment1Id
        assets.get(0).createdOn.time < System.currentTimeMillis()
        assets.get(0).name == "Apartment 1"
        assets.get(0).type == BuildingAsset.DESCRIPTOR.getName()
        assets.get(0).parentId == managerTestSetup.smartBuildingId
        assets.get(0).realm == keycloakTestSetup.realmBuilding.name
        assets.get(0).attributes.size() == 0
        assets.get(1).id == managerTestSetup.apartment2Id
        assets.get(2).id == managerTestSetup.apartment3Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .parents(new ParentPredicate(managerTestSetup.smartBuildingId))
                    .realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerTestSetup.apartment1Id
        assets.get(1).id == managerTestSetup.apartment2Id
        assets.get(2).id == managerTestSetup.apartment3Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .parents(new ParentPredicate(managerTestSetup.smartBuildingId))
                    .realm(new RealmPredicate(keycloakTestSetup.realmBuilding.name))
                    .orderBy(new OrderBy(NAME, true))
        )

        then: "result should match"
        assets.size() == 3
        assets.get(0).id == managerTestSetup.apartment3Id
        assets.get(1).id == managerTestSetup.apartment2Id
        assets.get(2).id == managerTestSetup.apartment1Id

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .parents(new ParentPredicate(managerTestSetup.smartBuildingId))
                    .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
        )

        then: "result should match"
        assets.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .paths(new PathPredicate(assetStorageService.find(managerTestSetup.apartment1Id, true).getPath()))
        )

        then: "result should match"
        assets.size() == 7
        assets.find {it.id == managerTestSetup.apartment1Id} != null
        assets.find {it.id == managerTestSetup.apartment1ServiceAgentId} != null
        assets.find {it.id == managerTestSetup.apartment1LivingroomId} != null
        assets.find {it.id == managerTestSetup.apartment1KitchenId} != null
        assets.find {it.id == managerTestSetup.apartment1HallwayId} != null
        assets.find {it.id == managerTestSetup.apartment1Bedroom1Id} != null
        assets.find {it.id == managerTestSetup.apartment1BathroomId} != null

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .paths(new PathPredicate(assetStorageService.find(managerTestSetup.apartment1Id, true).getPath()))
                    .types(RoomAsset.class)
        )

        then: "result should match"
        assets.size() == 5
        assets.find {it.id == managerTestSetup.apartment1LivingroomId} != null
        assets.find {it.id == managerTestSetup.apartment1KitchenId} != null
        assets.find {it.id == managerTestSetup.apartment1HallwayId} != null
        assets.find {it.id == managerTestSetup.apartment1Bedroom1Id} != null
        assets.find {it.id == managerTestSetup.apartment1BathroomId} != null

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .userIds(keycloakTestSetup.testuser3Id)
                    .access(PROTECTED)
        )

        then: "result should match"
        assets.size() == 6
        assets.find {it.id == managerTestSetup.apartment1Id} != null
        def livingroom = assets.find {it.id == managerTestSetup.apartment1LivingroomId}
        livingroom != null
        livingroom.getAttributes().size() == 7
        !livingroom.getAttribute("currentTemperature").get().getValue().isPresent()
        !livingroom.getAttribute("currentTemperature").get().meta.isEmpty()
        !livingroom.getAttribute("targetTemperature").get().getValue().isPresent()
        !livingroom.getAttribute("targetTemperature").get().meta.isEmpty()
        assets.find {it.id == managerTestSetup.apartment1KitchenId} != null
        assets.find {it.id == managerTestSetup.apartment1HallwayId} != null
        assets.find {it.id == managerTestSetup.apartment1Bedroom1Id} != null
        assets.find {it.id == managerTestSetup.apartment1BathroomId} != null

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .userIds(keycloakTestSetup.testuser2Id)
                    .access(PROTECTED)
        )

        then: "result should match"
        assets.size() == 0

        expect: "a result to match the executed query "
        assetStorageService.isUserAsset(keycloakTestSetup.testuser3Id, managerTestSetup.apartment1Id)
        assetStorageService.isUserAsset(keycloakTestSetup.testuser3Id, managerTestSetup.apartment1LivingroomId)
        assetStorageService.isUserAsset(keycloakTestSetup.testuser3Id, managerTestSetup.apartment1HallwayId)
        assetStorageService.isUserAsset(keycloakTestSetup.testuser3Id, managerTestSetup.apartment1KitchenId)
        assetStorageService.isUserAsset(keycloakTestSetup.testuser3Id, managerTestSetup.apartment1Bedroom1Id)
        assetStorageService.isUserAsset(keycloakTestSetup.testuser3Id, managerTestSetup.apartment1BathroomId)
        !assetStorageService.isUserAsset(keycloakTestSetup.testuser3Id, managerTestSetup.apartment2Id)
        !assetStorageService.isUserAsset(keycloakTestSetup.testuser3Id, managerTestSetup.apartment3Id)
        !assetStorageService.isUserAsset(keycloakTestSetup.testuser3Id, managerTestSetup.smartOfficeId)
    }

    // Specs too large, split up features
    def "Query assets 2"() {

        when: "a query is executed"
        def assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .types(Agent.class)
        )

        then: "result should match"
        assets.size() == 3
        assets.find {it.id == managerTestSetup.agentId} != null
        assets.find {it.id == managerTestSetup.apartment1ServiceAgentId} != null
        assets.find {it.id == managerTestSetup.smartCityServiceAgentId} != null

        when: "a query is executed"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select().excludeAttributes())
                .types(ThingAsset.class)
                .parents(new ParentPredicate(managerTestSetup.agentId))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select().excludeAttributes())
                .types(ThingAsset)
                .parents(new ParentPredicate(managerTestSetup.agentId))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.thingId


        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                    .attributes(
                        new AttributePredicate().meta(
                            new NameValuePredicate(READ_ONLY, new BooleanPredicate(true))
                        )
                    )
        )

        then: "result should match"
        assets.size() == 2
        assets.any{it.id == managerTestSetup.agentId}
        assets.any{it.id == managerTestSetup.thingId}

        when: "a query is executed"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select().excludeAttributes())
                .attributes(
                    new AttributePredicate().meta(
                        new NameValuePredicate(UNITS.name, new ArrayPredicate(Constants.UNITS_KILO, 0, 3, null, null, false))
                    )
                )
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select().excludeAttributes())
                .attributes(new AttributePredicate().meta(new NameValuePredicate(AGENT_LINK, new StringPredicate(managerTestSetup.agentId), false, new NameValuePredicate.Path("id"))))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .attributes(new AttributePredicate().meta(new NameValuePredicate(AGENT_LINK, new StringPredicate(managerTestSetup.agentId), false, new NameValuePredicate.Path("id"))))
                    .names(new StringPredicate(Match.CONTAINS, false, "thing"))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .attributes(new AttributePredicate().meta(new NameValuePredicate(AGENT_LINK, new StringPredicate(managerTestSetup.agentId), false, new NameValuePredicate.Path("id"))))
                    .names(new StringPredicate(Match.CONTAINS, true, "thing"))
        )

        then: "result should match"
        assets.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .attributes(new AttributePredicate().meta(new NameValuePredicate(AGENT_LINK, new StringPredicate(managerTestSetup.agentId), false, new NameValuePredicate.Path("id"))))
                    .parents(new ParentPredicate(managerTestSetup.agentId))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.thingId

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .attributes(new AttributePredicate().meta(new NameValuePredicate(AGENT_LINK, new StringPredicate(managerTestSetup.agentId), false, new NameValuePredicate.Path("id"))))
                    .parents(new ParentPredicate(managerTestSetup.apartment1LivingroomId))
        )

        then: "result should match"
        assets.size() == 0

        when: "a query is executed"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .attributes(new AttributePredicate().meta(new NameValuePredicate(AGENT_LINK, new StringPredicate(managerTestSetup.agentId), false, new NameValuePredicate.Path("id"))))
                    .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
        )

        then: "result should match"
        assets.size() == 1
        assets.get(0).id == managerTestSetup.thingId

        when: "a query is executed to select a subset of attributes"
        def asset = assetStorageService.find(
                new AssetQuery()
                    .ids(managerTestSetup.apartment1LivingroomId)
                    .select(new Select().attributes("co2Level", "lastPresenceDetected", "motionSensor"))
                    .access(PRIVATE)
        )

        then: "result should match"
        asset.id == managerTestSetup.apartment1LivingroomId
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Living Room 1"
        asset.type == RoomAsset.DESCRIPTOR.getName()
        asset.parentId != null
        asset.realm == keycloakTestSetup.realmBuilding.name
        asset.path != null
        asset.getAttributes().size() == 3
        asset.getAttribute("co2Level").isPresent()
        !asset.getAttribute("co2Level").get().meta.isEmpty()
        asset.getAttribute("lastPresenceDetected").isPresent()
        !asset.getAttribute("lastPresenceDetected").get().meta.isEmpty()
        asset.getAttribute("motionSensor").isPresent()
        !asset.getAttribute("motionSensor").get().meta.isEmpty()

        when: "a query is executed to select a subset of protected attributes"
        asset = assetStorageService.find(
                new AssetQuery()
                        .ids(managerTestSetup.apartment1LivingroomId)
                        .select(new Select().attributes("co2Level", "lastPresenceDetected", "motionSensor"))
                        .access(PROTECTED)
        )

        then: "result should contain only matches that are protected"
        asset.id == managerTestSetup.apartment1LivingroomId
        asset.createdOn.time < System.currentTimeMillis()
        asset.name == "Living Room 1"
        asset.type == RoomAsset.DESCRIPTOR.getName()
        asset.parentId == managerTestSetup.apartment1Id
        asset.realm == keycloakTestSetup.realmBuilding.name
        asset.path != null
        asset.getAttributes().size() == 1
        asset.getAttribute("co2Level").isPresent()
        !asset.getAttribute("co2Level").get().meta.isEmpty()
        !asset.getAttribute("lastPresenceDetected").isPresent()
        !asset.getAttribute("motionSensor").isPresent()

        when: "a query is executed to select an asset with multiple attribute predicates 'ANDED'"
        asset = assetStorageService.find(
                new AssetQuery().attributes(
                        new AttributePredicate(
                                new StringPredicate("windowOpen"), new BooleanPredicate(false)
                        ),
                        new AttributePredicate(
                                new StringPredicate("co2Level"), new NumberPredicate(340, Operator.GREATER_THAN)
                        )
                )
        )

        then: "result should contain an Asset with the expected values"
        assert asset != null
        assert asset.getAttribute("windowOpen").isPresent()
        assert !asset.getAttribute("windowOpen").get().value.get()
        assert asset.getAttribute("co2Level").isPresent()
        assert asset.getAttribute("co2Level").get().value.get() == 350

        when: "a query is executed to select an asset with multiple attribute predicates 'ANDED' where one predicate is false"
        asset = assetStorageService.find(
            new AssetQuery().attributes(
                new AttributePredicate(
                    new StringPredicate("windowOpen"), new BooleanPredicate(false)
                ),
                new AttributePredicate(
                    new StringPredicate("co2Level"), new NumberPredicate(360, Operator.GREATER_THAN)
                )
            )
        )

        then: "no assets should match"
        assert asset == null
    }

    def "Query logic groups"() {

        when: "a query is executed to select an asset with and conditions on the same attribute"
        def asset = assetStorageService.find(
                new AssetQuery().attributes(
                        new AttributePredicate(
                                new StringPredicate("co2Level"), new NumberPredicate(360, Operator.LESS_EQUALS)
                        ),
                        new AttributePredicate(
                                new StringPredicate("co2Level"), new NumberPredicate(50, Operator.GREATER_THAN)
                        )
                )
        )

        then: "result should contain an Asset with the expected values"
        assert asset != null
        assert asset.getAttribute("windowOpen").isPresent()
        assert !asset.getAttribute("windowOpen").get().value.get()
        assert asset.getAttribute("co2Level").isPresent()
        assert asset.getAttribute("co2Level").get().value.get() == 350

        when: "a query is executed to select an asset with multiple logic groups"
        asset = assetStorageService.find(
                new AssetQuery().attributes(
                        new LogicGroup<AttributePredicate>(LogicGroup.Operator.AND, [
                                new LogicGroup<AttributePredicate>(LogicGroup.Operator.OR, [
                                        new AttributePredicate(
                                                new StringPredicate("co2Level"), new NumberPredicate(340, Operator.GREATER_THAN)
                                        ),
                                        new AttributePredicate(
                                                new StringPredicate("co2Level"), new NumberPredicate(50, Operator.LESS_THAN)
                                        )
                                ])
                        ], [
                                new AttributePredicate(
                                        new StringPredicate("windowOpen"), new BooleanPredicate(false)
                                )
                        ])
                )
        )

        then: "result should contain an Asset with the expected values"
        assert asset != null
        assert asset.getAttribute("windowOpen").isPresent()
        assert !asset.getAttribute("windowOpen").get().value.get()
        assert asset.getAttribute("co2Level").isPresent()
        assert asset.getAttribute("co2Level").get().value.get() == 350

        when: "a query is executed to select an asset with multiple logic groups and one of the groups is false"
        asset = assetStorageService.find(
                new AssetQuery().attributes(
                        new LogicGroup<AttributePredicate>(LogicGroup.Operator.AND, [
                                new LogicGroup<AttributePredicate>(LogicGroup.Operator.OR, [
                                        new AttributePredicate(
                                                new StringPredicate("co2Level"), new NumberPredicate(340, Operator.GREATER_THAN)
                                        ),
                                        new AttributePredicate(
                                                new StringPredicate("co2Level"), new NumberPredicate(50, Operator.LESS_THAN)
                                        )
                                ])
                        ], [
                                new AttributePredicate(
                                        new StringPredicate("windowOpen"), new BooleanPredicate(true)
                                )
                        ])
                )
        )

        then: "no assets should match"
        assert asset == null

        when: "a query is executed to select an asset with multiple logic groups where the other logic group is false"
        asset = assetStorageService.find(
                new AssetQuery().attributes(
                        new LogicGroup<AttributePredicate>(LogicGroup.Operator.AND, [
                                new LogicGroup(LogicGroup.Operator.OR, [
                                        new AttributePredicate(
                                                new StringPredicate("co2Level"), new NumberPredicate(360, Operator.GREATER_THAN)
                                        ),
                                        new AttributePredicate(
                                                new StringPredicate("co2Level"), new NumberPredicate(50, Operator.LESS_THAN)
                                        )
                                ])
                        ], [
                                new AttributePredicate(
                                        new StringPredicate("windowOpen"), new BooleanPredicate(false)
                                )
                        ])
                )
        )

        then: "no assets should match"
        assert asset == null
    }

    def "Location queries"() {

        given: "polling conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "a location filtering query is executed"
        def assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select().excludeAttributes())
                .attributes(new LocationAttributePredicate(new RadialGeofencePredicate(10, 51.44541688237109d, 5.460315214821094d)))
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .orderBy(new OrderBy(NAME))
        )

        then: "assets in the queried region should be retrieved"
        assets.size() == 5
        assets.find {it.id == managerTestSetup.agentId}.name == "Demo Agent"
        assets.find {it.id == managerTestSetup.thingId}.name == "Demo Thing"
        assets.find {it.id == managerTestSetup.groundFloorId}.name == "Ground floor"
        assets.find {it.id == managerTestSetup.lobbyId}.name == "Lobby"
        assets.find {it.id == managerTestSetup.smartOfficeId}.name == "Smart office"

        when: "one of the assets in the region is moved"
        def lobby = assetStorageService.find(managerTestSetup.lobbyId, true) as RoomAsset
        lobby.setLocation(new GeoJSONPoint(5.46108d, 51.44593d))
        lobby = assetStorageService.merge(lobby)

        then: "the system should settle down"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 100)
        }

        when: "the same query is run again"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select().excludeAttributes())
                .attributes(new LocationAttributePredicate(new RadialGeofencePredicate(10, 51.44541688237109d, 5.460315214821094d)))
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .orderBy(new OrderBy(NAME))
        )

        then: "the moved asset should not be included"
        assets.size() == 4
        !assets.any {it.name == "Lobby"}

        when: "a rectangular region is used that includes previous assets and moved asset"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select().excludeAttributes())
                .attributes(new LocationAttributePredicate(new RectangularGeofencePredicate(51.44540d, 5.46031d, 51.44594d, 5.46110d)))
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .orderBy(new OrderBy(NAME))
        )

        then: "all 5 assets should be returned"
        assets.size() == 5
        assets.find {it.id == managerTestSetup.agentId}.name == "Demo Agent"
        assets.find {it.id == managerTestSetup.thingId}.name == "Demo Thing"
        assets.find {it.id == managerTestSetup.groundFloorId}.name == "Ground floor"
        assets.find {it.id == managerTestSetup.lobbyId}.name == "Lobby"
        assets.find {it.id == managerTestSetup.smartOfficeId}.name == "Smart office"

        when: "a rectangular region is used that doesn't cover any assets"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select().excludeAttributes())
                .attributes(new LocationAttributePredicate(new RectangularGeofencePredicate(51.44542d,  5.46032d, 51.44590d, 5.46100d)))
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .orderBy(new OrderBy(NAME))
        )

        then: "no assets should be returned"
        assets.size() == 0

        when: "the same region is used but negated"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .select(new Select().excludeAttributes())
                .attributes(new LocationAttributePredicate(new RectangularGeofencePredicate(51.44542d,  5.46032d, 51.44590d, 5.46100d).negate()))
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .orderBy(new OrderBy(NAME))
        )

        then: "all 5 realm assets should be returned"
        assets.size() == 5
        assets.find {it.id == managerTestSetup.agentId}.name == "Demo Agent"
        assets.find {it.id == managerTestSetup.thingId}.name == "Demo Thing"
        assets.find {it.id == managerTestSetup.groundFloorId}.name == "Ground floor"
        assets.find {it.id == managerTestSetup.lobbyId}.name == "Lobby"
        assets.find {it.id == managerTestSetup.smartOfficeId}.name == "Smart office"
    }

    def "Calendar queries"() {
        given: "polling conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        when: "several assets are given a calendar event configuration attribute"
        def lobby = assetStorageService.find(managerTestSetup.lobbyId, true)
        def floor = assetStorageService.find(managerTestSetup.groundFloorId, true)
        def office = assetStorageService.find(managerTestSetup.smartOfficeId, true)
        def calendar = Calendar.getInstance(Locale.ROOT)
        calendar.setTimeInMillis(1517151600000) // 28/01/2018 @ 3:00pm (UTC)
        def start = calendar.getTime()
        calendar.add(Calendar.HOUR, 2)
        def end = calendar.getTime()
        def recur = new Recur(Recur.DAILY, 5)
        recur.setInterval(2)

        lobby.addAttributes(
            new Attribute<>("test", CALENDAR_EVENT, new CalendarEvent(start, end, recur))
        )
        lobby = assetStorageService.merge(lobby)

        recur = new Recur(Recur.DAILY, 3)
        recur.setInterval(2)

        floor.addAttributes(
            new Attribute<>("test", CALENDAR_EVENT, new CalendarEvent(start, end, recur))
        )
        floor = assetStorageService.merge(floor)
        office.addAttributes(
            new Attribute<>("test", CALENDAR_EVENT, new CalendarEvent(start, end))
        )
        office = assetStorageService.merge(office)

        then: "the system should settle down"
        conditions.eventually {
            assert noEventProcessedIn(assetProcessingService, 100)
        }

        when: "a calendar event filtering query is executed for the correct date and time of the event"
        def assets = assetStorageService.findAll(
            new AssetQuery()
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .attributes(new AttributePredicate(new StringPredicate("test"), new CalendarEventPredicate(new Date(1517155200000)))) // 28/01/2018 @ 4:00pm (UTC)
                .orderBy(new OrderBy(NAME))
        )

        then: "all 3 assets should be returned"
        assets.size() == 3
        assets.any {it.id == managerTestSetup.lobbyId}
        assets.any {it.name == "Lobby"}
        assets.any {it.id == managerTestSetup.groundFloorId}
        assets.any {it.name == "Ground floor"}
        assets.any {it.id == managerTestSetup.smartOfficeId}
        assets.any {it.name == "Smart office"}

        when: "a calendar event filtering query is executed for future event on a correct day but wrong time"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .attributes(new AttributePredicate(new StringPredicate("test"), new CalendarEventPredicate(new Date(1517335200000)))) // 30/01/2018 @ 6:00pm (UTC)
                .orderBy(new OrderBy(NAME))
        )

        then: "no assets should be returned"
        assets.size() == 0

        when: "a calendar event filtering query is executed for a future event on a wrong day but correct time"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .attributes(new AttributePredicate(new StringPredicate("test"), new CalendarEventPredicate(new Date(1517238600000)))) // 29/01/2018 @ 3:10pm (UTC)
                .orderBy(new OrderBy(NAME))
        )

        then: "no assets should be returned"
        assets.size() == 0

        when: "a calendar event filtering query is executed inside a valid future event date and time"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .attributes(new AttributePredicate(new StringPredicate("test"), new CalendarEventPredicate(new Date(1517503800000)))) // 01/02/2018 @ 4:50pm (UTC))
                .orderBy(new OrderBy(NAME))
        )

        then: "the lobby and ground floor assets should be returned"
        assets.size() == 2
        assets.any {it.id == managerTestSetup.lobbyId}
        assets.any {it.name == "Lobby"}
        assets.any {it.id == managerTestSetup.groundFloorId}
        assets.any {it.name == "Ground floor"}

        when: "a calendar event filtering query is executed inside a valid future event date and time of the lobby asset"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .attributes(new AttributePredicate(new StringPredicate("test"), new CalendarEventPredicate(new Date(1517849400000)))) // 05/02/2018 @ 4:50pm (UTC))
                .orderBy(new OrderBy(NAME))
        )

        then: "the lobby and ground floor assets should be returned"
        assets.size() == 1
        assets.any {it.id == managerTestSetup.lobbyId}
        assets.any {it.name == "Lobby"}

        when: "a calendar event filtering query is executed for some time after the last occurrence"
        assets = assetStorageService.findAll(
            new AssetQuery()
                .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                .attributes(new AttributePredicate(new StringPredicate("test"), new CalendarEventPredicate(new Date(1518017520000)))) // 02/07/2018 @ 3:32pm (UTC)
                .orderBy(new OrderBy(NAME))
        )

        then: "no assets should be returned"
        assets.size() == 0
    }

    def "DateTime queries"() {
        when: "the lobby has an opening date and the date falls is between the filtering date"
        def lobby = assetStorageService.find(managerTestSetup.lobbyId, true)
        lobby.addAttributes(
                new Attribute<>("openingDate", TIMESTAMP_ISO8601, ZonedDateTime.ofInstant(Instant.ofEpochMilli(1517151600000), ZoneOffset.UTC).format(ISO_ZONED_DATE_TIME)) // 28/01/2018 @ 2:00pm (UTC)
        )
        lobby = assetStorageService.merge(lobby)

        def rangeStart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1517151600000), ZoneOffset.UTC).minusHours(2) // 28/01/2018 @ 1:00pm (UTC)
        def rangeEnd = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1517151600000), ZoneOffset.UTC).plusHours(3) // 28/01/2018 @ 6:00pm (UTC)

        def assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                    .attributeValue(
                        "openingDate",
                        new DateTimePredicate(rangeStart.format(ISO_ZONED_DATE_TIME), rangeEnd.format(ISO_ZONED_DATE_TIME))
                            .operator(Operator.BETWEEN))
        )


        then: "the lobby asset should be retrieved"
        assets.size() == 1
        assets[0].id == lobby.id

        when: "the lobby has an opening date and the date is after the filtering date"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                    .attributeValue(
                        "openingDate",
                        new DateTimePredicate(Operator.GREATER_THAN, rangeStart.format(ISO_ZONED_DATE_TIME)))
        )


        then: "the lobby asset should be retrieved"
        assets.size() == 1
        assets[0].id == lobby.id

        when: "the lobby has an opening date and the date is before the filtering date"
        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                    .attributeValue(
                    "openingDate",
                    new DateTimePredicate(Operator.LESS_THAN, rangeEnd.format(ISO_ZONED_DATE_TIME)))
        )


        then: "the lobby asset should be retrieved"
        assets.size() == 1
        assets[0].id == lobby.id

        when: "the lobby has an opening date and the date is equal to the filtering date"
        rangeStart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(1517151600000), ZoneOffset.UTC) // 28/01/2018 @ 2:00pm (UTC)

        assets = assetStorageService.findAll(
                new AssetQuery()
                    .select(new Select().excludeAttributes())
                    .realm(new RealmPredicate(keycloakTestSetup.realmMaster.name))
                    .attributeValue(
                    "openingDate",
                    new DateTimePredicate(Operator.EQUALS, rangeStart.format(ISO_ZONED_DATE_TIME)))
        )


        then: "the lobby asset should be retrieved"
        assets.size() == 1
        assets[0].id == lobby.id
    }
}
