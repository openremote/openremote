/*
 * Copyright 2026, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

class TestSummaryListener implements TestListener, Serializable {

    private final List<String> failedTests = Collections.synchronizedList(new ArrayList<>())

    @Override
    void beforeSuite(TestDescriptor suite) {}

    @Override
    void afterSuite(TestDescriptor desc, TestResult result) {
        if (desc.parent != null) {
            return
        }

        def output = "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
        def startItem = '|  '
        def endItem = '  |'
        def repeatLength = startItem.length() + output.length() + endItem.length()

        println '\n' +
                ('-' * repeatLength) + '\n' +
                startItem + output + endItem + '\n' +
                ('-' * repeatLength)

        def failures = new ArrayList<>(failedTests).sort()

        if (!failures.empty) {
            println '\nFailed tests:'
            failures.each { failure ->
                println "  - ${failure}"
            }
        }
    }

    @Override
    void beforeTest(TestDescriptor desc) {}

    @Override
    void afterTest(TestDescriptor desc, TestResult result) {
        if (result.resultType == TestResult.ResultType.FAILURE) {
            def className = desc.className ?: desc.parent?.name ?: '<unknown>'
            failedTests.add("${className} > ${desc.name}")
        }

        println "${desc.className} > ${desc.name} took: ${(result.endTime - result.startTime)}ms"
    }
}
