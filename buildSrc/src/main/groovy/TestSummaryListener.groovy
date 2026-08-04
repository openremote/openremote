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
