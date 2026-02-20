package org.openremote.test.syslog

import org.openremote.container.persistence.PersistenceService
import org.openremote.manager.syslog.SyslogService
import org.openremote.model.syslog.SyslogCategory
import org.openremote.model.syslog.SyslogEvent
import org.openremote.model.syslog.SyslogLevel
import org.openremote.test.ManagerContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import java.time.Instant

class SysLogServiceTest extends Specification implements ManagerContainerTrait {

    @Shared
    static SyslogService syslogService
    @Shared
    static PersistenceService persistenceService

    def setupSpec() {
        syslogService = new SyslogService()
        def container = startContainer(defaultConfig(), defaultServices(syslogService))
        persistenceService = container.getService(PersistenceService.class)
        syslogService.clearStoredEvents()
    }

    def cleanupSpec() {
        if (syslogService != null) {
            syslogService.clearStoredEvents()
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
        def result = syslogService.getEvents(SyslogLevel.WARN, 2, 1, from, to, [SyslogCategory.API], [subCategory])

        then: "only matching events are returned in descending timestamp order"
        result != null
        result.key == 2
        result.value.size() == 2
        result.value[0].message == "error"
        result.value[1].message == "warn"
        result.value.every { it.category == SyslogCategory.API }
        result.value.every { it.level.ordinal() >= SyslogLevel.WARN.ordinal() }

        when: "requesting a limited page size"
        def limited = syslogService.getEvents(SyslogLevel.WARN, 1, 1, from, to, [SyslogCategory.API], [subCategory])

        then: "pagination respects the per-page limit"
        limited.key == 2
        limited.value.size() == 1
        limited.value[0].message == "error"
    }
}
