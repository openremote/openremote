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
        GroovySandboxOperation.GET_PROPERTY   | "org.openremote.model.asset.Asset" | "class"          | GroovySandboxClassification.DANGEROUS
        GroovySandboxOperation.SOURCE_GRAB    | "groovy.lang.Grab"              | "-"              | GroovySandboxClassification.DANGEROUS
        GroovySandboxOperation.CONSTRUCTOR    | "groovy.lang.Tuple"             | "<init>"         | GroovySandboxClassification.UNKNOWN
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
            source(GroovySandboxOperation.SOURCE_ANNOTATION, "ToString", "-"),
            source(GroovySandboxOperation.SOURCE_CLASS, "org.openremote.test.rules.Helper", "java.lang.Object", GroovySandboxClassification.UNKNOWN),
            source(GroovySandboxOperation.SOURCE_METHOD, "org.openremote.test.rules.Helper", "<init>", GroovySandboxClassification.UNKNOWN, "String"),
            source(GroovySandboxOperation.SOURCE_METHOD, "org.openremote.test.rules.Helper", "value", GroovySandboxClassification.UNKNOWN, "Integer")
        ] as Set)
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

    private GroovySandboxReporter reporter() {
        def timerService = Stub(TimerService) {
            getCurrentTimeMillis() >>> [100L, 101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L, 110L, 111L]
        }
        new GroovySandboxReporter(timerService, 100)
    }

    private static GlobalRuleset ruleset() {
        new GlobalRuleset("Sandbox report test", Ruleset.Lang.GROOVY, "").setId(1L).setVersion(1L)
    }

    private static GroovyShell reportingShell(GroovySandboxReporter reporter, GlobalRuleset ruleset) {
        new GroovyShell(new CompilerConfiguration().addCompilationCustomizers(
            new ReportingGroovyCompilationCustomizer(reporter, ruleset)
        ))
    }

    private static Set<GroovySandboxSignature> signatures(GroovySandboxReporter reporter, GlobalRuleset ruleset) {
        reporter.reports[new GroovySandboxReporter.RulesetKey(ruleset)].signatureCounters.keySet()
    }

    private static GroovySandboxSignature source(
        GroovySandboxOperation operation,
        String receiverType,
        String member,
        GroovySandboxClassification classification = GroovySandboxClassifier.classify(operation, receiverType, member),
        String... argumentTypes
    ) {
        GroovySandboxSignature.of(
            GroovySandboxPhase.SOURCE,
            operation,
            receiverType,
            member,
            classification,
            argumentTypes
        )
    }
}
