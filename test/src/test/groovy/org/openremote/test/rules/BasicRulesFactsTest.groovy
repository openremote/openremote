package org.openremote.test.rules

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.openremote.container.Container
import org.openremote.container.timer.TimerService
import org.openremote.manager.asset.AssetStorageService
import org.openremote.manager.rules.RulesClock
import org.openremote.manager.rules.RulesEngine
import org.openremote.manager.rules.RulesFacts
import org.openremote.manager.rules.facade.AssetsFacade
import spock.lang.Specification

import java.util.stream.Collectors

class BasicRulesFactsTest extends Specification {

    @ToString(includeNames = true)
    class AnonFact {
        String foo
        double bar
        boolean baz

        AnonFact(String foo, double bar, boolean baz) {
            this.foo = foo
            this.bar = bar
            this.baz = baz
        }
    }

    @EqualsAndHashCode(includes = "id")
    @ToString(includeNames = true)
    class AnonFactWithId {
        String id
        String foo

        AnonFactWithId(String id, String foo) {
            this.id = id
            this.foo = foo
        }
    }

    def assetsFacade
    RulesFacts rulesFacts

    def setupSpec() {
        // Init logging config
        Container.LOG.info("Running test...")
    }

    def setup() {
        given: "some rule facts"
        assetsFacade = Mock(AssetsFacade)
        def timerService = new TimerService()
        def assetStorageService = new AssetStorageService()
        timerService.clock = TimerService.Clock.PSEUDO
        rulesFacts = new RulesFacts(timerService, assetStorageService, assetsFacade, this, RulesEngine.RULES_LOG)

        and: "a rules clock"
        def rulesClock = new RulesClock(0)
        rulesFacts.setClock(rulesClock)
    }

    def "Handle named facts"() {

        when: "a fact is added with reserved name"
        rulesFacts.put(RulesFacts.ASSET_STATES, "FOO")

        then: "it should not be allowed"
        IllegalArgumentException ex = thrown()
        assert ex

        when: "some named facts are added"
        rulesFacts.put("foo", "FOO")
        rulesFacts.put("bar", "BAR")
        rulesFacts.put("baz", ["BAZ111", "BAZ111", "BAZ222"])

        then: "facts should be present"
        assert rulesFacts.allFacts.count() == 3
        assert rulesFacts.assetStates.size() == 0
        assert rulesFacts.assetEvents.size() == 0
        assert rulesFacts.namedFacts.size() == 3
        assert rulesFacts.anonymousFacts.size() == 0
        assert rulesFacts.vars.size() == 0
        assert !rulesFacts.hasTemporaryFacts()

        and: "first matching should succeed"
        assert rulesFacts.matchFirst("foo", { fact -> fact == "FOO" }).isPresent()
        assert !rulesFacts.matchFirst("foo", { fact -> fact == "FOO123" }).isPresent()
        assert rulesFacts.matchFirst("bar").isPresent()
        assert !rulesFacts.matchFirst("bar", { fact -> fact == "BAR123" }).isPresent()

        and: "stream matching should succeed "
        assert rulesFacts.match(String.class).count() == 2
        assert rulesFacts.match(String.class, { fact -> fact == "FOO" }).count() == 1
        assert rulesFacts.match(String.class, { fact -> fact == "FOO123" }).count() == 0
        assert rulesFacts.match(Integer.class).count() == 0
        assert rulesFacts.match(Collection.class, { fact -> fact.size() == 3 }).count() == 1
        assert rulesFacts.match(Collection.class, { fact -> fact.size() == 2 }).count() == 0

        when: "a named fact is updated"
        rulesFacts.put("foo", "NEWFOO")

        then: "the update should be present"
        assert !rulesFacts.matchFirst("foo", { fact -> fact == "FOO" }).isPresent()
        assert rulesFacts.matchFirst("foo", { fact -> fact == "NEWFOO" }).isPresent()
        assert rulesFacts.namedFacts.size() == 3

        when: "a named fact is removed"
        rulesFacts.remove("foo")

        then: "the fact should be removed"
        assert !rulesFacts.matchFirst("foo", { fact -> fact == "NEWFOO" }).isPresent()
        assert rulesFacts.namedFacts.size() == 2
    }

