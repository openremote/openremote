package org.openremote.test.assets

import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.LightAsset
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.datapoint.DatapointExportFormat
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

        then: "the default CSV export should return a file"
        def inputStream1 = assetDatapointService.exportDatapoints(
                [new AttributeRef(asset.id, attributeName)] as AttributeRef[],
                dateTime.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        and: "the CSV export should contain all 5 data points"
        def csvExport1Lines = inputStream1.readLines()
        assert csvExport1Lines.size() == 6
        assert csvExport1Lines[1].endsWith("10.0")
        assert csvExport1Lines[2].endsWith("20.0")
        assert csvExport1Lines[3].endsWith("30.0")
        assert csvExport1Lines[4].endsWith("40.0")
        assert csvExport1Lines[5].endsWith("50.0")

        /* ------------------------- */

        when: "exporting with format CSV_CROSSTAB"
        def inputStream2 = assetDatapointService.exportDatapoints(
                [new AttributeRef(asset.id, attributeName)] as AttributeRef[],
                dateTime.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                DatapointExportFormat.CSV_CROSSTAB
        )

        then: "the crosstab CSV export should contain all 5 data points in columnar format"
        def csvExport2Lines = inputStream2.readLines()
        assert csvExport2Lines.size() == 6 // header + 5 data rows
        assert csvExport2Lines[0].contains("timestamp")
        assert csvExport2Lines[0].contains(assetName + ": " + attributeName)

        /* ------------------------- */

        when: "exporting with format CSV_CROSSTAB_MINUTE"
        def inputStream3 = assetDatapointService.exportDatapoints(
                [new AttributeRef(asset.id, attributeName)] as AttributeRef[],
                dateTime.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                DatapointExportFormat.CSV_CROSSTAB_MINUTE
        )

        then: "the time-bucketed CSV export should contain aggregated data with correct values"
        def csvExport3Lines = inputStream3.readLines()
        assert csvExport3Lines.size() >= 2 // header + at least 1 aggregated row
        assert csvExport3Lines[0].contains("timestamp")
        assert csvExport3Lines[0].contains(assetName + ": " + attributeName)

        and: "the aggregated values should be correct averages within 1-minute buckets"
        // Each datapoint is 5 minutes apart, so each should be in its own 1-minute bucket
        // Therefore we should have 5 rows of data (one per bucket)
        assert csvExport3Lines.size() == 6 // header + 5 buckets

        // Extract the values from the CSV (skip header, get last column which is the brightness value)
        def values = csvExport3Lines[1..5].collect { line ->
            def parts = line.split(',')
            parts.size() > 1 ? parts[1].trim() : null
        }.findAll { it != null && it != '' }

        // Should have 5 values, one per bucket, matching our input: 10, 20, 30, 40, 50
        assert values.size() == 5
        assert values.contains("10.000") || values.contains("10")
        assert values.contains("20.000") || values.contains("20")
        assert values.contains("30.000") || values.contains("30")
        assert values.contains("40.000") || values.contains("40")
        assert values.contains("50.000") || values.contains("50")
    }
}
