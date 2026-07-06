import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

class TestSummaryListener implements TestListener, Serializable {

    @Override
    void beforeSuite(TestDescriptor suite) {}

    @Override
    void afterSuite(TestDescriptor desc, TestResult result) {
        if (desc.parent == null) {
            def output = "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
            def startItem = '|  '
            def endItem = '  |'
            def repeatLength = startItem.length() + output.length() + endItem.length()

            println '\n' +
                    ('-' * repeatLength) + '\n' +
                    startItem + output + endItem + '\n' +
                    ('-' * repeatLength)
        }
    }

    @Override
    void beforeTest(TestDescriptor desc) {}

    @Override
    void afterTest(TestDescriptor desc, TestResult result) {
        println "${desc.className} > ${desc.name} took: ${(result.endTime - result.startTime)}ms"
    }
}