    def "Handle anonymous facts"() {

        when: "some anonymous facts are added"
        def anonFact1 = new AnonFact("FOO1", 123, true)
        rulesFacts.put(anonFact1)
        rulesFacts.put(new AnonFact("FOO2", 456, false))
        rulesFacts.put(new AnonFact("FOO3", 789, true))

        then: "facts should be present"
        assert rulesFacts.allFacts.count() == 3
        assert rulesFacts.assetStates.size() == 0
        assert rulesFacts.assetEvents.size() == 0
        assert rulesFacts.namedFacts.size() == 0
        assert rulesFacts.anonymousFacts.size() == 3
        assert rulesFacts.vars.size() == 0
        assert !rulesFacts.hasTemporaryFacts()

        and: "first matching should succeed"
        assert rulesFacts.matchFirst(AnonFact, { fact -> fact.foo == "FOO1" }).isPresent()
        assert rulesFacts.matchFirst({ fact -> fact.foo == "FOO1" }).isPresent()
        assert rulesFacts.matchFirst({ fact -> fact.bar == 456 }).isPresent()
        assert rulesFacts.matchFirst({ fact -> fact.baz == true }).isPresent()
        assert !rulesFacts.matchFirst({ fact -> fact.foo == "FOO1123" }).isPresent()
        assert !rulesFacts.matchFirst({ fact -> fact.bar == 111 }).isPresent()

        and: "stream matching should succeed "
        assert rulesFacts.match({ fact -> fact.foo == "FOO1" }).count() == 1
        assert rulesFacts.match({ fact -> fact.bar == 456 }).count() == 1
        assert rulesFacts.match({ fact -> fact.baz == true }).count() == 2
        assert rulesFacts.match(AnonFact, { fact -> fact.foo == "FOO1" }).count() == 1
        assert rulesFacts.match(AnonFact, { fact -> fact.bar == 456 }).count() == 1
        assert rulesFacts.match(AnonFact, { fact -> fact.baz }).count() == 2
        assert rulesFacts.match(String, { fact -> true }).count() == 0

        when: "an anonymous fact is updated"
        anonFact1.foo = "NEWFOO1"
        rulesFacts.put(anonFact1)

        then: "the update should be present"
        assert !rulesFacts.matchFirst({ fact -> fact.foo == "FOO" }).isPresent()
        assert rulesFacts.matchFirst({ fact -> fact.foo == "NEWFOO1" }).isPresent()
        assert rulesFacts.anonymousFacts.size() == 3

        when: "an anonymous fact is removed"
        rulesFacts.remove(anonFact1)

        then: "the fact should be removed"
        assert !rulesFacts.matchFirst({ fact -> fact.foo == "NEWFOO1" }).isPresent()
        assert rulesFacts.anonymousFacts.size() == 2
    }

    def "Handle anonymous facts with value equality"() {

        when: "some anonymous facts are added"
        rulesFacts.put(new AnonFact("NOID1", 1, true))
        rulesFacts.put(new AnonFact("NOID2", 2, true))
        rulesFacts.put(new AnonFactWithId("ONE", "FOO1"))
        rulesFacts.put(new AnonFactWithId("ONE", "FOO2"))
        rulesFacts.put(new AnonFactWithId("ONE", "FOO3"))
        rulesFacts.put(new AnonFactWithId("TWO", "BAR"))
        rulesFacts.put(new AnonFactWithId("THREE", "BAZ"))

        then: "facts should be present"
        assert rulesFacts.allFacts.count() == 5
        assert rulesFacts.assetStates.size() == 0
        assert rulesFacts.assetEvents.size() == 0
        assert rulesFacts.namedFacts.size() == 0
        assert rulesFacts.anonymousFacts.size() == 5
        assert rulesFacts.vars.size() == 0
        assert !rulesFacts.hasTemporaryFacts()

        and: "matching should succeed"
        assert rulesFacts.match(AnonFact).count() == 2
        assert rulesFacts.match(AnonFactWithId).count() == 3
        assert rulesFacts.match(AnonFactWithId, { it.foo == "FOO3"}).count() == 1
        assert rulesFacts.matchFirst(AnonFactWithId, { it.foo == "FOO3"}).isPresent()

        when: "an anonymous fact is updated"
        rulesFacts.put(new AnonFactWithId("ONE", "FOO4"))

        then: "updated facts should be present"
        assert rulesFacts.allFacts.count() == 5
        assert rulesFacts.assetStates.size() == 0
        assert rulesFacts.assetEvents.size() == 0
        assert rulesFacts.namedFacts.size() == 0
        assert rulesFacts.anonymousFacts.size() == 5
        assert rulesFacts.vars.size() == 0
        assert !rulesFacts.hasTemporaryFacts()

        and: "matching should succeed"
        assert rulesFacts.match(AnonFact).count() == 2
        assert rulesFacts.match(AnonFactWithId).count() == 3
        assert rulesFacts.match(AnonFactWithId, { it.foo == "FOO3"}).count() == 0
        assert !rulesFacts.matchFirst(AnonFactWithId, { it.foo == "FOO3"}).isPresent()
        assert rulesFacts.match(AnonFactWithId, { it.foo == "FOO4"}).count() == 1
        assert rulesFacts.matchFirst(AnonFactWithId, { it.foo == "FOO4"}).isPresent()

        when: "an anonymous fact is removed"
        rulesFacts.remove(new AnonFactWithId("ONE", "whatever"))

        then: "the fact should be removed"
        assert rulesFacts.match(AnonFact).count() == 2
        assert rulesFacts.match(AnonFactWithId).count() == 2
        assert rulesFacts.match(AnonFactWithId, { it.foo == "FOO4"}).count() == 0
        assert !rulesFacts.matchFirst(AnonFactWithId, { it.foo == "FOO4"}).isPresent()
        assert rulesFacts.anonymousFacts.size() == 4
    }

