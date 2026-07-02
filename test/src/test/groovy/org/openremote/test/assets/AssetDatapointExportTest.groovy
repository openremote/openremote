package org.openremote.test.assets

import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.datapoint.AssetDatapointService
import org.openremote.manager.setup.SetupService
import org.openremote.manager.web.ManagerWebService
import org.openremote.model.asset.impl.LightAsset
import org.openremote.model.attribute.Attribute
import org.openremote.model.attribute.AttributeRef
import org.openremote.model.datapoint.DatapointExportFormat
import org.openremote.model.datapoint.DatapointQueryTooLargeException
import org.openremote.model.datapoint.ValueDatapoint
import org.openremote.model.query.AssetQuery
import org.openremote.model.query.filter.RealmPredicate
import org.openremote.setup.integration.KeycloakTestSetup
import org.openremote.setup.integration.ManagerTestSetup
import org.openremote.test.ManagerContainerTrait
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId

import static org.openremote.model.Constants.KEYCLOAK_CLIENT_ID
import static org.openremote.model.value.ValueType.NUMBER

class AssetDatapointExportTest extends Specification implements ManagerContainerTrait {

    // Important for test is that we use a different TZ for DB than for JVM
    private static final String EXPORT_TEST_DATABASE_TIME_ZONE = ZoneId.systemDefault().id == "Europe/Amsterdam" ? "UTC" : "Europe/Amsterdam"
    private static final int EXPORT_TEST_DATABASE_POOL_SIZE = 5

    def "Test CSV export functionality for asset data points"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started with a bounded DB connection pool"
        def config = defaultConfig()
        config.put(PersistenceService.OR_DB_POOL_MIN_SIZE, Integer.toString(EXPORT_TEST_DATABASE_POOL_SIZE))
        config.put(PersistenceService.OR_DB_POOL_MAX_SIZE, Integer.toString(EXPORT_TEST_DATABASE_POOL_SIZE))
        def container = startContainer(config, defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)
        def persistenceService = container.getService(PersistenceService.class)
        setDatabaseSessionTimeZone(persistenceService, EXPORT_TEST_DATABASE_TIME_ZONE, EXPORT_TEST_DATABASE_POOL_SIZE)
        assetDatapointService.datapointExportLimit = 1000

        and: "the database session timezone is different from the JVM timezone"
        assert getDatabaseSessionTimeZone(persistenceService) == EXPORT_TEST_DATABASE_TIME_ZONE
        assert ZoneId.systemDefault() != ZoneId.of(EXPORT_TEST_DATABASE_TIME_ZONE)

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
        assert csvExport2Lines[0].contains(assetName + " : " + attributeName)

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
        assert csvExport3Lines[0].contains(assetName + " : " + attributeName)

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

        /* ------------------------- */

        when: "the limit of data export is lowered to 4"
        assetDatapointService.datapointExportLimit = 4

        and: "we try to export the same amount of data points"
        def inputStream4 = assetDatapointService.exportDatapoints(
                [new AttributeRef(asset.id, attributeName)] as AttributeRef[],
                dateTime.minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        then: "the CSV export should throw an exception"
        thrown(DatapointQueryTooLargeException)

        /* ------------------------- */

        cleanup: "Remove the limit on datapoint querying"
        if (assetDatapointService != null) {
            assetDatapointService.datapointExportLimit = assetDatapointService.OR_DATA_POINTS_EXPORT_LIMIT_DEFAULT
        }
        if (persistenceService != null) {
            setDatabaseSessionTimeZone(persistenceService, "UTC", EXPORT_TEST_DATABASE_POOL_SIZE)
        }
    }

    def "Export query is not vulnerable to SQL injection via attributeRefs"() {

        given: "expected conditions"
        def conditions = new PollingConditions(timeout: 10, delay: 0.2)

        and: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)

        and: "ensure there are no datapoints"
        assetDatapointService.purgeDataPoints()

