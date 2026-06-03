/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.manager.rules

import groovy.lang.GroovyShell
import org.codehaus.groovy.control.CompilerConfiguration
import org.kohsuke.groovy.sandbox.SandboxTransformer
import org.openremote.container.timer.TimerService
import org.openremote.model.rules.GlobalRuleset
import org.openremote.model.rules.Ruleset
import spock.lang.Specification
import spock.lang.Unroll

class GroovySandboxReportingTest extends Specification {

    @Unroll
    def "classifies #receiverType #member as #classification"() {
        expect:
        GroovySandboxClassifier.classify(operation, receiverType, member) == classification

        where:
        operation                             | receiverType                    | member           | classification
        GroovySandboxOperation.METHOD         | "org.openremote.model.rules.Assets" | "dispatch"       | GroovySandboxClassification.KNOWN
        GroovySandboxOperation.CONSTRUCTOR    | "java.util.HashMap"             | "<init>"         | GroovySandboxClassification.KNOWN
        GroovySandboxOperation.STATIC_METHOD  | "java.lang.System"              | "currentTimeMillis" | GroovySandboxClassification.DANGEROUS
        GroovySandboxOperation.GET_PROPERTY   | "org.openremote.model.asset.Asset" | "class"          | GroovySandboxClassification.KNOWN
        GroovySandboxOperation.METHOD         | "org.openremote.model.asset.Asset" | "getClass"       | GroovySandboxClassification.DANGEROUS
        GroovySandboxOperation.SOURCE_GRAB    | "groovy.lang.Grab"              | "-"              | GroovySandboxClassification.DANGEROUS
        GroovySandboxOperation.CONSTRUCTOR    | "groovy.lang.Tuple"             | "<init>"         | GroovySandboxClassification.KNOWN
    }

    def "reports source package imports annotations classes and methods"() {
        given:
        def reporter = reporter()
        def ruleset = ruleset()
        def shell = reportingShell(reporter, ruleset)

        when:
        shell.parse("""
            package org.openremote.test.rules

            import groovy.transform.ToString
            import java.util.concurrent.TimeUnit
            import static java.lang.System.currentTimeMillis
            import static org.openremote.model.query.AssetQuery.Operator.*

            @ToString
            class Helper {
                Helper(String name) {}
                String value(Integer amount) { "value-\$amount" }
            }

            rules.add()
        """)

        then:
        signatures(reporter, ruleset).containsAll([
            source(GroovySandboxOperation.SOURCE_PACKAGE, "org.openremote.test.rules", "-"),
            source(GroovySandboxOperation.SOURCE_IMPORT, "groovy.transform.ToString", "ToString"),
            source(GroovySandboxOperation.SOURCE_IMPORT, "java.util.concurrent.TimeUnit", "TimeUnit"),
            source(GroovySandboxOperation.SOURCE_STATIC_IMPORT, "java.lang.System", "currentTimeMillis", GroovySandboxClassification.DANGEROUS),
            source(GroovySandboxOperation.SOURCE_STATIC_STAR_IMPORT, "org.openremote.model.query.AssetQuery.Operator", "*"),
            source(GroovySandboxOperation.SOURCE_ANNOTATION, "groovy.transform.ToString", "-"),
            source(GroovySandboxOperation.SOURCE_CLASS, "org.openremote.test.rules.Helper", "java.lang.Object", GroovySandboxClassification.UNKNOWN),
            source(GroovySandboxOperation.SOURCE_METHOD, "org.openremote.test.rules.Helper", "<init>", GroovySandboxClassification.UNKNOWN, "String"),
            source(GroovySandboxOperation.SOURCE_METHOD, "org.openremote.test.rules.Helper", "value", GroovySandboxClassification.UNKNOWN, "Integer")
        ] as Set)

        and:
        !signatures(reporter, ruleset).any {
            it.operation() == GroovySandboxOperation.SOURCE_CLASS && it.receiverType() == "org.openremote.test.rules.Script1"
        }
    }

    def "reports grab annotations as dangerous source signatures"() {
        given:
        def reporter = reporter()
        def ruleset = ruleset()
        def shell = reportingShell(reporter, ruleset)

        and:
        def grabSource = """
                @Grab("org.example:example:1.0")
                class Helper {}
            """

        when:
        ReportingGroovyCompilationCustomizer.reportGrapeAnnotations(reporter, ruleset, grabSource)
        try {
            shell.parse(grabSource)
        } catch (Exception ignored) {
            // Grape resolution continues after the source signature is reported.
        }

        then:
        signatures(reporter, ruleset).contains(
            source(GroovySandboxOperation.SOURCE_GRAB, "groovy.lang.Grab", "-", GroovySandboxClassification.DANGEROUS)
        )
    }