    def "Handle anonymous and named facts of same type"() {

        when: "some anonymous and named facts are added"

        def fact1 = new AnonFactWithId("ONE", "FOO")
        def fact2 = new AnonFactWithId("TWO", "BAR")
        def fact3 = new AnonFactWithId("THREE", "BAZ")
        rulesFacts.put(fact1)
        rulesFacts.put(fact2)
        rulesFacts.put(fact3)
        rulesFacts.put("Anon-$fact1.foo", fact1)
        rulesFacts.put("Anon-$fact2.foo", fact2)
        rulesFacts.put("Anon-$fact3.foo", fact3)

        then: "facts should be present"
        assert rulesFacts.allFacts.count() == 6
        assert rulesFacts.assetStates.size() == 0
        assert rulesFacts.assetEvents.size() == 0
        assert rulesFacts.namedFacts.size() == 3
        assert rulesFacts.anonymousFacts.size() == 3
        assert rulesFacts.vars.size() == 0
        assert !rulesFacts.hasTemporaryFacts()

        and: "matching should succeed"
        assert rulesFacts.match(AnonFactWithId).count() == 6
        assert rulesFacts.match(AnonFactWithId, { it.foo == "FOO"}).count() == 2
        assert rulesFacts.matchFirst(AnonFactWithId, { it.foo == "FOO"}).isPresent()

        when: "an anonymous fact is updated"
        rulesFacts.put(new AnonFactWithId("ONE", "NEW"))

        then: "updated facts should be present"
        assert rulesFacts.allFacts.count() == 6
        assert rulesFacts.assetStates.size() == 0
        assert rulesFacts.assetEvents.size() == 0
        assert rulesFacts.namedFacts.size() == 3
        assert rulesFacts.anonymousFacts.size() == 3
        assert rulesFacts.vars.size() == 0
        assert !rulesFacts.hasTemporaryFacts()

        and: "matching should succeed"
        assert rulesFacts.match(AnonFactWithId).count() == 6
        assert rulesFacts.match(AnonFactWithId, { it.foo == "FOO"}).count() == 1
        assert rulesFacts.matchFirst(AnonFactWithId, { it.foo == "FOO"}).isPresent()
        assert rulesFacts.match(AnonFactWithId, { it.foo == "NEW"}).count() == 1
        assert rulesFacts.matchFirst(AnonFactWithId, { it.foo == "NEW"}).isPresent()

        when: "an anonymous fact is removed"
        rulesFacts.remove(new AnonFactWithId("ONE", "whatever"))

        then: "updated facts should be present"
        assert rulesFacts.allFacts.count() == 5
        assert rulesFacts.assetStates.size() == 0
        assert rulesFacts.assetEvents.size() == 0
        assert rulesFacts.namedFacts.size() == 3
        assert rulesFacts.anonymousFacts.size() == 2
        assert rulesFacts.vars.size() == 0
        assert !rulesFacts.hasTemporaryFacts()

        then: "the fact should be removed"
        assert rulesFacts.match(AnonFactWithId).count() == 5
        assert rulesFacts.match(AnonFactWithId, { it.foo == "FOO"}).count() == 1
        assert rulesFacts.matchFirst(AnonFactWithId, { it.foo == "FOO"}).isPresent()
        assert rulesFacts.match(AnonFactWithId, { it.foo == "NEW"}).count() == 0
        assert !rulesFacts.matchFirst(AnonFactWithId, { it.foo == "NEW"}).isPresent()
    }