        when: "requesting the first light asset in City realm"
        def asset = assetStorageService.find(
                new AssetQuery()
                        .types(LightAsset.class)
                        .realm(new RealmPredicate(keycloakTestSetup.realmCity.name))
                        .names("Light 1")
        )

        then: "asset should exist and be correct"
        assert asset != null

        when: "exporting with a malicious attribute name"
        def injectedAttributeName = "x')) UNION ((SELECT now()::timestamp, table_name::text, 'injected'::text, NULL::jsonb FROM information_schema.tables WHERE table_schema='public"
        def csvExport = assetDatapointService.exportDatapoints(
                [new AttributeRef(asset.id, injectedAttributeName)] as AttributeRef[],
                LocalDateTime.now().minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

        then: "the CSV export should not contain injected rows"
        assert csvExport != null
        def csvExportLines = csvExport.readLines()
        assert csvExportLines.size() == 1
    }

    def "Crosstab exports treat asset names with SQL delimiters as CSV labels"() {

        given: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)
        assetDatapointService.datapointExportLimit = 10000

        and: "ensure there are no datapoints"
        assetDatapointService.purgeDataPoints()

        and: "an asset name contains SQL delimiter syntax"
        def asset = assetStorageService.find(
                new AssetQuery()
                        .types(LightAsset.class)
                        .realm(new RealmPredicate(keycloakTestSetup.realmCity.name))
                        .names("Light 1")
        )
        assert asset != null
        def originalName = asset.name
        def maliciousAssetName = 'SQL "delimiter" $cat$ -- label'
        def attributeName = "brightness"
        asset.name = maliciousAssetName
        asset = assetStorageService.merge(asset)

