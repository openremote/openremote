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
import static org.openremote.manager.datapoint.AssetDatapointService.OR_DATA_POINTS_MAX_AGE_WEEKS

class AssetDatapointPurgeTest extends Specification implements ManagerContainerTrait {

    def "Test TimescaleDB hypercore compression and purge"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 30, delay: 1)

        and: "the container is started with a 4-week retention period"
        def config = defaultConfig()
        config.put(OR_DATA_POINTS_MAX_AGE_WEEKS, "4")
        def container = startContainer(config, defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)
        def persistenceService = container.getService(PersistenceService.class)

        and: "the schema name is retrieved"
        def schemaName = persistenceService.persistenceUnitProperties.getProperty(AvailableSettings.DEFAULT_SCHEMA)
        getLOG().info("Using schema: ${schemaName}")

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
        getLOG().info("Created test asset with ID: ${testAsset.id}")

        when: "25.000 datapoints are inserted across multiple attributes with some within the 4-week retention window"
        def totalDatapoints = 25_000
        def attributeNames = ["temperature", "humidity", "pressure", "co2"]
        def datapointsPerAttribute = (totalDatapoints / attributeNames.size()) as int
        def recentDatapointsPerAttribute = (datapointsPerAttribute * 0.2) as int
        def oldDatapointsPerAttribute = datapointsPerAttribute - recentDatapointsPerAttribute
        def expectedRecentDatapoints = recentDatapointsPerAttribute * attributeNames.size()
        def currentTime = getClockTimeOf(container)
        def oldBaseTimestamp = Instant.ofEpochMilli(currentTime).minus(365, ChronoUnit.DAYS)
        def recentBaseTimestamp = Instant.ofEpochMilli(currentTime).minus(7, ChronoUnit.DAYS)

        getLOG().info("Starting to insert ${totalDatapoints} datapoints...")
        def insertStartTime = System.currentTimeMillis()

        // Insert datapoints in batches for better performance
        def batchSize = 10000
        attributeNames.each { attributeName ->
            getLOG().info("Inserting ${datapointsPerAttribute} datapoints for attribute: ${attributeName}")

            for (int batchStart = 0; batchStart < datapointsPerAttribute; batchStart += batchSize) {
                def batchEnd = Math.min(batchStart + batchSize, datapointsPerAttribute)
                def datapoints = []
                for (int index = batchStart; index < batchEnd; index++) {
                    def timestamp = index < oldDatapointsPerAttribute
                            ? oldBaseTimestamp.plus(index * 30, ChronoUnit.SECONDS).toEpochMilli()
                            : recentBaseTimestamp.plus((index - oldDatapointsPerAttribute) * 30, ChronoUnit.SECONDS).toEpochMilli()
                    def value = 20.0 + (Math.sin(index / 100.0) * 10.0) + (Math.random() * 2.0)
                    datapoints.add(new ValueDatapoint<>(timestamp, value))
                }
                assetDatapointService.upsertValues(testAsset.id, attributeName, datapoints)

                if (batchEnd % (batchSize * 10) == 0 || batchEnd == datapointsPerAttribute) {
                    getLOG().info("  Progress: ${batchEnd} / ${datapointsPerAttribute} datapoints inserted for ${attributeName}")
                }
            }
        }

        def insertEndTime = System.currentTimeMillis()
        def insertDuration = (insertEndTime - insertStartTime) / 1000.0
        getLOG().info("Finished inserting ${totalDatapoints} datapoints in ${insertDuration} seconds")

        then: "datapoints should be stored"
        conditions.eventually {
            def count = 0
            attributeNames.each { attributeName ->
                def datapoints = assetDatapointService.getDatapoints(new AttributeRef(testAsset.id, attributeName))
                count += datapoints.size()
            }
            assert count >= totalDatapoints * 0.99 // Allow for 1% margin
            getLOG().info("Verified ${count} datapoints stored")
        }

        when: "storage usage is measured before enabling hypercore"
        def orDatabaseSizeBefore = persistenceService.doReturningTransaction { em ->
            def query = em.createNativeQuery("""
                SELECT pg_database_size(pg_database.datname)
                FROM pg_database
                WHERE pg_database.datname = 'openremote';
            """)
            return query.getSingleResult() as Long
        }

        getLOG().info("\n=== Storage BEFORE Hypercore ===")
        getLOG().info("\nDatabase size: ${String.format('%.2f MB', orDatabaseSizeBefore / (1024.0 * 1024.0))}")

        and: "compression job is called manually"
        def job_id = persistenceService.doReturningTransaction { em ->
            def query = em.createNativeQuery("""
                SELECT job_id
                FROM timescaledb_information.jobs 
                WHERE proc_name = 'policy_compression' AND hypertable_name = 'asset_datapoint';
            """)
            return query.getSingleResult()
        }

        // run_job contains internal COMMIT statements, so it must be executed outside of a Hibernate transaction
        def dataSource = persistenceService.persistenceUnitProperties.get(AvailableSettings.DATASOURCE) as javax.sql.DataSource
        def connection = dataSource.getConnection()
        try {
            connection.setAutoCommit(true)
            def stmt = connection.prepareCall("CALL public.run_job(?)")
            stmt.setInt(1, job_id as int)
            stmt.execute()
            stmt.close()
        } finally {
            connection.close()
        }

        def orDatabaseSizeAfter = persistenceService.doReturningTransaction { em ->
            def query = em.createNativeQuery("""
                SELECT pg_database_size(pg_database.datname)
                FROM pg_database
                WHERE pg_database.datname = 'openremote';
            """)
            return query.getSingleResult() as Long
        }

        getLOG().info("\n=== Storage AFTER Hypercore ===")
        getLOG().info("\nDatabase size: ${String.format('%.2f MB', orDatabaseSizeAfter / (1024.0 * 1024.0))}")

        then: "storage should be smaller after hypercore"
        orDatabaseSizeBefore > orDatabaseSizeAfter

        when: "Purging will be called, count datapoints before purging"
        def countBeforePurge = 0
        attributeNames.each { attributeName ->
            def dataPoints = assetDatapointService.getDatapoints(new AttributeRef(testAsset.id, attributeName))
            countBeforePurge += dataPoints.size()
        }
        getLOG().info("Datapoints before purge: ${countBeforePurge}")

        and: "the purge routine is executed"
        def deleteStartTime = System.currentTimeMillis()
        assetDatapointService.purgeDataPoints()
        def deleteEndTime = System.currentTimeMillis()
        def deleteDuration = (deleteEndTime - deleteStartTime) / 1000.0

        getLOG().info("Purge completed in ${deleteDuration} seconds")

        then: "data points beyond the 4-week retention window should be purged via drop_chunks"
        conditions.eventually {
            def countAfterPurge = 0
            attributeNames.each { attributeName ->
                def dataPoints = assetDatapointService.getDatapoints(new AttributeRef(testAsset.id, attributeName))
                countAfterPurge += dataPoints.size()
            }
            def deletedCount = countBeforePurge - countAfterPurge

            getLOG().info("Datapoints after purge: ${countAfterPurge}")
            getLOG().info("Deleted ${deletedCount} datapoints")
            getLOG().info("Deletion rate: ${String.format('%.0f', deletedCount / deleteDuration)} datapoints/second")

            // With 365 days of data and 4-week (28-day) retention, most data should be purged
            assert countAfterPurge < countBeforePurge
            assert countAfterPurge >= expectedRecentDatapoints
            assert deletedCount > 0
        }

        when: "storage usage is measured after deletion"
        def orDatabaseSizeAfterDeletion = persistenceService.doReturningTransaction { em ->
            def query = em.createNativeQuery("""
                SELECT pg_database_size(pg_database.datname)
                FROM pg_database
                WHERE pg_database.datname = 'openremote';
            """)
            return query.getSingleResult() as Long
        }

        getLOG().info("\n=== Storage AFTER Deletion ===")
        getLOG().info("\nDatabase size: ${String.format('%.2f MB', orDatabaseSizeAfterDeletion / (1024.0 * 1024.0))}")

        then: "final storage should be measured"
        true

        when: "final verification of remaining datapoints"
        def finalCount = 0
        attributeNames.each { attributeName ->
            def datapoints = assetDatapointService.getDatapoints(new AttributeRef(testAsset.id, attributeName))
            finalCount += datapoints.size()
        }
        def actualDeleted = countBeforePurge - finalCount
        def compressionRatio = ((orDatabaseSizeBefore - orDatabaseSizeAfter) / (double) orDatabaseSizeBefore) * 100

        getLOG().info("\n=== Final Summary ===")
        getLOG().info("Total datapoints inserted: ${totalDatapoints}")
        getLOG().info("Datapoints before purge: ${countBeforePurge}")
        getLOG().info("Datapoints deleted by purge: ${actualDeleted}")
        getLOG().info("Datapoints remaining: ${finalCount}")
        getLOG().info("Deletion percentage: ${String.format('%.2f', (actualDeleted / totalDatapoints * 100.0))}%")
        getLOG().info("\nStorage Summary:")
        getLOG().info("  Before hypercore: ${String.format('%.2f MB', orDatabaseSizeBefore / (1024.0 * 1024.0))}")
        getLOG().info("  After hypercore:  ${String.format('%.2f MB', orDatabaseSizeAfter / (1024.0 * 1024.0))}")
        getLOG().info("  After deletion:   ${String.format('%.2f MB', orDatabaseSizeAfterDeletion / (1024.0 * 1024.0))}")
        getLOG().info("\nPerformance Summary:")
        getLOG().info("  Insert time: ${insertDuration} seconds")
        getLOG().info("  Purge time: ${deleteDuration} seconds")
        getLOG().info("  Compression: ${String.format('%.2f', compressionRatio)}%")

        then: "No exception should have occurred"
        true
    }
}
