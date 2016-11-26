package org.openremote.test.rules

import org.kie.api.KieBase
import org.kie.api.KieServices
import org.kie.api.builder.KieFileSystem
import org.kie.api.builder.model.KieSessionModel
import org.kie.api.runtime.KieContainer
import org.kie.api.runtime.KieSession
import org.kie.internal.io.ResourceFactory
import org.openremote.controller.event.CustomStateEvent
import org.openremote.controller.rules.RuleExecutionLogger
import org.openremote.test.ContainerTrait
import spock.lang.Shared
import spock.lang.Specification

import java.util.logging.*

import static org.kie.api.builder.Message.Level.ERROR

class RuleExecutionLoggerTest extends Specification implements ContainerTrait {

    public static TEST_SENSOR_NAME = "SENSOR_NAME";

    @Shared
    KieSession ksession

    def setup() {
        def kieServices = KieServices.Factory.get()
        def kieModuleModel = kieServices.newKieModuleModel()
        def kieBaseModel = kieModuleModel.newKieBaseModel("OpenRemoteKBase")
                .setDefault(true)
                .setEqualsBehavior(org.kie.api.conf.EqualityBehaviorOption.EQUALITY)
        def kieSessionModel = kieBaseModel.newKieSessionModel("OpenRemoteKSession")
                .setDefault(true)
                .setType(KieSessionModel.KieSessionType.STATEFUL)
        KieFileSystem kfs = kieServices.newKieFileSystem()
        kfs.writeKModuleXML(kieModuleModel.toXML())

        kfs.write(ResourceFactory.newClassPathResource("org/openremote/test/rules/simple/TestRuleFiring.drl")
        );
        def kieBuilder = kieServices.newKieBuilder(kfs).buildAll();

        if (kieBuilder.getResults().hasMessages(ERROR)) {
            throw new RuntimeException("KIE builder has errors")
        }

        KieContainer kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId())
        KieBase kb = kieContainer.getKieBase();
        ksession = kb.newKieSession();
    }

    def cleanup() {
        if (ksession != null) {
            ksession.dispose()
            ksession = null;
        }
    }

    /**
     * This test confirms the plumbing for the event listener is working.
     * This test is testing the following behavior:
     * The event listener detects rule activations.
     * The listener fires "BeforeMatchFired" in response to rule activations.
     * The listener is properly detecting and logging information about the rule (name, declarations, LHS, etc.).
     */
    def "Event listener plumbing"() {
        given: "a custom state event"
        CustomStateEvent newState = new CustomStateEvent(1, TEST_SENSOR_NAME, "ON");
        ksession.insert(newState)

        and: "a logger of rule execution"
        RuleExecutionLogger ruleExecutionLogger = new RuleExecutionLogger()
        ksession.addEventListener(ruleExecutionLogger)
        //add a handler so logging output can be stored in active memory
        Logger ruleLogger = ruleExecutionLogger.getLOG()
        def testLogHandler = new TestLogHandler()
        ruleLogger.addHandler(testLogHandler);
        ruleLogger.setLevel(Level.ALL);

        when: "rules are fired"
        ksession.fireAllRules();

        String lastLog = String.format("rule \"%s\" // (package org.openremote.test.rules)\n" +
                "\tDeclarations \n\t\tDeclaration: \"\$e\"\n\t\tValue:\n\t\t\tSensor Name: \"%s\"\n\t\t\tSensor Value: \"ON\"\n" +
                "\tLHS objects(antecedents)\n\t\tClass: \"CustomStateEvent\"\n\t\tFields: \n\t\t\tEvent Name: \t\"%s\"\n\t\t\tEvent Value: \t\"ON\"\n", "TestRuleFiring", TEST_SENSOR_NAME, TEST_SENSOR_NAME);

        then: "the execution should be logged"
        testLogHandler.lastLevel == Level.FINE
        testLogHandler.lastMessage == lastLog
    }

    static class TestLogHandler extends Handler {

        Level lastLevel
        String lastMessage;

        TestLogHandler() {
            this.setLevel(Level.ALL);
            this.setFormatter(new SimpleFormatter());
        }

        @Override
        void publish(LogRecord record) {
            lastLevel = record.getLevel();
            lastMessage = record.getMessage();
        }

        @Override
        void flush() {
        }

        @Override
        void close() {
        }
    }
}