    def "Handle temporary named facts"() {

        when: "some temporary facts are added"
        rulesFacts.putTemporary("foo", "PT5S", "FOO")
        rulesFacts.putTemporary("bar", "PT10S", "BAR")
        rulesFacts.putTemporary("baz", "PT15S", "BAZ")

        and: "the clock is advanced and temporary facts are expired"
        rulesFacts.setClock(new RulesClock(3000))
        rulesFacts.removeExpiredTemporaryFacts()

        then: "all temporary facts should still be present"
        assert rulesFacts.hasTemporaryFacts()
        assert rulesFacts.getAllFacts().count() == 3
        assert rulesFacts.matchFirst("foo").isPresent()
        assert rulesFacts.matchFirst("bar").isPresent()
        assert rulesFacts.matchFirst("baz").isPresent()
        assert rulesFacts.get("foo") == "FOO"
        assert rulesFacts.getOptional("foo").get() == "FOO"
        assert rulesFacts.get("abc") == null
        assert !rulesFacts.getOptional("abc").isPresent()

        when: "the clock is advanced and temporary facts are expired"
        rulesFacts.setClock(new RulesClock(6000))
        rulesFacts.removeExpiredTemporaryFacts()

        then: "some temporary facts should still be present"
        assert rulesFacts.hasTemporaryFacts()
        assert rulesFacts.getAllFacts().count() == 2
        assert !rulesFacts.matchFirst("foo").isPresent()
        assert rulesFacts.matchFirst("bar").isPresent()
        assert rulesFacts.matchFirst("baz").isPresent()

        when: "the clock is advanced and temporary facts are expired"
        rulesFacts.setClock(new RulesClock(12000))
        rulesFacts.removeExpiredTemporaryFacts()

        then: "some temporary facts should still be present"
        assert rulesFacts.hasTemporaryFacts()
        assert rulesFacts.getAllFacts().count() == 1
        assert !rulesFacts.matchFirst("foo").isPresent()
        assert !rulesFacts.matchFirst("bar").isPresent()
        assert rulesFacts.matchFirst("baz").isPresent()

        when: "a temporary fact is removed manually"
        rulesFacts.remove("baz")

        then: "all temporary facts should be gone"
        assert !rulesFacts.hasTemporaryFacts()
        assert rulesFacts.getAllFacts().count() == 0
        assert !rulesFacts.matchFirst("foo").isPresent()
        assert !rulesFacts.matchFirst("bar").isPresent()
        assert !rulesFacts.matchFirst("baz").isPresent()
    }

    def "Handle temporary anonymous facts"() {

        when: "some temporary facts are added"
        def anonFact1 = new AnonFact("FOO1", 123, true)
        rulesFacts.putTemporary("PT5S", anonFact1)
        def anonFact2 = new AnonFact("FOO2", 456, false)
        rulesFacts.putTemporary("PT10S", anonFact2)
        def anonFact3 = new AnonFact("FOO3", 789, true)
        rulesFacts.putTemporary("PT15S", anonFact3)

        and: "the clock is advanced and temporary facts are expired"
        rulesFacts.setClock(new RulesClock(3000))
        rulesFacts.removeExpiredTemporaryFacts()

        then: "all temporary facts should still be present"
        assert rulesFacts.hasTemporaryFacts()
        assert rulesFacts.match(AnonFact).count() == 3
        assert rulesFacts.match(String).count() == 0
        assert rulesFacts.match(AnonFact, { fact -> fact.foo == "FOO1" }).count() == 1
        assert rulesFacts.matchFirst(AnonFact, { fact -> fact.foo == "FOO1" }).get() == anonFact1
        assert rulesFacts.match(AnonFact, { fact -> fact.baz }).count() == 2
        assert rulesFacts.match(AnonFact).collect(Collectors.toList()).contains(anonFact1)
        assert rulesFacts.match(AnonFact).collect(Collectors.toList()).contains(anonFact2)
        assert rulesFacts.match(AnonFact).collect(Collectors.toList()).contains(anonFact3)

        when: "the clock is advanced and temporary facts are expired"
        rulesFacts.setClock(new RulesClock(6000))
        rulesFacts.removeExpiredTemporaryFacts()

        then: "some temporary facts should still be present"
        assert rulesFacts.hasTemporaryFacts()
        assert rulesFacts.match(AnonFact).count() == 2
        assert rulesFacts.match(AnonFact, { fact -> fact.foo == "FOO1" }).count() == 0
        assert rulesFacts.match(AnonFact, { fact -> fact.foo.startsWith("FOO") }).count() == 2
        assert rulesFacts.match(AnonFact, { fact -> fact.baz }).count() == 1

        when: "the clock is advanced and temporary facts are expired"
        rulesFacts.setClock(new RulesClock(12000))
        rulesFacts.removeExpiredTemporaryFacts()

        then: "some temporary facts should still be present"
        assert rulesFacts.hasTemporaryFacts()
        assert rulesFacts.match(AnonFact).count() == 1
        assert rulesFacts.match(AnonFact, { fact -> fact.foo == "FOO1" }).count() == 0
        assert rulesFacts.match(AnonFact, { fact -> fact.foo.startsWith("FOO") }).count() == 1
        assert rulesFacts.match(AnonFact, { fact -> fact.baz }).count() == 1

        when: "a temporary fact is removed manually"
        rulesFacts.remove(anonFact3)

        then: "all temporary facts should be gone"
        assert !rulesFacts.hasTemporaryFacts()
        assert rulesFacts.match(AnonFact).count() == 0
    }
}
