package org.openremote.test.syslog

import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.syslog.SyslogService
import org.openremote.model.syslog.SyslogCategory
import org.openremote.model.syslog.SyslogEvent
import org.openremote.model.syslog.SyslogLevel
import org.openremote.model.syslog.SyslogRealmMarker
import org.openremote.model.syslog.SyslogRealmRegistry
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant
import java.util.logging.Level
import java.util.logging.LogRecord

class SysLogServiceTest extends Specification implements ManagerContainerTrait {

    @Shared
    static SyslogService syslogService
    @Shared
    static PersistenceService persistenceService

    def setupSpec() {
        syslogService = new SyslogService()
        def container = startContainer(defaultConfig(), defaultServices(syslogService))
        persistenceService = container.getService(PersistenceService.class)
        syslogService.clearStoredEvents(null)
    }

    def cleanupSpec() {
        if (syslogService != null) {
            syslogService.clearStoredEvents(null)
        }
    }

    def "Get events applies filters and pagination"() {
        given: "syslog events are stored with distinct level/category combinations"
        Instant now = getInstantTimeOf(container)
        String subCategory = "SysLogServiceTest-" + UUID.randomUUID()
        Instant from = now.minusSeconds(600)
        Instant to = now.plusSeconds(600)

        persistenceService.doTransaction { em ->
            em.persist(new SyslogEvent(now.minusSeconds(300).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "info"))
            em.persist(new SyslogEvent(now.minusSeconds(200).toEpochMilli(), SyslogLevel.WARN, SyslogCategory.API, subCategory, "warn"))
            em.persist(new SyslogEvent(now.minusSeconds(100).toEpochMilli(), SyslogLevel.ERROR, SyslogCategory.API, subCategory, "error"))
            em.persist(new SyslogEvent(now.minusSeconds(50).toEpochMilli(), SyslogLevel.ERROR, SyslogCategory.DATA, subCategory, "other"))
        }

        when: "requesting events for WARN+ API category with a subcategory filter"
        def result = syslogService.getEvents(SyslogLevel.WARN, 2, 1, from, to, [SyslogCategory.API], [subCategory], null)

        then: "only matching events are returned in descending timestamp order"
        result != null
        result.key == 2
        result.value.size() == 2
        result.value[0].message == "error"
        result.value[1].message == "warn"
        result.value.every { it.category == SyslogCategory.API }
        result.value.every { it.level.ordinal() >= SyslogLevel.WARN.ordinal() }

        when: "requesting a limited page size"
        def limited = syslogService.getEvents(SyslogLevel.WARN, 1, 1, from, to, [SyslogCategory.API], [subCategory], null)

        then: "pagination respects the per-page limit"
        limited.key == 2
        limited.value.size() == 1
        limited.value[0].message == "error"
    }

    def "Get events filters by realm"() {
        given: "syslog events are stored for different realms and as system logs"
        Instant now = getInstantTimeOf(container)
        String subCategory = "SysLogServiceRealmTest-" + UUID.randomUUID()
        Instant from = now.minusSeconds(600)
        Instant to = now.plusSeconds(600)

        persistenceService.doTransaction { em ->
            em.persist(new SyslogEvent(now.minusSeconds(300).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "building event", "building"))
            em.persist(new SyslogEvent(now.minusSeconds(200).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "smartcity event", "smartcity"))
            em.persist(new SyslogEvent(now.minusSeconds(100).toEpochMilli(), SyslogLevel.INFO, SyslogCategory.API, subCategory, "system event"))
        }

        when: "requesting events for a specific realm"
        def buildingResult = syslogService.getEvents(null, 10, 1, from, to, null, [subCategory], "building")

        then: "only that realm's events are returned and system logs (no realm) are excluded"
        buildingResult.key == 1
        buildingResult.value.size() == 1
        buildingResult.value[0].message == "building event"
        buildingResult.value[0].realm == "building"

        when: "requesting events without a realm filter"
        def allResult = syslogService.getEvents(null, 10, 1, from, to, null, [subCategory], null)

        then: "events of all realms including system logs are returned"
        allResult.key == 3
        allResult.value.collect { it.message }.toSet() == ["building event", "smartcity event", "system event"].toSet()
    }

    def "Level category filter applies realm"() {
        given: "a level/category filter restricted to a realm"
        def filter = new SyslogEvent.LevelCategoryFilter(SyslogLevel.INFO)
        filter.setRealm("building")

        expect: "an event of the realm passes and other/system events are dropped"
        filter.apply(new SyslogEvent(0, SyslogLevel.INFO, SyslogCategory.API, null, "msg", "building")) != null
        filter.apply(new SyslogEvent(0, SyslogLevel.INFO, SyslogCategory.API, null, "msg", "smartcity")) == null
        filter.apply(new SyslogEvent(0, SyslogLevel.INFO, SyslogCategory.API, null, "msg", null)) == null

        and: "without a realm the filter passes events of any realm"
        def unrestricted = new SyslogEvent.LevelCategoryFilter(SyslogLevel.INFO)
        unrestricted.apply(new SyslogEvent(0, SyslogLevel.INFO, SyslogCategory.API, null, "msg", "building")) != null
        unrestricted.apply(new SyslogEvent(0, SyslogLevel.INFO, SyslogCategory.API, null, "msg", null)) != null
    }

    def "Map syslog event resolves realm from registry and marker"() {
        given: "a logger name registered for a realm"
        def loggerName = "org.openremote.manager.rules.RulesEngine.RealmEngine-building.RULES"
        SyslogRealmRegistry.register(loggerName, "building")

        when: "a log record of the registered logger is mapped"
        def record = new LogRecord(Level.INFO, "rules message")
        record.setLoggerName(loggerName)
        def event = SyslogCategory.mapSyslogEvent(record)

        then: "the event is attributed to the realm"
        event != null
        event.realm == "building"

        when: "a log record of an unregistered logger is mapped"
        def systemRecord = new LogRecord(Level.INFO, "system message")
        systemRecord.setLoggerName("org.openremote.manager.rules.RulesEngine.GlobalEngine-.RULES")
        def systemEvent = SyslogCategory.mapSyslogEvent(systemRecord)

        then: "the event has no realm"
        systemEvent != null
        systemEvent.realm == null

        when: "a log record carries an explicit realm marker parameter"
        def markerRecord = new LogRecord(Level.INFO, "marker message")
        markerRecord.setLoggerName(loggerName)
        markerRecord.setParameters([new SyslogRealmMarker("smartcity")] as Object[])
        def markerEvent = SyslogCategory.mapSyslogEvent(markerRecord)

        then: "the marker wins over the registry"
        markerEvent != null
        markerEvent.realm == "smartcity"

        cleanup:
        SyslogRealmRegistry.unregister(loggerName)
    }
}