        and: "the asset has one datapoint for a real exported attribute"
        def dateTime = LocalDateTime.now()
        assetDatapointService.upsertValues(
                asset.getId(),
                attributeName,
                [new ValueDatapoint<>(dateTime.minusMinutes(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 42d)]
        )

        expect: "both crosstab formats export the datapoint without treating the asset name as SQL"
        [DatapointExportFormat.CSV_CROSSTAB, DatapointExportFormat.CSV_CROSSTAB_MINUTE].each { format ->
            def csvExport = assetDatapointService.exportDatapoints(
                    [new AttributeRef(asset.id, attributeName)] as AttributeRef[],
                    dateTime.minusMinutes(5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    format
            ).getText(StandardCharsets.UTF_8.name())
            def csvExportLines = csvExport.readLines()

            assert csvExportLines.size() == 2
            assert parseCsvLine(csvExportLines[0]) == ["timestamp", maliciousAssetName + " : " + attributeName]
            assert csvExportLines[1].contains("42")
        }

        cleanup: "restore the fixture asset name"
        if (assetStorageService != null && asset != null && originalName != null) {
            def currentAsset = assetStorageService.find(asset.id, true)
            if (currentAsset != null) {
                currentAsset.name = originalName
                assetStorageService.merge(currentAsset)
            }
        }
        if (assetDatapointService != null) {
            assetDatapointService.datapointExportLimit = assetDatapointService.OR_DATA_POINTS_EXPORT_LIMIT_DEFAULT
        }
    }

    def "Export query is not vulnerable to SQL injection via REST API"() {

        given: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)
        assetDatapointService.datapointExportLimit = 10000

        and: "ensure there are no datapoints"
        assetDatapointService.purgeDataPoints()

        and: "a resource client is created"
        def accessToken = authenticate(
                container,
                keycloakTestSetup.realmCity.name,
                KEYCLOAK_CLIENT_ID,
                "smartcity",
                "smartcity"
        )
        def response = null

        when: "exporting with a malicious attribute name via REST API"
        def asset = assetStorageService.find(
                new AssetQuery()
                        .types(LightAsset.class)
                        .realm(new RealmPredicate(keycloakTestSetup.realmCity.name))
                        .names("Light 1")
        )
        assert asset != null
        def injectedAttributeName = "x')) UNION ((SELECT now()::timestamp, table_name::text, 'injected'::text, NULL::jsonb FROM information_schema.tables WHERE table_schema='public"
        def attributeRefsJson = "[{\"id\":\"${asset.id}\",\"name\":\"${injectedAttributeName}\"}]"
        def encodedAttributeRefs = URLEncoder.encode(attributeRefsJson, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        def fromTimestamp = LocalDateTime.now().minusMinutes(30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        def toTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        def baseUri = serverUri(serverPort).clone()
                .replacePath(ManagerWebService.API_PATH)
                .path(keycloakTestSetup.realmCity.name)
                .path("asset")
                .path("datapoint")
                .path("export")
                .build()
        def url = new URL("${baseUri}?attributeRefs=${encodedAttributeRefs}&fromTimestamp=${fromTimestamp}&toTimestamp=${toTimestamp}")
        response = (HttpURLConnection) url.openConnection()
        response.setRequestMethod("GET")
        response.setRequestProperty("Authorization", "Bearer ${accessToken}")
        response.setRequestProperty("Accept", "application/zip")
        response.connect()

        then: "the REST API should reject the unknown attribute name"
        response.responseCode == 404

        cleanup: "Remove the limit on datapoint exporting"
        if (response != null) {
            response.disconnect()
        }
        assetDatapointService.datapointExportLimit = assetDatapointService.OR_DATA_POINTS_EXPORT_LIMIT_DEFAULT
    }

    def "Restricted users cannot export datapoints for linked non-restricted attributes"() {
        given: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)
        assetDatapointService.datapointExportLimit = 10000

        and: "a linked asset has historical data for an attribute that is not restricted-readable"
        assetDatapointService.purgeDataPoints()
        def asset = assetStorageService.find(managerTestSetup.apartment1Id, true)
        def attributeName = "nonRestrictedHistory"
        asset.getAttributes().addOrReplace(new Attribute<>(attributeName, NUMBER, 0d))
        assetStorageService.merge(asset)

        def dateTime = LocalDateTime.now()
        assetDatapointService.upsertValues(
                asset.getId(),
                attributeName,
                [new ValueDatapoint<>(dateTime.minusMinutes(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 42d)]
        )

        and: "a restricted user is authenticated in the asset realm"
        def accessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        )
        when: "the restricted user exports the historical datapoints"
        def response = null
        def attributeRefsJson = "[{\"id\":\"${asset.id}\",\"name\":\"${attributeName}\"}]"
        def encodedAttributeRefs = URLEncoder.encode(attributeRefsJson, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        def fromTimestamp = dateTime.minusMinutes(5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        def toTimestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        def baseUri = serverUri(serverPort).clone()
                .replacePath(ManagerWebService.API_PATH)
                .path(keycloakTestSetup.realmBuilding.name)
                .path("asset")
                .path("datapoint")
                .path("export")
                .build()
        def url = new URL("${baseUri}?attributeRefs=${encodedAttributeRefs}&fromTimestamp=${fromTimestamp}&toTimestamp=${toTimestamp}")
        response = (HttpURLConnection) url.openConnection()
        response.setRequestMethod("GET")
        response.setRequestProperty("Authorization", "Bearer ${accessToken}")
        response.setRequestProperty("Accept", "application/zip")
        response.connect()

        then: "the export endpoint should apply the same restricted attribute check"
        response.responseCode == 403

        cleanup: "Remove the limit on datapoint exporting"
        if (response != null) {
            response.disconnect()
        }
        assetDatapointService.datapointExportLimit = assetDatapointService.OR_DATA_POINTS_EXPORT_LIMIT_DEFAULT
    }

    def "Restricted users cannot get datapoint periods for linked non-restricted attributes"() {
        given: "the container is started"
        def container = startContainer(defaultConfig(), defaultServices())
        def keycloakTestSetup = container.getService(SetupService.class).getTaskOfType(KeycloakTestSetup.class)
        def managerTestSetup = container.getService(SetupService.class).getTaskOfType(ManagerTestSetup.class)
        def assetStorageService = container.getService(AssetStorageService.class)
        def assetDatapointService = container.getService(AssetDatapointService.class)

        and: "a linked asset has historical data for an attribute that is not restricted-readable"
        assetDatapointService.purgeDataPoints()
        def asset = assetStorageService.find(managerTestSetup.apartment1Id, true)
        def attributeName = "nonRestrictedHistoryPeriod"
        asset.getAttributes().addOrReplace(new Attribute<>(attributeName, NUMBER, 0d))
        assetStorageService.merge(asset)

        def dateTime = LocalDateTime.now()
        assetDatapointService.upsertValues(
                asset.getId(),
                attributeName,
                [new ValueDatapoint<>(dateTime.minusMinutes(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), 42d)]
        )

        and: "a restricted user is authenticated in the asset realm"
        def accessToken = authenticate(
                container,
                keycloakTestSetup.realmBuilding.name,
                KEYCLOAK_CLIENT_ID,
                "testuser3",
                "testuser3"
        )

        when: "the restricted user requests the historical datapoint period"
        def response = null
        def encodedAssetId = URLEncoder.encode(asset.id, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        def encodedAttributeName = URLEncoder.encode(attributeName, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        def baseUri = serverUri(serverPort).clone()
                .replacePath(ManagerWebService.API_PATH)
                .path(keycloakTestSetup.realmBuilding.name)
                .path("asset")
                .path("datapoint")
                .path("periods")
                .build()
        def url = new URL("${baseUri}?assetId=${encodedAssetId}&attributeName=${encodedAttributeName}")
        response = (HttpURLConnection) url.openConnection()
        response.setRequestMethod("GET")
        response.setRequestProperty("Authorization", "Bearer ${accessToken}")
        response.setRequestProperty("Accept", "application/json")
        response.connect()

        then: "the period endpoint should apply the same restricted attribute check"
        response.responseCode == 403

        cleanup:
        if (response != null) {
            response.disconnect()
        }
    }

    private static List<String> parseCsvLine(String line) {
        def fields = []
        def current = new StringBuilder()
        boolean quoted = false

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i)
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"')
                        i++
                    } else {
                        quoted = false
                    }
                } else {
                    current.append(c)
                }
            } else if (c == '"') {
                quoted = true
            } else if (c == ',') {
                fields.add(current.toString())
                current.setLength(0)
            } else {
                current.append(c)
            }
        }

        fields.add(current.toString())
        return fields
    }