    def "normalizes generated script and closure runtime type names"() {
        given:
        def script = new GroovyShell().parse("{ value -> value }")
        def closure = script.run()

        expect:
        GroovySandboxSignature.typeName(script) == "groovy.lang.Script"
        GroovySandboxSignature.typeName(closure) == "groovy.lang.Closure"
    }

    def "reports runtime operations from sandbox transformed Groovy scripts"() {
        given:
        def reporter = reporter()
        def ruleset = ruleset()
        def script = sandboxedShell().parse("""
            import java.util.ArrayList

            def values = new ArrayList()
            values.add("one")
            values[0]
            Math.max(1, 2)
        """)
        def interceptor = new ReportingGroovyInterceptor(reporter, ruleset, GroovySandboxPhase.RUNTIME)

        when:
        interceptor.register()
        try {
            script.run()
        } finally {
            interceptor.unregister()
        }

        then:
        signatures(reporter, ruleset).containsAll([
            runtime(GroovySandboxOperation.CONSTRUCTOR, "java.util.ArrayList", "<init>"),
            runtime(GroovySandboxOperation.METHOD, "java.util.ArrayList", "add", "java.lang.String"),
            runtime(GroovySandboxOperation.GET_ARRAY, "java.util.ArrayList", "-", "java.lang.Integer"),
            runtime(GroovySandboxOperation.STATIC_METHOD, "java.lang.Math", "max", "java.lang.Integer", "java.lang.Integer")
        ] as Set)
    }

    def "ruleset deployment reports source and deployment Groovy signatures"() {
        given:
        def reporter = reporter()
        def ruleset = new GlobalRuleset("Deployment report test", Ruleset.Lang.GROOVY, """
            import org.openremote.manager.rules.RulesBuilder

            RulesBuilder rules = binding.rules

            rules.add()
                .name("Reported")
                .when { facts -> true }
                .then { facts -> }
        """).setId(2L).setVersion(1L)
        def deployment = new RulesetDeployment(
            ruleset,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            reporter
        )

        when:
        def compiled = deployment.compileRulesGroovy(ruleset, null, null, null, null, null)

        then:
        compiled
        deployment.getError() == null
        deployment.rules.size() == 1
        signatures(reporter, ruleset).containsAll([
            source(GroovySandboxOperation.SOURCE_IMPORT, "org.openremote.manager.rules.RulesBuilder", "RulesBuilder"),
            signature(GroovySandboxPhase.DEPLOYMENT, GroovySandboxOperation.METHOD, "org.openremote.manager.rules.RulesBuilder", "add"),
            signature(GroovySandboxPhase.DEPLOYMENT, GroovySandboxOperation.METHOD, "org.openremote.manager.rules.RulesBuilder\$Builder", "name", "java.lang.String"),
            signature(GroovySandboxPhase.DEPLOYMENT, GroovySandboxOperation.METHOD, "org.openremote.manager.rules.RulesBuilder\$Builder", "when", "groovy.lang.Closure"),
            signature(GroovySandboxPhase.DEPLOYMENT, GroovySandboxOperation.METHOD, "org.openremote.manager.rules.RulesBuilder\$Builder", "then", "groovy.lang.Closure")
        ] as Set)
    }

    def "flush tracks only pending signature counts"() {
        given:
        def reporter = reporter()
        def ruleset = ruleset()
        def signature = source(GroovySandboxOperation.SOURCE_IMPORT, "java.util.List", "List")

        when:
        reporter.report(ruleset, signature)
        reporter.report(ruleset, signature)

        then:
        counter(reporter, ruleset, signature).pendingCount() == 2

        when:
        reporter.flush(ruleset)

        then:
        counter(reporter, ruleset, signature).lastFlushedCount == 2
        counter(reporter, ruleset, signature).pendingCount() == 0

        when:
        reporter.flush(ruleset)

        then:
        counter(reporter, ruleset, signature).lastFlushedCount == 2
        counter(reporter, ruleset, signature).pendingCount() == 0

        when:
        reporter.report(ruleset, signature)

        then:
        counter(reporter, ruleset, signature).pendingCount() == 1
    }

