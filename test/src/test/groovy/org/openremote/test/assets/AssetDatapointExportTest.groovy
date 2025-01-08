package org.openremote.test.assets

import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.LightAsset
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.datapoint.DatapointQueryTooLargeException
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.LocalDateTime
import java.time.ZoneId

class AssetDatapointExportTest extends Specification implements ManagerContainerTrait {

    def "Test CSV export functionality for asset data points"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)
        assetDatapointService.datapointExportLimit = 1000

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
                        new ValueDatapoint<>(dateTime.minusMinutes(25).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 50d),
                        new ValueDatapoint<>(dateTime.minusMinutes(20).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 40d),
                        new ValueDatapoint<>(dateTime.minusMinutes(15).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 30d),
                        new ValueDatapoint<>(dateTime.minusMinutes(10).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 20d),
                        new ValueDatapoint<>(dateTime.minusMinutes(5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 10d),
                ]
        )

        then: "the CSV export should return a file"
        def csvExport1Future = assetDatapointService.exportDatapoints(
                [new AttributeRef(asset.id, attributeName)] as AttributeRef[],
                dateTime.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        assert csvExport1Future != null
        def csvExport1 = csvExport1Future.get()
        assert csvExport1 != null

        and: "the CSV export should contain all 5 data points"
        def csvExport1Lines = csvExport1.readLines()
        assert csvExport1Lines.size() == 6
        assert csvExport1Lines[1].endsWith("10.0")
        assert csvExport1Lines[2].endsWith("20.0")
        assert csvExport1Lines[3].endsWith("30.0")
        assert csvExport1Lines[4].endsWith("40.0")
        assert csvExport1Lines[5].endsWith("50.0")

        /* ------------------------- */

        when: "the limit of data export is lowered to 4"
        assetDatapointService.datapointExportLimit = 4

        and: "we try to export the same amount of data points"
        def csvExport2Future = assetDatapointService.exportDatapoints(
                [new AttributeRef(asset.id, attributeName)] as AttributeRef[],
                dateTime.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        then: "the CSV export should throw an exception"
        thrown(DatapointQueryTooLargeException)

        /* ------------------------- */

        cleanup: "Remove the limit on datapoint querying"
        assetDatapointService.datapointExportLimit = assetDatapointService.OR_DATA_POINTS_EXPORT_LIMIT_DEFAULT;
    }
}