    private static void setDatabaseSessionTimeZone(PersistenceService persistenceService, String timeZone, int connectionCount) {
        def connectionProvider = getConnectionProvider(persistenceService)
        def connections = []
        try {
            connectionCount.times {
                connections.add(connectionProvider.getConnection())
            }
            connections.each { connection ->
                def statement = connection.createStatement()
                try {
                    statement.execute("set time zone '${timeZone}'")
                } finally {
                    statement.close()
                }
            }
        } finally {
            connections.each { connection ->
                connectionProvider.closeConnection(connection)
            }
        }
    }

    private static String getDatabaseSessionTimeZone(PersistenceService persistenceService) {
        withConnection(persistenceService) { connection ->
            def statement = connection.createStatement()
            try {
                def resultSet = statement.executeQuery("select current_setting('TimeZone')")
                try {
                    resultSet.next()
                    return resultSet.getString(1)
                } finally {
                    resultSet.close()
                }
            } finally {
                statement.close()
            }
        }
    }

    private static <T> T withConnection(PersistenceService persistenceService, Closure<T> closure) {
        def connectionProvider = getConnectionProvider(persistenceService)
        def connection = connectionProvider.getConnection()
        try {
            return closure.call(connection)
        } finally {
            connectionProvider.closeConnection(connection)
        }
    }

    private static ConnectionProvider getConnectionProvider(PersistenceService persistenceService) {
        return persistenceService.entityManagerFactory
                .unwrap(SessionFactoryImplementor.class)
                .serviceRegistry
                .requireService(ConnectionProvider.class)
    }

}
