/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.openremote.test.rules;

import org.drools.core.time.SessionPseudoClock;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.model.KieBaseModel;
import org.kie.api.builder.model.KieModuleModel;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.rule.FactHandle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Based on:
 * https://github.com/kiegroup/drools/blob/master/drools-compiler/src/test/java/org/drools/compiler/integrationtests/CepFireUntilHaltTimerTest.java
 */
public class PseudoClockTimerTest {

    private KieSession ksession;
    private SessionPseudoClock clock;

    public void init(String clockType) {
        String drl = "package test.rules\n" +
            "\n" +
            "rule \"Do something triggered by timer\"\n" +
            "timer (int: 1s 1s)\n" +
            "then\n" +
            "    System.out.println(\"### DO SOMETHING\");\n" +
            "    insert(\"### TIME IS \" + drools.getWorkingMemory().getSessionClock().getCurrentTime());\n" +
            "end\n";

        KieServices ks = KieServices.Factory.get();

        KieModuleModel module = ks.newKieModuleModel();

        KieBaseModel defaultBase = module.newKieBaseModel("defaultKBase")
            .setDefault(true)
            .addPackage("*");
        defaultBase.newKieSessionModel("defaultKSession")
            .setDefault(true)
            .setClockType(ClockTypeOption.get(clockType));

        KieFileSystem kfs = ks.newKieFileSystem()
            .write("src/main/resources/r1.drl", drl);
        kfs.writeKModuleXML(module.toXML());
        ks.newKieBuilder(kfs).buildAll();

        ksession = ks.newKieContainer(ks.getRepository().getDefaultReleaseId())
            .newKieSession();

        if (clockType.equals("pseudo"))
            clock = ksession.getSessionClock();
    }

    public void cleanup() {
        ksession.dispose();
    }

    @Test
    public void testTimerExecution() throws Exception {
/*
        init("realtime");
        performRealtimeClockTest();
        cleanup();
*/

/*
        init("pseudo");
        performPseudoClockTest();
        cleanup();
*/
    }

    private void performRealtimeClockTest() throws Exception {
        ExecutorService thread = Executors.newSingleThreadExecutor();
        final Future fireUntilHaltResult = thread.submit(() -> ksession.fireUntilHalt());

        try {
            Thread.sleep(10500);
            for (FactHandle factHandle : ksession.getFactHandles()) {
                System.err.println("GOT: " + ksession.getObject(factHandle));
            }
            assertEquals(10, ksession.getFactCount());
        } finally {
            ksession.halt();
            fireUntilHaltResult.get(60000, TimeUnit.SECONDS);
            thread.shutdown();
        }
    }

    private void performPseudoClockTest() throws Exception {
        ExecutorService thread = Executors.newSingleThreadExecutor();
        final Future fireUntilHaltResult = thread.submit(() -> ksession.fireUntilHalt());

        try {
            Thread.sleep(1000);
            for (int i = 0; i < 10; i++) {
                clock.advanceTime(1, TimeUnit.SECONDS);
                ksession.insert("foo" + i);
            }
            Thread.sleep(1000);
            for (FactHandle factHandle : ksession.getFactHandles()) {
                System.err.println("GOT: " + ksession.getObject(factHandle));
            }
            assertEquals(10, ksession.getFactCount());
        } finally {
            ksession.halt();
            fireUntilHaltResult.get(60000, TimeUnit.SECONDS);
            thread.shutdown();
        }
    }
}