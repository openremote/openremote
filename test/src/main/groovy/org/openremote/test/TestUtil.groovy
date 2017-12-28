package org.openremote.test

class TestUtil {

    static class TaskExecution {
        def message = "Task"
        def count = 1
        def threadsPerInterval = 1
        Closure task

        @Override
        String toString() {
            return "\"message\": \"" + message + "\"" +
                    ", \"count\": " + count +
                    ", \"threadsPerInterval\": " + threadsPerInterval
        }
    }

    static class TaskReport {
        def execution
        def startTimestamp
        def endTimestamp
        def averageMillis
        def totalMillis
        def ops
        def errorCount = 0
        def lastError

        @Override
        String toString() {
            return "{" + execution +
                    ", \"startTimestamp\":" + startTimestamp +
                    ", \"endTimestamp\":" + endTimestamp +
                    ", \"averageMillis\":" + averageMillis +
                    ", \"totalMillis\": " + totalMillis +
                    ", \"ops\": " + ops+
                    (errorCount > 0 ? ", \"errorCount\": " + errorCount : "") +
                    (lastError != null ? ", \"lastError\": \"" + lastError + "\"" : "") +
                    "}"
        }
    }

    /**
     * <code>
     * TaskReport report = performTimed(
     *     new TaskExecution(message: "MyLoadTestingTask", count: 1000, task: { ... })
     * )
     * </code>
     */
    static Closure<TaskReport> performTimed = { TaskExecution taskExecution ->
        def report = new TaskReport(execution: taskExecution)
        report.startTimestamp = System.currentTimeMillis()
        taskExecution.count.times {
            def threads = []
            taskExecution.threadsPerInterval.times {
                threads << new Thread(new Runnable() {
                    @Override
                    void run() {
                        try {
                            taskExecution.task()
                        } catch (Throwable t) {

                            report.lastError = t
                            report.errorCount++
                        }
                    }
                })
            }
            threads*.start()
            threads*.join()
        }
        report.endTimestamp = System.currentTimeMillis()
        report.totalMillis = report.endTimestamp - report.startTimestamp
        report.averageMillis = report.totalMillis / taskExecution.count
        report.ops = 1000 / report.averageMillis
        return report
    }

    static void main(String[] args) {
        performTimed
    }

}