    def "tracks dropped signatures when per-ruleset cap is reached"() {
        given:
        def reporter = reporter(1)
        def ruleset = ruleset()
        def retainedSignature = source(GroovySandboxOperation.SOURCE_IMPORT, "java.util.List", "List")
        def droppedSignature = source(GroovySandboxOperation.SOURCE_IMPORT, "java.util.Map", "Map")

        when:
        reporter.report(ruleset, retainedSignature)
        reporter.report(ruleset, droppedSignature)
        reporter.report(ruleset, droppedSignature)

        then:
        signatures(reporter, ruleset) == [retainedSignature] as Set
        report(reporter, ruleset).droppedSignatureCount.sum() == 2
        report(reporter, ruleset).lastFlushedDroppedSignatureCount == 0

        when:
        reporter.flush(ruleset)

        then:
        report(reporter, ruleset).lastFlushedDroppedSignatureCount == 2

        when:
        reporter.flush(ruleset)

        then:
        report(reporter, ruleset).lastFlushedDroppedSignatureCount == 2

        when:
        reporter.report(ruleset, droppedSignature)

        then:
        report(reporter, ruleset).droppedSignatureCount.sum() == 3
        report(reporter, ruleset).lastFlushedDroppedSignatureCount == 2
    }

    @Unroll
    def "parses Groovy sandbox mode config value '#value' as #mode"() {
        expect:
        GroovySandboxMode.fromConfig(value) == mode

        where:
        value      | mode
        null       | GroovySandboxMode.OFF
        ""         | GroovySandboxMode.OFF
        " "        | GroovySandboxMode.OFF
        "off"      | GroovySandboxMode.OFF
        "REPORT"   | GroovySandboxMode.REPORT
        " report " | GroovySandboxMode.REPORT
    }

    def "rejects unsupported Groovy sandbox mode config values"() {
        when:
        GroovySandboxMode.fromConfig("enforce")

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("Unsupported Groovy sandbox mode")
        e.message.contains("OFF, REPORT")
    }

    @Unroll
    def "parses positive integer config value '#value' as #expected"() {
        expect:
        RulesService.getPositiveInteger(config, "TEST_KEY", 60) == expected

        where:
        value | expected
        null  | 60
        ""    | 60
        "15"  | 15

        config = value == null ? [:] : ["TEST_KEY": value]
    }

    @Unroll
    def "rejects non-positive integer config value '#value'"() {
        when:
        RulesService.getPositiveInteger(["TEST_KEY": value], "TEST_KEY", 60)

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("TEST_KEY")
        e.message.contains("greater than zero")

        where:
        value << ["0", "-1"]
    }

    private GroovySandboxReporter reporter() {
        reporter(100)
    }

    private GroovySandboxReporter reporter(int maxSignaturesPerRuleset) {
        def timerService = Stub(TimerService) {
            getCurrentTimeMillis() >>> [100L, 101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L, 110L, 111L]
        }
        new GroovySandboxReporter(timerService, maxSignaturesPerRuleset)
    }

    private static GlobalRuleset ruleset() {
        new GlobalRuleset("Sandbox report test", Ruleset.Lang.GROOVY, "").setId(1L).setVersion(1L)
    }

    private static GroovyShell reportingShell(GroovySandboxReporter reporter, GlobalRuleset ruleset) {
        new GroovyShell(new CompilerConfiguration().addCompilationCustomizers(
            new ReportingGroovyCompilationCustomizer(reporter, ruleset)
        ))
    }

    private static GroovyShell sandboxedShell() {
        new GroovyShell(new CompilerConfiguration().addCompilationCustomizers(new SandboxTransformer()))
    }

    private static Set<GroovySandboxSignature> signatures(GroovySandboxReporter reporter, GlobalRuleset ruleset) {
        report(reporter, ruleset).signatureCounters.keySet()
    }

    private static GroovySandboxReporter.SignatureCounter counter(
        GroovySandboxReporter reporter,
        GlobalRuleset ruleset,
        GroovySandboxSignature signature
    ) {
        report(reporter, ruleset).signatureCounters[signature]
    }

    private static GroovySandboxReporter.RulesetReport report(
        GroovySandboxReporter reporter,
        GlobalRuleset ruleset
    ) {
        reporter.reports[new GroovySandboxReporter.RulesetKey(ruleset)]
    }

    private static GroovySandboxSignature source(
        GroovySandboxOperation operation,
        String receiverType,
        String member,
        GroovySandboxClassification classification = GroovySandboxClassifier.classify(operation, receiverType, member),
        String... argumentTypes
    ) {
        signature(
            GroovySandboxPhase.SOURCE,
            operation,
            receiverType,
            member,
            argumentTypes
        )
    }

    private static GroovySandboxSignature runtime(
        GroovySandboxOperation operation,
        String receiverType,
        String member,
        String... argumentTypes
    ) {
        signature(GroovySandboxPhase.RUNTIME, operation, receiverType, member, argumentTypes)
    }

    private static GroovySandboxSignature signature(
        GroovySandboxPhase phase,
        GroovySandboxOperation operation,
        String receiverType,
        String member,
        String... argumentTypes
    ) {
        GroovySandboxSignature.of(
            phase,
            operation,
            receiverType,
            member,
            GroovySandboxClassifier.classify(operation, receiverType, member),
            argumentTypes
        )
    }
}
