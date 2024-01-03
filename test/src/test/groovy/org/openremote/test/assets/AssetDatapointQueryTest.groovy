package org.openremote.test.assets

import org.openremote.container.util.UniqueIdentifierGenerator
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.LightAsset
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.attribute.MetaItem
import org.openremote.model.datapoint.AssetDatapoint
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.model.datapoint.query.AssetDatapointIntervalQuery
import org.openremote.model.datapoint.query.AssetDatapointLTTBQuery
import org.openremote.model.geo.GeoJSONPoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.model.util.Pair
import org.openremote.model.value.MetaItemType
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.LocalDateTime

import static java.util.concurrent.TimeUnit.HOURS

class AssetDatapointQueryTest extends Specification implements ManagerContainerTrait {

    def "Test lttb datapoint query"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)


        when: "requesting the first light asset in City realm"
        def asset = assetStorageService.find(
                new AssetQuery()
                        .types(LightAsset.class)
                        .realm(new RealmPredicate(keycloakTestSetup.realmCity.name))
                        .names("Light 1")
        )

        then: "asset should exist and be correct"
        def assetName = "Light 1"
        def attributeName = "brightness"
        def dateTime = LocalDateTime.now()
        assert asset != null // light 1, 2 and 3
        assert asset.name == assetName
        assert asset.attributes.has(attributeName)

        and: "no datapoints should exist"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(asset.getId(), attributeName))
            assert datapoints.isEmpty()
        }


        /* ------------------------- */

        when: "datapoints are added to the asset"
        assetDatapointService.upsertValues(asset.getId(), attributeName,
            [
                new ValueDatapoint<>(dateTime.minusMinutes(25).toDate(), 50d),
                new ValueDatapoint<>(dateTime.minusMinutes(20).toDate(), 40d),
                new ValueDatapoint<>(dateTime.minusMinutes(15).toDate(), 30d),
                new ValueDatapoint<>(dateTime.minusMinutes(10).toDate(), 20d),
                new ValueDatapoint<>(dateTime.minusMinutes(5).toDate(), 10d),
            ]
        )

        then: "datapoints should exist"
        def allDatapoints = new ArrayList<ValueDatapoint>()
        conditions.eventually {
            allDatapoints = assetDatapointService.getDatapoints(new AttributeRef(asset.getId(), attributeName))
            assert allDatapoints.size() == 5
        }

        /* ------------------------- */

        and: "requesting 50 datapoints using the LTTB algorithm should return 5 values"
        def lttbDatapoints1 = assetDatapointService.queryDatapoints(
                asset.getId(),
                asset.getAttribute(attributeName).orElseThrow({ new RuntimeException("Missing attribute") }),
                new AssetDatapointLTTBQuery(dateTime.minusMinutes(30), dateTime, 5)
        )
        assert lttbDatapoints1.size() == 5

        and: "the 5 values are equal to 5 values in 'allDatapoints'"
        def index = 0
        Collections.reverse(allDatapoints) // since they're returned in reverse other (from new to old)
        allDatapoints.stream().forEach {dp -> {
            assert dp.timestamp == lttbDatapoints1[index].timestamp
            assert dp.value == lttbDatapoints1[index].value
            index++
        }}

        and: "requesting 3 datapoints using LTTB should return 50, 40 and 10 to reflect the algorithm spec."
        def lttbDatapoints2 = assetDatapointService.queryDatapoints(
                asset.getId(),
                asset.getAttribute(attributeName).orElseThrow({ new RuntimeException("Missing attribute") }),
                new AssetDatapointLTTBQuery(dateTime.minusMinutes(30), dateTime, 3)
        )
        assert lttbDatapoints2.size() == 3
        assert lttbDatapoints2[0].value == 50d
        assert lttbDatapoints2[1].value == 40d
        assert lttbDatapoints2[2].value == 10d


        /* ------------------------- */

        when: "the datapoints are cleared"
        assetDatapointService.purgeDataPoints()

        and: "datapoints are added that have a spike in value, it should be included in any downsample"
        assetDatapointService.upsertValues(asset.getId(), attributeName,
            [
                // placing them in a random order to verify order that is returned with the query
                new ValueDatapoint<>(dateTime.minusMinutes(10).toDate(), 25d),
                new ValueDatapoint<>(dateTime.minusMinutes(5).toDate(), 30d),
                new ValueDatapoint<>(dateTime.minusMinutes(30).toDate(), 10d),
                new ValueDatapoint<>(dateTime.minusMinutes(20).toDate(), 90d),
                new ValueDatapoint<>(dateTime.minusMinutes(25).toDate(), 15d),
                new ValueDatapoint<>(dateTime.minusMinutes(15).toDate(), 20d),
            ]
        )

        then: "the spike should be present as the 2nd value"
        def lttbDatapoints3 = assetDatapointService.queryDatapoints(
                asset.getId(),
                asset.getAttribute(attributeName).orElseThrow({ new RuntimeException("Missing attribute") }),
                new AssetDatapointLTTBQuery(dateTime.minusMinutes(60), dateTime, 4)
        )
        assert lttbDatapoints3.size() == 4
        assert lttbDatapoints3[0].value == 10d
        assert lttbDatapoints3[1].value == 90d
        assert lttbDatapoints3[2].value == 20d
        assert lttbDatapoints3[3].value == 30d

        and: "returned datapoints should be in chronological order" // so from earliest to most recent
        def index2 = 0
        lttbDatapoints3.toList().stream().forEach { dp -> {
            if(index2 > 0) {
                assert dp.timestamp > lttbDatapoints3[index2 - 1].timestamp
            }
            index2++
        }}
    }



    def "Lttb should not accept anything else than boolean and number"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)

        when: "a thing asset is added to the building realm"
        def thingId = UniqueIdentifierGenerator.generateId("TestThing")
        def thingAsset = new ThingAsset("TestThing")
                .setId(thingId)
                .setRealm(keycloakTestSetup.realmBuilding.name)
                .setLocation(new GeoJSONPoint(0, 0))
                .addAttributes(
                        new Attribute<>("status", ValueType.BOOLEAN, null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS)),
                        new Attribute<>("deviceId", ValueType.TEXT, null).addMeta(new MetaItem<>(MetaItemType.STORE_DATA_POINTS)),
                )
        thingAsset = assetStorageService.merge(thingAsset)

        and: "add datapoints to the attributes"
        assetDatapointService.upsertValue(thingAsset.getId(), "status", true, LocalDateTime.now())
        assetDatapointService.upsertValue(thingAsset.getId(), "deviceId", "abcdefghijklmnopqrstuvwxyz", LocalDateTime.now())


        then: "datapoints should exist"
        conditions.eventually {
            def booleanDatapoints = assetDatapointService.getDatapoints(new AttributeRef(thingAsset.getId(), "status"))
            assert booleanDatapoints.size() == 1
            def textDatapoints = assetDatapointService.getDatapoints(new AttributeRef(thingAsset.getId(), "deviceId"))
            assert textDatapoints.size() == 1
        }

        and: "lttbQuery with boolean should not throw exception"
        conditions.eventually {
            def lttbDatapoints = assetDatapointService.queryDatapoints(
                    thingAsset.getId(),
                    thingAsset.getAttribute("status").orElseThrow({ new RuntimeException("Missing attribute") }),
                    new AssetDatapointLTTBQuery(LocalDateTime.now().minusMinutes(5), LocalDateTime.now(), 3)
            )
            assert lttbDatapoints.size() == 1
            assert lttbDatapoints[0].value == 1.0 // Aka true as boolean
        }

        when: "lttbQuery is created with text, it should throw exception"
        assetDatapointService.queryDatapoints(
                thingAsset.getId(),
                thingAsset.getAttribute("deviceId").orElseThrow({ new RuntimeException("Missing attribute") }),
                new AssetDatapointLTTBQuery(LocalDateTime.now().minusMinutes(5), LocalDateTime.now(), 3)
        )

        then: "exception to be thrown"
        IllegalStateException ex = thrown(IllegalStateException)
        assert ex.getMessage() == "Query of type LTTB requires either a number or a boolean attribute."
    }



    def "Interval query should be returned in chronological order"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)


        when: "requesting the first light asset in City realm"
        def asset = assetStorageService.find(
                new AssetQuery()
                        .types(LightAsset.class)
                        .realm(new RealmPredicate(keycloakTestSetup.realmCity.name))
                        .names("Light 1")
        )

        then: "asset should exist and be correct"
        def assetName = "Light 1"
        def attributeName = "brightness"
        def dateTime = LocalDateTime.now()
        assert asset != null // light 1, 2 and 3
        assert asset.name == assetName
        assert asset.attributes.has(attributeName)

        and: "no datapoints should exist"
        conditions.eventually {
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(asset.getId(), attributeName))
            assert datapoints.isEmpty()
        }


        /* ------------------------- */

        when: "the first datapoints are added to the asset"
        assetDatapointService.upsertValue(asset.getId(), attributeName, 25d, dateTime.minusMinutes(10))
        assetDatapointService.upsertValue(asset.getId(), attributeName, 30d, dateTime.minusMinutes(5))
        assetDatapointService.upsertValue(asset.getId(), attributeName, 10d, dateTime.minusMinutes(30))

        and: "the clock advances by an hour"
        advancePseudoClock(1, HOURS, container)

        and: "the other datapoints are added to the asset, using an timestamp earlier than the previous ones"
        assetDatapointService.upsertValue(asset.getId(), attributeName, 90d, dateTime.minusMinutes(20))
        assetDatapointService.upsertValue(asset.getId(), attributeName, 15d, dateTime.minusMinutes(25))
        assetDatapointService.upsertValue(asset.getId(), attributeName, 20d, dateTime.minusMinutes(15))

        then: "datapoints should exist"
        def allDatapoints = new ArrayList<AssetDatapoint>()
        conditions.eventually {
            allDatapoints = assetDatapointService.getDatapoints(new AttributeRef(asset.getId(), attributeName))
            assert allDatapoints.size() == 6
        }

        /* ------------------------- */

        and: "returned datapoints (without gapfill) should be in chronological order" // so from earliest to most recent
        def intervalDatapoints1 = assetDatapointService.queryDatapoints(
                asset.getId(),
                asset.getAttribute(attributeName).orElseThrow({ new RuntimeException("Missing attribute") }),
                new AssetDatapointIntervalQuery(dateTime.minusMinutes(60), dateTime, "1 minute", AssetDatapointIntervalQuery.Formula.AVG, false)
        )
        assert intervalDatapoints1.size() == 6
        def index = 0
        intervalDatapoints1.toList().stream().forEach { dp -> {
            if(index > 0) {
                assert dp.timestamp > intervalDatapoints1[index - 1].timestamp
            }
            index++
        }}

        and: "returned datapoints (with gapfill) should be in chronological order" // so from earliest to most recent
        def intervalDatapoints2 = assetDatapointService.queryDatapoints(
                asset.getId(),
                asset.getAttribute(attributeName).orElseThrow({ new RuntimeException("Missing attribute") }),
                new AssetDatapointIntervalQuery(dateTime.minusMinutes(60), dateTime, "1 minute", AssetDatapointIntervalQuery.Formula.AVG, true)
        )
        assert intervalDatapoints2.size() == 61
        def index2 = 0
        intervalDatapoints2.toList().stream().forEach { dp -> {
            if(index2 > 0) {
                assert dp.timestamp > intervalDatapoints2[index2 - 1].timestamp
            }
            index2++
        }}

    }
}
