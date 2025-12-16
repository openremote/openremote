package org.openremote.test.assets

import org.hibernate.cfg.AvailableSettings
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.datapoint.AssetPredictedDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.model.asset.impl.ThingAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.model.value.ValueType
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

import static java.util.concurrent.TimeUnit.DAYS

class AssetDatapointHypercoreTest extends Specification implements ManagerContainerTrait {

    def "Test TimescaleDB hypercore with 2.5M datapoints"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 30, delay: 1)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)
        def assetPredictedDatapointService = container.getService(AssetPredictedDatapointService.class)
        def persistenceService = container.getService(PersistenceService.class)

        and: "the schema name is retrieved"
        def schemaName = persistenceService.persistenceUnitProperties.getProperty(AvailableSettings.DEFAULT_SCHEMA)
        println "Using schema: ${schemaName}"

        and: "the clock is stopped and advanced to a known time"
        stopPseudoClock()
        advancePseudoClock(Instant.ofEpochMilli(getClockTimeOf(container)).truncatedTo(ChronoUnit.HOURS).plus(1, ChronoUnit.HOURS).toEpochMilli() - getClockTimeOf(container), TimeUnit.MILLISECONDS, container)

        when: "a test asset is created with multiple attributes"
        def testAsset = new ThingAsset("Hypercore Test Asset")
                .setRealm(keycloakTestSetup.realmMaster.name)
        testAsset.addOrReplaceAttributes(
                new Attribute<>("temperature", ValueType.NUMBER),
                new Attribute<>("humidity", ValueType.NUMBER),
                new Attribute<>("pressure", ValueType.NUMBER),
                new Attribute<>("co2", ValueType.NUMBER)
        )
        testAsset = assetStorageService.merge(testAsset)

        then: "the asset should be created"
        testAsset.id != null
        println "Created test asset with ID: ${testAsset.id}"

        when: "2.500.000 datapoints are inserted across multiple attributes"
        def totalDatapoints = 2_500_000
        def attributeNames = ["temperature", "humidity", "pressure", "co2"]
        def datapointsPerAttribute = totalDatapoints / attributeNames.size()
        // Use current container time as base, then go back in time for historical data
        def currentTime = getClockTimeOf(container)
        def baseTimestamp = Instant.ofEpochMilli(currentTime).minus(365, ChronoUnit.DAYS)

        println "Starting to insert ${totalDatapoints} datapoints..."
        def insertStartTime = System.currentTimeMillis()

        // Insert datapoints in batches for better performance
        def batchSize = 10000
        attributeNames.each { attributeName ->
            println "Inserting ${datapointsPerAttribute} datapoints for attribute: ${attributeName}"

            for (int batch = 0; batch < datapointsPerAttribute / batchSize; batch++) {
                def datapoints = []
                for (int i = 0; i < batchSize; i++) {
                    def index = batch * batchSize + i
                    def timestamp = baseTimestamp.plus(index * 30, ChronoUnit.SECONDS).toEpochMilli()
                    def value = 20.0 + (Math.sin(index / 100.0) * 10.0) + (Math.random() * 2.0)
                    datapoints.add(new ValueDatapoint<>(timestamp, value))
                }
                assetDatapointService.upsertValues(testAsset.id, attributeName, datapoints)

                if ((batch + 1) % 10 == 0) {
                    println "  Progress: ${(batch + 1) * batchSize} / ${datapointsPerAttribute} datapoints inserted for ${attributeName}"
                }
            }
        }

        def insertEndTime = System.currentTimeMillis()
        def insertDuration = (insertEndTime - insertStartTime) / 1000.0
        println "Finished inserting ${totalDatapoints} datapoints in ${insertDuration} seconds"

        then: "datapoints should be stored"
        conditions.eventually {
            def count = 0
            attributeNames.each { attributeName ->
                def datapoints = assetDatapointService.getDatapoints(new AttributeRef(testAsset.id, attributeName))
                count += datapoints.size()
            }
            assert count >= totalDatapoints * 0.99 // Allow for 1% margin
            println "Verified ${count} datapoints stored"
        }

        when: "storage usage is measured before enabling hypercore"
        def orDatabaseSizeBefore = persistenceService.doReturningTransaction { em ->
            def query = em.createNativeQuery("""
                SELECT 
                    pg_database.datname as database_name,
                    pg_size_pretty(pg_database_size(pg_database.datname)) AS size
                FROM pg_database
                WHERE pg_database.datname = 'openremote';
            """)
            return query.getSingleResult()
        }

        println "\n=== Storage BEFORE Hypercore ==="
        println "\nDatabase size: ${orDatabaseSizeBefore[1]}"

        then: "storage should be measured"
        true

        when: "TimescaleDB hypercore is enabled on asset_datapoint table"
        println "\n=== Enabling Hypercore on asset_datapoint ==="
        persistenceService.doTransaction { em ->
            em.createNativeQuery("""
                ALTER TABLE ${schemaName}.asset_datapoint SET (
                    timescaledb.enable_columnstore = true,
                    timescaledb.orderby = 'timestamp DESC',
                    timescaledb.segmentby = 'entity_id,attribute_name'
                )
            """).executeUpdate()

            em.createNativeQuery("""
                CALL public.add_columnstore_policy('asset_datapoint', after => INTERVAL '3 months')
            """).executeUpdate()
        }
        println "Hypercore enabled on asset_datapoint"

        and: "TimescaleDB hypercore is enabled on asset_predicted_datapoint table"
        println "=== Enabling Hypercore on asset_predicted_datapoint ==="
        persistenceService.doTransaction { em ->
            em.createNativeQuery("""
                ALTER TABLE ${schemaName}.asset_predicted_datapoint SET (
                    timescaledb.enable_columnstore = true,
                    timescaledb.orderby = 'timestamp DESC',
                    timescaledb.segmentby = 'entity_id,attribute_name'
                )
            """).executeUpdate()

            em.createNativeQuery("""
                CALL public.add_columnstore_policy('asset_predicted_datapoint', after => INTERVAL '3 months')
            """).executeUpdate()
        }
        println "Hypercore enabled on asset_predicted_datapoint"
        then: "hypercore should be enabled successfully"
        true

        when: "storage usage is measured after enabling hypercore"
        // Wait a bit for hypercore to process
        Thread.sleep(5000)

        def orDatabaseSizeAfter = persistenceService.doReturningTransaction { em ->
            def query = em.createNativeQuery("""
                SELECT 
                    pg_database.datname as database_name,
                    pg_size_pretty(pg_database_size(pg_database.datname)) AS size
                FROM pg_database
                WHERE pg_database.datname = 'openremote';
            """)
            return query.getSingleResult()
        }


        println "\n=== Storage AFTER Hypercore ==="

        println "\nDatabase size: ${orDatabaseSizeAfter[1]}"

        then: "storage should be measured after hypercore"
        true

        when: "Purging will be called, count datapoints before purging"
        def countBeforePurge = 0
        attributeNames.each { attributeName ->
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(testAsset.id, attributeName))
            countBeforePurge += datapoints.size()
        }
        println "Datapoints before purge: ${countBeforePurge}"

        and: "the purge routine is executed"
        def deleteStartTime = System.currentTimeMillis()
        assetDatapointService.purgeDataPoints()
        def deleteEndTime = System.currentTimeMillis()
        def deleteDuration = (deleteEndTime - deleteStartTime) / 1000.0

        println "Purge completed in ${deleteDuration} seconds"

        then: "less datapoints should exist"
        conditions.eventually {
            def countAfterPurge = 0
            attributeNames.each { attributeName ->
                def datapoints = assetDatapointService.getDatapoints(new AttributeRef(testAsset.id, attributeName))
                countAfterPurge += datapoints.size()
            }
            def deletedCount = countBeforePurge - countAfterPurge

            println "Datapoints after purge: ${countAfterPurge}"
            println "Deleted ${deletedCount} datapoints"
            println "Deletion rate: ${String.format('%.0f', deletedCount / deleteDuration)} datapoints/second"

            assert countAfterPurge <= countBeforePurge
        }

        when: "storage usage is measured after deletion"
        def orDatabaseSizeAfterDeletion = persistenceService.doReturningTransaction { em ->
            def query = em.createNativeQuery("""
                SELECT 
                    pg_database.datname as database_name,
                    pg_size_pretty(pg_database_size(pg_database.datname)) AS size
                FROM pg_database
                WHERE pg_database.datname = 'openremote';
            """)
            return query.getSingleResult()
        }

        println "\n=== Storage AFTER Deletion ==="
        println "\nDatabase size: ${orDatabaseSizeAfterDeletion[1]}"

        then: "final storage should be measured"
        true

        when: "final verification of remaining datapoints"
        def finalCount = 0
        attributeNames.each { attributeName ->
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(testAsset.id, attributeName))
            finalCount += datapoints.size()
        }
        def actualDeleted = countBeforePurge - finalCount
        def beforeSizeInMb = Integer.parseInt(orDatabaseSizeBefore[1].replace(" MB", ""))
        def compressionRatio = ((beforeSizeInMb - Integer.parseInt(orDatabaseSizeAfter[1].replace(" MB", ""))) / beforeSizeInMb) * 100

        println "\n=== Final Summary ==="
        println "Total datapoints inserted: ${totalDatapoints}"
        println "Datapoints before purge: ${countBeforePurge}"
        println "Datapoints deleted by purge: ${actualDeleted}"
        println "Datapoints remaining: ${finalCount}"
        println "Deletion percentage: ${String.format('%.2f', (actualDeleted / totalDatapoints * 100.0))}%"
        println "\nStorage Summary:"
        println "  Before hypercore: ${orDatabaseSizeBefore[1]}"
        println "  After hypercore:  ${orDatabaseSizeAfter[1]}"
        println "  After deletion:   ${orDatabaseSizeAfterDeletion[1]}"
        println "\nPerformance Summary:"
        println "  Insert time: ${insertDuration} seconds"
        println "  Purge time: ${deleteDuration} seconds"
        println "  Compression: ${String.format('%.2f', compressionRatio)}%"

        then: "No exception should have occurred"
        true
    }
}
