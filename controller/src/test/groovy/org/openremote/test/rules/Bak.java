package org.openremote.test.rules;

public class Bak {


//
//    /**
//     * Test a mixed deployment of multiple rules, some from drools rule language,
//     * others from CSV decision table.
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testMixedDRLCSVDeployment() throws Exception {
//        String newResourcePath = AllTests.getAbsoluteFixturePath()
//            .resolve("statuscache/rules/mixed/").toString();
//
//        config.setResourcePath(newResourcePath);
//
//        RuleEngine rules = new RuleEngine();
//        EventGrab grab = new EventGrab();
//
//        List<EventProcessor> processors = new ArrayList<EventProcessor>();
//        processors.add(rules);
//        processors.add(grab);
//
//        CommandCounter commandCounter = new CommandCounter();
//
//        StatusCache cache = createCache(processors, "commandCounter", commandCounter, "count");
//
//
//        // should not trigger commands...
//
//        Switch sw = new Switch(1, "test", "on", Switch.State.ON);
//
//        cache.update(sw);
//        Event evt = grab.event;
//
//        Assert.assertTrue(evt.getSource().equals("test"));
//        Assert.assertTrue(evt.getSourceID() == 1);
//        Assert.assertTrue(evt.getValue().equals("on"));
//        Assert.assertTrue(evt.serialize().equals("on"));
//        Assert.assertTrue(evt instanceof Switch);
//        Assert.assertTrue(evt.equals(sw));
//        Assert.assertTrue(evt == sw);
//
//        Switch swevt = (Switch) evt;
//
//        Assert.assertTrue(swevt.getState().equals(Switch.State.ON));
//
//        Assert.assertTrue(grab.count == 1);
//        Assert.assertTrue(commandCounter.count == 0);
//
//
//        // Should get its value modified by EventMod.drl above range max...
//
//        Level level = new Level(123, "test level mod", 30);
//
//        cache.registerSensor(new LevelSensor("test level mod", 123, cache, new TestCommand(), 1));
//
//        cache.update(level);
//
//        Thread.sleep(2000);
//
//        evt = grab.event;
//
//        Assert.assertTrue(evt.getSource().equals("test level mod"));
//
//        Assert.assertTrue(
//            "Was expecting 100, got " + evt.getValue(),
//            evt.getValue().equals(100)
//        );
//
//        Assert.assertTrue(evt.serialize().equals("100"));
//        Assert.assertTrue(evt instanceof Level);
//        Assert.assertTrue(!evt.equals(level));
//        Assert.assertTrue(evt != level);
//
//        Level l1 = (Level) evt;
//        Assert.assertTrue(l1.getValue() == 100);
//
//        Assert.assertTrue(grab.count == 2);
//
//
//        // should execute command with param 55...
//
//        level = new Level(555, "test level mod 555", 101);
//
//        cache.update(level);
//        evt = grab.event;
//
//        Assert.assertTrue(evt.getSource().equals("test level mod 555"));
//        Assert.assertTrue(evt.getSourceID() == 555);
//        Assert.assertTrue(evt.getValue().equals(100));
//        Assert.assertTrue(evt.serialize().equals("100"));
//        Assert.assertTrue(evt instanceof Level);
//        Assert.assertTrue(evt.equals(level));
//        Assert.assertTrue(evt == level);
//
//
//        Assert.assertTrue(commandCounter.count == 1);
//        Assert.assertTrue(commandCounter.lastValue.equals("55"));
//        Assert.assertTrue(grab.count == 3);
//
//
//        // should execute command with param 6666...
//
//        level = new Level(666, "test level mod 666", 1);
//
//        cache.update(level);
//        evt = grab.event;
//
//        Assert.assertTrue(evt.getSource().equals("test level mod 666"));
//        Assert.assertTrue(evt.getSourceID() == 666);
//        Assert.assertTrue(evt.getValue().equals(1));
//        Assert.assertTrue(evt.serialize().equals("1"));
//
//        Assert.assertTrue(commandCounter.count == 2);
//        Assert.assertTrue(commandCounter.lastValue.equals("6666"));
//        Assert.assertTrue(grab.count == 4);
//
//
//        // should execute command with param -77
//
//        level = new Level(777, "test level mod 777", 10);
//
//        cache.update(level);
//        evt = grab.event;
//
//        Assert.assertTrue(evt.getSource().equals("test level mod 777"));
//        Assert.assertTrue(evt.getSourceID() == 777);
//        Assert.assertTrue(evt.getValue().equals(10));
//        Assert.assertTrue(evt.serialize().equals("10"));
//
//        Assert.assertTrue(commandCounter.count == 3);
//        Assert.assertTrue(commandCounter.lastValue.equals("-77"));
//        Assert.assertTrue(grab.count == 5);
//    }
//
//
//    /**
//     * Test rule engine initialization in case of a broken CSV file (should not
//     * throw an exception that propagates through the event processor).
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testDecisionTableNoRuleSet() throws Exception {
//        String newResourcePath = AllTests.getAbsoluteFixturePath()
//            .resolve("statuscache/rules/dtable-noruleset/").toString();
//
//        config.setResourcePath(newResourcePath);
//
//        RuleEngine rules = new RuleEngine();
//        rules.init();
//
//        rules.start(new LifeCycleEvent(null));
//    }
//
//
//    /**
//     * Test rule engine initialization in case of a incorrectly defined decision table
//     * (should not propagate exceptions through event processor implementation).
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testDecisionTableIncorrectDefinition() throws Exception {
//        String newResourcePath = AllTests.getAbsoluteFixturePath()
//            .resolve("statuscache/rules/dtable-incorrect-definition/").toString();
//
//        config.setResourcePath(newResourcePath);
//
//        RuleEngine rules = new RuleEngine();
//        rules.init();
//
//        rules.start(new LifeCycleEvent(null));
//    }
//
//
//    /**
//     * Test basic decision table deployment and operation to modify the processed events.
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testDecisionTable() throws Exception {
//        String newResourcePath = AllTests.getAbsoluteFixturePath()
//            .resolve("statuscache/rules/dtable/").toString();
//
//        config.setResourcePath(newResourcePath);
//
//        RuleEngine rules = new RuleEngine();
//        rules.init();
//        EventGrab grab = new EventGrab();
//
//        List<EventProcessor> processors = new ArrayList<EventProcessor>();
//        processors.add(rules);
//        processors.add(grab);
//
//        CommandCounter commandCounter = new CommandCounter();
//        StatusCache cache = createCache(processors, "commandCounter", commandCounter, "count");
//
//
//        Switch sw = new Switch(555, "test555", "on", Switch.State.ON);
//
//        cache.update(sw);
//
//        Assert.assertTrue(
//            "Expected 'triggered', got '" + commandCounter.lastValue + "'",
//            commandCounter.lastValue.equals("triggered")
//        );
//
//        sw = new Switch(666, "test666", "on", Switch.State.ON);
//
//        cache.update(sw);
//
//        Assert.assertTrue(commandCounter.lastValue.equals("started"));
//
//        sw = new Switch(777, "test777", "on", Switch.State.ON);
//
//        cache.update(sw);
//
//        Assert.assertTrue(commandCounter.lastValue.equals("good"));
//    }
//
//
//    /**
//     * TODO
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void vacationTest() throws Exception {
//        String newResourcePath = AllTests.getAbsoluteFixturePath()
//            .resolve("statuscache/rules/holiday-example/").toString();
//
//        config.setResourcePath(newResourcePath);
//
//        RuleEngine rules = new RuleEngine();
//        EventGrab grab = new EventGrab();
//
//        List<EventProcessor> processors = new ArrayList<EventProcessor>();
//        processors.add(rules);
//        processors.add(grab);
//
//        CommandCounter tempCommands = new CommandCounter();
//
//        StatusCache cache = createCache(processors, "commandCounter", tempCommands, "temp");
//
//
//        Assert.assertTrue(grab.count == 0);
//
//
//        {
//            CustomState state = new CustomState(20, "time of day", "day");
//
//            cache.update(state);
//
//            Event evt = grab.event;
//
//            Assert.assertTrue(grab.count == 1);
//
//            Assert.assertTrue(evt.getSourceID() == 20);
//            Assert.assertTrue(evt.getSource().equals("time of day"));
//            Assert.assertTrue(evt.getValue().equals("day"));
//            Assert.assertTrue(evt instanceof CustomState);
//            Assert.assertTrue(evt.equals(state));
//            Assert.assertTrue(evt == state);
//
//
//            Assert.assertTrue(tempCommands.count == 1);
//
//            Assert.assertTrue(
//                "Expected last command value '21', got '" + tempCommands.lastValue + "'",
//                tempCommands.lastValue.equals("21")
//            );
//        }
//
//
//        {
//            CustomState state = new CustomState(20, "time of day", "night");
//
//            cache.update(state);
//
//            Event evt = grab.event;
//
//            Assert.assertTrue(grab.count == 2);
//
//            Assert.assertTrue(evt.getSourceID() == 20);
//            Assert.assertTrue(evt.getSource().equals("time of day"));
//            Assert.assertTrue(evt.getValue().equals("night"));
//            Assert.assertTrue(evt instanceof CustomState);
//            Assert.assertTrue(evt.equals(state));
//            Assert.assertTrue(evt == state);
//
//
//            Assert.assertTrue(tempCommands.count == 2);
//
//            Assert.assertTrue(
//                "Expected last command value '18', got '" + tempCommands.lastValue + "'",
//                tempCommands.lastValue.equals("18")
//            );
//        }
//
//
//        {
//            Switch sw = new Switch(1, "vacation start", "on", Switch.State.ON);
//
//            cache.update(sw);
//
//            Event evt = grab.event;
//
//            Assert.assertTrue(grab.count == 3);
//
//            Assert.assertTrue(evt.getSourceID() == 1);
//            Assert.assertTrue(evt.getSource().equals("vacation start"));
//            Assert.assertTrue(evt instanceof Switch);
//            Assert.assertTrue(evt.equals(sw));
//            Assert.assertTrue(evt == sw);
//
//            Assert.assertTrue(
//                "Expected command count 3, got " + tempCommands.count,
//                tempCommands.count == 3
//            );
//
//            Assert.assertTrue(
//                "Expected last command value '15', got '" + tempCommands.lastValue + "'",
//                tempCommands.lastValue.equals("15")
//            );
//
//        }
//
//
//        {
//            CustomState state = new CustomState(20, "time of day", "day");
//
//            cache.update(state);
//
//            Event evt = grab.event;
//
//            Assert.assertTrue(grab.count == 4);
//
//            Assert.assertTrue(evt.getSourceID() == 20);
//            Assert.assertTrue(evt.getSource().equals("time of day"));
//            Assert.assertTrue(evt.getValue().equals("day"));
//            Assert.assertTrue(evt instanceof CustomState);
//            Assert.assertTrue(evt.equals(state));
//            Assert.assertTrue(evt == state);
//
//
//            Assert.assertTrue(
//                "Expected command count 4, got " + tempCommands.count,
//                tempCommands.count == 4
//            );
//
//            Assert.assertTrue(
//                "Expected last command value '15', got '" + tempCommands.lastValue + "'",
//                tempCommands.lastValue.equals("15")
//            );
//        }
//
//
//        {
//            Switch sw = new Switch(1, "vacation end", Switch.State.OFF.serialize(), Switch.State.ON);
//
//            cache.update(sw);
//
//            Event evt = grab.event;
//
//            Assert.assertTrue(grab.count == 5);
//
//            Assert.assertTrue(evt.getSourceID() == 1);
//            Assert.assertTrue(evt.getSource().equals("vacation end"));
//            Assert.assertTrue(evt instanceof Switch);
//            Assert.assertTrue(evt.equals(sw));
//            Assert.assertTrue(evt == sw);
//
//            Assert.assertTrue(
//                "Expected command count 5, got " + tempCommands.count,
//                tempCommands.count == 5
//            );
//
//            Assert.assertTrue(
//                "Expected last command value '21', got '" + tempCommands.lastValue + "'",
//                tempCommands.lastValue.equals("21")
//            );
//
//        }
//
//    }
//
//
//    /**
//     * TODO
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testSwitchModifications() throws Exception {
//        String newResourcePath = AllTests.getAbsoluteFixturePath()
//            .resolve("statuscache/rules/switch/").toString();
//
//        config.setResourcePath(newResourcePath);
//
//        RuleEngine rules = new RuleEngine();
//        EventGrab grab = new EventGrab();
//
//        List<EventProcessor> processors = new ArrayList<EventProcessor>();
//        processors.add(rules);
//        processors.add(grab);
//
//        StatusCache cache = createCache(processors, "commandCounter", new CommandCounter(), "counter");
//
//        // Just run through a single event to make sure no rules get triggered...
//
//        Assert.assertTrue(grab.count == 0);
//
//        Switch sw = new Switch(555, "switch555", "on", Switch.State.ON);
//
//        cache.update(sw);
//        Event evt = grab.event;
//
//        Assert.assertTrue(grab.count == 1);
//
//        Assert.assertTrue(evt.getSourceID() == 555);
//        Assert.assertTrue(evt.getSource().equals("switch555"));
//        Assert.assertTrue(evt instanceof Switch);
//        Assert.assertTrue(evt.equals(sw));
//        Assert.assertTrue(evt == sw);
//
//        Switch sw1 = (Switch) evt;
//
//        Assert.assertTrue(sw1.getValue().equals("on"));
//        Assert.assertTrue(sw1.getState() == Switch.State.ON);
//
//
//        Switch sw2 = new Switch(444, "switch444", "off", Switch.State.OFF);
//        cache.registerSensor(new SwitchSensor("switch444", 444, cache, new TestCommand(), 1));
//
//        cache.update(sw2);
//
//        Thread.sleep(2000);
//
//        evt = grab.event;
//
//        Assert.assertTrue("Expected count 2, got " + grab.count, grab.count == 2);
//
//
//        Assert.assertTrue(evt.getSourceID() == 444);
//        Assert.assertTrue(evt.getSource().equals("switch444"));
//        Assert.assertTrue(evt instanceof Switch);
//        Assert.assertTrue(!evt.equals(sw2));
//
//        Switch sw3 = (Switch) evt;
//
//        Assert.assertTrue(
//            "Expected 'Complete', got '" + sw3.getValue() + "'",
//            sw3.getValue().equals("Complete")
//        );
//
//        Assert.assertTrue(sw3.getState() == Switch.State.ON);
//    }
//
//
//    /**
//     * Basic Level event tests to check rule execution based on event equality (which includes
//     * event source name and id and event's value. Repeated events from the same source and
//     * with same value should not re-trigger rule execution (unless the event value has from the
//     * source has changed in between).
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testLevelCommandExecution() throws Exception {
//        StatusCache cache = null;
//
//        try {
//            String newResourcePath = AllTests.getAbsoluteFixturePath()
//                .resolve("statuscache/rules/level-command-execution/").toString();
//
//            config.setResourcePath(newResourcePath);
//
//
//            RuleEngine sre = new RuleEngine();
//            EventGrab grab = new EventGrab();
//
//            List<EventProcessor> processors = new ArrayList<EventProcessor>();
//            processors.add(sre);
//            processors.add(grab);
//
//            CommandCounter commandCounter = new CommandCounter();
//            cache = createCache(processors, "commandCounter", commandCounter, "counter");
//
//
//            // Starting condition...
//
//            Assert.assertTrue(commandCounter.count == 0);
//
//
//            // should not trigger rule (command execution)...
//
//            Level level1 = new Level(1, "level-test", 50);
//
//            cache.update(level1);
//
//            Assert.assertTrue(commandCounter.count == 0);
//
//
//            Event evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("level-test"));
//            Assert.assertTrue(evt.getSourceID() == 1);
//            Assert.assertTrue(
//                "Expected 50, got " + evt.getValue(),
//                evt.getValue().equals(50)
//            );
//            Assert.assertTrue(evt.serialize().equals("50"));
//            Assert.assertTrue(evt instanceof Level);
//            Assert.assertTrue(evt.equals(level1));
//            Assert.assertTrue(evt == level1);
//
//            Level l1 = (Level) evt;
//
//            Assert.assertTrue(l1.getValue() == 50);
//
//
//            // should execute command due to above trigger level (>75)...
//
//            Level level2 = new Level(1, "level-test", 80);
//
//            cache.update(level2);
//
//            Assert.assertTrue(commandCounter.count == 1);
//
//            evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("level-test"));
//            Assert.assertTrue(evt.getSourceID() == 1);
//            Assert.assertTrue(
//                "Expected 80, got " + evt.getValue(),
//                evt.getValue().equals(80)
//            );
//            Assert.assertTrue(evt.serialize().equals("80"));
//            Assert.assertTrue(evt instanceof Level);
//            Assert.assertTrue(evt.equals(level2));
//            Assert.assertTrue(evt == level2);
//
//            Level l2 = (Level) evt;
//
//            Assert.assertTrue(l2.getValue() == 80);
//
//
//            // should execute command again...
//
//            Level level3 = new Level(1, "level-test", 90);
//
//            cache.update(level3);
//
//            Assert.assertTrue(commandCounter.count == 2);
//
//            evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("level-test"));
//            Assert.assertTrue(evt.getSourceID() == 1);
//            Assert.assertTrue(
//                "Expected 90, got " + evt.getValue(),
//                evt.getValue().equals(90)
//            );
//            Assert.assertTrue(evt.serialize().equals("90"));
//            Assert.assertTrue(evt instanceof Level);
//            Assert.assertTrue(evt.equals(level3));
//            Assert.assertTrue(evt == level3);
//
//            Level l3 = (Level) evt;
//
//            Assert.assertTrue(l3.getValue() == 90);
//
//
//            // should execute once more...
//
//            Level level4 = new Level(1, "level-test", 100);
//
//            cache.update(level4);
//
//            Assert.assertTrue(commandCounter.count == 3);
//
//            evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("level-test"));
//            Assert.assertTrue(evt.getSourceID() == 1);
//            Assert.assertTrue(
//                "Expected 100, got " + evt.getValue(),
//                evt.getValue().equals(100)
//            );
//            Assert.assertTrue(evt.serialize().equals("100"));
//            Assert.assertTrue(evt instanceof Level);
//            Assert.assertTrue(evt.equals(level4));
//            Assert.assertTrue(evt == level4);
//
//            Level l4 = (Level) evt;
//
//            Assert.assertTrue(l4.getValue() == 100);
//
//
//            // should not trigger...
//
//            Level level5 = new Level(1, "level-test", 0);
//
//            cache.update(level5);
//
//            Assert.assertTrue(commandCounter.count == 3);
//
//            evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("level-test"));
//            Assert.assertTrue(evt.getSourceID() == 1);
//            Assert.assertTrue(
//                "Expected 0, got " + evt.getValue(),
//                evt.getValue().equals(0)
//            );
//            Assert.assertTrue(evt.serialize().equals("0"));
//            Assert.assertTrue(evt instanceof Level);
//            Assert.assertTrue(evt.equals(level5));
//            Assert.assertTrue(evt == level5);
//
//            Level l5 = (Level) evt;
//
//            Assert.assertTrue(l5.getValue() == 0);
//
//
//            // should trigger again, repeating earlier value (but was zeroed in between)...
//
//            Level level6 = new Level(1, "level-test", 100);
//
//            cache.update(level6);
//
//            Assert.assertTrue(commandCounter.count == 4);
//
//            evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("level-test"));
//            Assert.assertTrue(evt.getSourceID() == 1);
//            Assert.assertTrue(
//                "Expected 100, got " + evt.getValue(),
//                evt.getValue().equals(100)
//            );
//            Assert.assertTrue(evt.serialize().equals("100"));
//            Assert.assertTrue(evt instanceof Level);
//            Assert.assertTrue(evt.equals(level6));
//            Assert.assertTrue(evt == level6);
//
//            Level l6 = (Level) evt;
//
//            Assert.assertTrue(l6.getValue() == 100);
//
//
//            // should NOT re-trigger due to equality rules...
//
//            Level level7 = new Level(1, "level-test", 100);
//
//            cache.update(level7);
//
//            Assert.assertTrue(commandCounter.count == 4);
//
//            evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("level-test"));
//            Assert.assertTrue(evt.getSourceID() == 1);
//            Assert.assertTrue(
//                "Expected 100, got " + evt.getValue(),
//                evt.getValue().equals(100)
//            );
//            Assert.assertTrue(evt.serialize().equals("100"));
//            Assert.assertTrue(evt instanceof Level);
//            Assert.assertTrue(evt.equals(level7));
//            Assert.assertTrue(evt == level7);
//
//            Level l7 = (Level) evt;
//
//            Assert.assertTrue(l7.getValue() == 100);
//        } finally {
//            if (cache != null) {
//                cache.shutdown();
//            }
//        }
//    }
//
//
//    /**
//     * Tests command execution based on a schedule (1s interval schedule)
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testScheduledCommandExecution() throws Exception {
//        StatusCache cache = null;
//
//        try {
//            String newResourcePath = AllTests.getAbsoluteFixturePath()
//                .resolve("statuscache/rules/scheduled-command-execution/").toString();
//
//            config.setResourcePath(newResourcePath);
//
//
//            RuleEngine sre = new RuleEngine();
//            EventGrab grab = new EventGrab();
//
//            List<EventProcessor> processors = new ArrayList<EventProcessor>();
//            processors.add(sre);
//            processors.add(grab);
//
//            CommandCounter commandCounter = new CommandCounter();
//
//            Assert.assertTrue(
//                "Expected 0, got " + commandCounter.count,
//                commandCounter.count == 0
//            );
//
//            cache = createCache(processors, "commandCounter", commandCounter, "counter");
//
//
//            // wait 6 seconds, we should have *minimum* of five hits by then...
//
//            Thread.sleep(6 * 1000);
//
//            Assert.assertTrue(
//                "Expect >= 5, got " + commandCounter.count,
//                commandCounter.count >= 5
//            );
//        } finally {
//            if (cache != null) {
//                cache.shutdown();
//            }
//        }
//    }
//
//
//    /**
//     * Tests rules with range events and a three-way rule definition that
//     * executes with different command parameters depending on whether the range value
//     * is at min boundary, max boundary or between boundaries.
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testSpecializedEventAPI() throws Exception {
//        StatusCache cache = null;
//
//        try {
//            String newResourcePath = AllTests.getAbsoluteFixturePath()
//                .resolve("statuscache/rules/range-limits/").toString();
//
//            config.setResourcePath(newResourcePath);
//
//            RuleEngine rules = new RuleEngine();
//
//            List<EventProcessor> processors = new ArrayList<EventProcessor>();
//            processors.add(rules);
//
//            CommandCounter commandCounter = new CommandCounter();
//
//            cache = createCache(processors, "commandCounter", commandCounter, "counter");
//
//            for (int rangeValue = 0; rangeValue < 10; rangeValue++) {
//                Range range = new Range(11, "temperature", rangeValue, -10, 10);
//
//                cache.update(range);
//
//                Assert.assertTrue(
//                    "Expected '" + rangeValue + "', got " + commandCounter.lastValue,
//                    commandCounter.lastValue.equals(Integer.toString(rangeValue))
//                );
//
//                Assert.assertTrue(commandCounter.count == rangeValue + 1);
//            }
//
//
//            {
//                Range range = new Range(11, "temperature", 10, -10, 10);
//
//                cache.update(range);
//
//                Assert.assertTrue(
//                    "Expected 'hit the max', got " + commandCounter.lastValue,
//                    commandCounter.lastValue.equals("hit the max")
//                );
//
//                Assert.assertTrue(
//                    "Expected 11, got " + commandCounter.count,
//                    commandCounter.count == 11
//                );
//            }
//
//            for (int rangeValue = 9; rangeValue > -10; rangeValue--) {
//                Range range = new Range(11, "temperature", rangeValue, -10, 10);
//
//                cache.update(range);
//
//                Assert.assertTrue(
//                    "Expected '" + rangeValue + "', got " + commandCounter.lastValue,
//                    commandCounter.lastValue.equals(Integer.toString(rangeValue))
//                );
//            }
//
//            Range range = new Range(11, "temperature", -10, -10, 10);
//
//            cache.update(range);
//
//            Assert.assertTrue(
//                "Expected 'hit the min', got " + commandCounter.lastValue,
//                commandCounter.lastValue.equals("hit the min")
//            );
//
//            Assert.assertTrue(
//                "Expected 31, got " + commandCounter.count,
//                commandCounter.count == 31
//            );
//        } finally {
//            if (cache != null) {
//                cache.shutdown();
//            }
//        }
//    }
//
//
//    /**
//     * Test parameterized command execution based on sensor event condition.
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testParameterizedCommandExecution() throws Exception {
//        StatusCache cache = null;
//
//        try {
//            String newResourcePath = AllTests.getAbsoluteFixturePath()
//                .resolve("statuscache/rules/param-command-execution/").toString();
//
//            config.setResourcePath(newResourcePath);
//
//            RuleEngine ruleEngine = new RuleEngine();
//
//            List<EventProcessor> processors = new ArrayList<EventProcessor>();
//            processors.add(ruleEngine);
//
//            CommandCounter commandCounter = new CommandCounter();
//
//            cache = createCache(processors, "commandCounter", commandCounter, "counter");
//
//            Assert.assertTrue(commandCounter.count == 0);
//
//            Switch sw1 = new Switch(100, "test sensor", "on", Switch.State.ON);
//
//            cache.update(sw1);
//
//            Assert.assertTrue(commandCounter.count == 1);
//            Assert.assertTrue(commandCounter.lastValue.equals("5"));
//        } finally {
//            if (cache != null) {
//                cache.shutdown();
//            }
//        }
//    }
//
//
//    /**
//     * Test command execution (no params) based on sensor event condition.
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testCommandExecution() throws Exception {
//        StatusCache cache = null;
//
//        try {
//            String newResourcePath = AllTests.getAbsoluteFixturePath()
//                .resolve("statuscache/rules/command-execution/").toString();
//
//            config.setResourcePath(newResourcePath);
//
//            RuleEngine rules = new RuleEngine();
//
//            List<EventProcessor> processors = new ArrayList<EventProcessor>();
//            processors.add(rules);
//
//            CommandCounter commandCounter = new CommandCounter();
//
//            cache = createCache(processors, "commandCounter", commandCounter, "counter");
//
//            Assert.assertTrue(commandCounter.count == 0);
//
//            Switch sw1 = new Switch(1, "test sensor", "on", Switch.State.ON);
//
//            cache.update(sw1);
//
//            Assert.assertTrue(commandCounter.count == 1);
//        } finally {
//            if (cache != null) {
//                cache.shutdown();
//            }
//        }
//    }
//
//
//    /**
//     * Test rule execution that require three separate switch events (sensors) to be
//     * in 'on' state or 'off' state.
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testThreeWaySwitchOn() throws Exception {
//        StatusCache cache = null;
//
//        try {
//            String newResourcePath = AllTests.getAbsoluteFixturePath()
//                .resolve("statuscache/rules/sensor-three-switches/").toString();
//
//            config.setResourcePath(newResourcePath);
//
//            RuleEngine rules = new RuleEngine();
//            EventGrab grab = new EventGrab();
//
//            List<EventProcessor> processors = new ArrayList<EventProcessor>();
//            processors.add(rules);
//            processors.add(grab);
//
//            CommandCounter commandCounter = new CommandCounter();
//
//            cache = createCache(processors, "commandCounter", commandCounter, "counter");
//
//            Assert.assertTrue(commandCounter.count == 0);
//
//
//            // Switch 1 off...
//
//            Switch sw1 = new Switch(1, "sensor 1", "off", Switch.State.OFF);
//
//            cache.update(sw1);
//
//            Assert.assertTrue(commandCounter.count == 0);
//
//            Event evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 1', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 1")
//            );
//
//            String currentState = cache.queryStatus(1);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // Switch 2 off...
//
//            Switch sw2 = new Switch(2, "sensor 2", "off", Switch.State.OFF);
//
//            cache.update(sw2);
//
//            Assert.assertTrue(commandCounter.count == 0);
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 2', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 2")
//            );
//
//            currentState = cache.queryStatus(2);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // All off...
//
//            Switch sw3 = new Switch(3, "sensor 3", "off", Switch.State.OFF);
//
//            cache.update(sw3);
//
//            Assert.assertTrue(
//                "Expected 1, got " + commandCounter.count,
//                commandCounter.count == 1
//            );
//
//            Assert.assertTrue(commandCounter.lastValue.equals("all off"));
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 3', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 3")
//            );
//
//            currentState = cache.queryStatus(3);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // Update sensor 1 'on'....
//
//            sw1 = new Switch(1, "sensor 1", "on", Switch.State.ON);
//
//            cache.update(sw1);
//
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 1', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 1")
//            );
//
//            currentState = cache.queryStatus(1);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//            Assert.assertTrue(
//                "Expected 1, got " + commandCounter.count,
//                commandCounter.count == 1
//            );
//
//
//            currentState = cache.queryStatus(2);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            currentState = cache.queryStatus(3);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // Update sensor 2 'on'....
//
//            sw2 = new Switch(2, "sensor 2", "on", Switch.State.ON);
//
//            cache.update(sw2);
//
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 2', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 2")
//            );
//
//            Assert.assertTrue(
//                "Expected 1, got " + commandCounter.count,
//                commandCounter.count == 1
//            );
//
//            currentState = cache.queryStatus(1);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//
//            currentState = cache.queryStatus(2);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//
//            currentState = cache.queryStatus(3);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // Update sensor 3 'on'...  this is where 'all on' command should trigger.
//
//            sw3 = new Switch(3, "sensor 3", "on", Switch.State.ON);
//
//            cache.update(sw3);
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'on', got '" + evt.getValue() + "'",
//                evt.getValue().equals("on")
//            );
//
//
//            Assert.assertTrue(
//                "Expected 2, got " + commandCounter.count,
//                commandCounter.count == 2
//            );
//
//            Assert.assertTrue(commandCounter.lastValue.equals("all on"));
//
//
//            currentState = cache.queryStatus(1);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//
//            currentState = cache.queryStatus(2);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//
//            currentState = cache.queryStatus(3);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//
//            // Turn all off again... random order this time.
//
//
//            // Switch 1 off...
//
//            sw2 = new Switch(2, "sensor 2", "off", Switch.State.OFF);
//
//            cache.update(sw2);
//
//            Assert.assertTrue(commandCounter.count == 2);
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 2', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 2")
//            );
//
//            currentState = cache.queryStatus(2);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // Switch 3 off...
//
//            sw3 = new Switch(3, "sensor 3", "off", Switch.State.OFF);
//
//            cache.update(sw3);
//
//            Assert.assertTrue(commandCounter.count == 2);
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 3', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 3")
//            );
//
//            currentState = cache.queryStatus(3);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // All off...
//
//            sw1 = new Switch(1, "sensor 1", "off", Switch.State.OFF);
//
//            cache.update(sw1);
//
//            Assert.assertTrue(
//                "Expected 3, got " + commandCounter.count,
//                commandCounter.count == 3
//            );
//
//            Assert.assertTrue(commandCounter.lastValue.equals("all off"));
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 1', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 1")
//            );
//
//            currentState = cache.queryStatus(1);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // Finally all on again... in reverse order.
//
//
//            sw3 = new Switch(3, "sensor 3", "on", Switch.State.ON);
//
//            cache.update(sw3);
//
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 3', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 3")
//            );
//
//            currentState = cache.queryStatus(3);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//            Assert.assertTrue(
//                "Expected 3, got " + commandCounter.count,
//                commandCounter.count == 3
//            );
//
//
//            currentState = cache.queryStatus(2);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            currentState = cache.queryStatus(1);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // Update sensor 2 'on'....
//
//            sw2 = new Switch(2, "sensor 2", "on", Switch.State.ON);
//
//            cache.update(sw2);
//
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'sensor 2', got '" + evt.getSource() + "'",
//                evt.getSource().equals("sensor 2")
//            );
//
//            Assert.assertTrue(
//                "Expected 3, got " + commandCounter.count,
//                commandCounter.count == 3
//            );
//
//            currentState = cache.queryStatus(3);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//
//            currentState = cache.queryStatus(2);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//
//            currentState = cache.queryStatus(1);
//
//            Assert.assertTrue(currentState.equals("off"));
//
//
//            // Update sensor 1 'on'...  this is where 'all on' command should trigger.
//
//            sw1 = new Switch(1, "sensor 1", "on", Switch.State.ON);
//
//            cache.update(sw1);
//
//            evt = grab.event;
//
//            Assert.assertTrue(
//                "Expected 'on', got '" + evt.getValue() + "'",
//                evt.getValue().equals("on")
//            );
//
//
//            Assert.assertTrue(
//                "Expected 4, got " + commandCounter.count,
//                commandCounter.count == 4
//            );
//
//            Assert.assertTrue(commandCounter.lastValue.equals("all on"));
//
//
//            currentState = cache.queryStatus(1);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//
//            currentState = cache.queryStatus(2);
//
//            Assert.assertTrue(currentState.equals("on"));
//
//
//            currentState = cache.queryStatus(3);
//
//            Assert.assertTrue(currentState.equals("on"));
//        } finally {
//            cache.shutdown();
//        }
//
//    }
//
//
//    /**
//     * Test rule condition based on two existing sensor events.
//     *
//     * @throws Exception if test fails
//     */
//    @Test
//    public void testRuleConditionOnExistingSensorValues() throws Exception {
//
//        StatusCache cache = null;
//
//        try {
//            String newResourcePath = AllTests.getAbsoluteFixturePath()
//                .resolve("statuscache/rules/sensors/").toString();
//
//            config.setResourcePath(newResourcePath);
//
//            RuleEngine rules = new RuleEngine();
//            EventGrab grab = new EventGrab();
//
//            List<EventProcessor> processors = new ArrayList<EventProcessor>();
//            processors.add(rules);
//            processors.add(grab);
//
//            CommandCounter commandCounter = new CommandCounter();
//
//            cache = createCache(processors, "commandCounter", commandCounter, "counter");
//
//
//            Assert.assertTrue(commandCounter.count == 0);
//
//
//            // should not trigger on existence of first event...
//
//            Switch sw1 = new Switch(999, "my event", "off", Switch.State.OFF);
//
//            cache.update(sw1);
//
//            Assert.assertTrue(commandCounter.count == 0);
//
//            Event evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("my event"));
//            Assert.assertTrue(evt.getSourceID() == 999);
//            Assert.assertTrue(
//                "Expected 'off', got '" + evt.getValue() + "'",
//                evt.getValue().equals("off")
//            );
//            Assert.assertTrue(evt.serialize().equals("off"));
//            Assert.assertTrue(evt instanceof Switch);
//            Assert.assertTrue(evt.equals(sw1));
//            Assert.assertTrue(evt == sw1);
//
//            Switch swevt = (Switch) evt;
//
//            Assert.assertTrue(swevt.getState().equals(Switch.State.OFF));
//
//
//            // should trigger on existence of both events....
//
//            Switch sw2 = new Switch(1000, "test sensor 1", "off", Switch.State.OFF);
//
//            cache.update(sw2);
//
//            Assert.assertTrue(commandCounter.count == 1);
//            Assert.assertTrue(commandCounter.lastValue.equals("triggered"));
//
//            evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("test sensor 1"));
//            Assert.assertTrue(evt.getSourceID() == 1000);
//            Assert.assertTrue(
//                "Expected 'off', got '" + evt.getValue() + "'",
//                evt.getValue().equals("off")
//            );
//            Assert.assertTrue(evt.serialize().equals("off"));
//            Assert.assertTrue(evt instanceof Switch);
//            Assert.assertTrue(evt.equals(sw2));
//            Assert.assertTrue(evt == sw2);
//
//            swevt = (Switch) evt;
//
//            Assert.assertTrue(swevt.getState().equals(Switch.State.OFF));
//
//
//            // should not re-trigger due to equivalence rules...
//
//            cache.update(sw1);
//
//            evt = grab.event;
//
//            Assert.assertTrue(commandCounter.count == 1);
//
//            Assert.assertTrue(evt.getSource().equals("my event"));
//            Assert.assertTrue(evt.getSourceID() == 999);
//            Assert.assertTrue(
//                "Expected 'off', got '" + evt.getValue() + "'",
//                evt.getValue().equals("off")
//            );
//            Assert.assertTrue(evt.serialize().equals("off"));
//            Assert.assertTrue(evt instanceof Switch);
//            Assert.assertTrue(evt.equals(sw1));
//            Assert.assertTrue(evt == sw1);
//
//            swevt = (Switch) evt;
//
//            Assert.assertTrue(swevt.getState().equals(Switch.State.OFF));
//
//
//            // should not re-trigger due to equivalence rules....
//
//            cache.update(sw2);
//
//            Assert.assertTrue(commandCounter.count == 1);
//
//            evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("test sensor 1"));
//            Assert.assertTrue(evt.getSourceID() == 1000);
//            Assert.assertTrue(
//                "Expected 'off', got '" + evt.getValue() + "'",
//                evt.getValue().equals("off")
//            );
//            Assert.assertTrue(evt.serialize().equals("off"));
//            Assert.assertTrue(evt instanceof Switch);
//            Assert.assertTrue(evt.equals(sw2));
//            Assert.assertTrue(evt == sw2);
//
//            swevt = (Switch) evt;
//
//            Assert.assertTrue(swevt.getState().equals(Switch.State.OFF));
//
//
//            // should trigger due to changed event value...
//
//            sw2 = new Switch(1000, "test sensor 1", "on", Switch.State.ON);
//
//            cache.update(sw2);
//
//            Assert.assertTrue(commandCounter.count == 2);
//
//            evt = grab.event;
//
//            Assert.assertTrue(evt.getSource().equals("test sensor 1"));
//            Assert.assertTrue(evt.getSourceID() == 1000);
//            Assert.assertTrue(
//                "Expected 'on', got '" + evt.getValue() + "'",
//                evt.getValue().equals("on")
//            );
//            Assert.assertTrue(evt.serialize().equals("on"));
//            Assert.assertTrue(evt instanceof Switch);
//            Assert.assertTrue(evt.equals(sw2));
//            Assert.assertTrue(evt == sw2);
//
//            swevt = (Switch) evt;
//
//            Assert.assertTrue(swevt.getState().equals(Switch.State.ON));
//
//
//            // should trigger again due to second event's changed value...
//
//            sw1 = new Switch(999, "my event", "on", Switch.State.ON);
//
//            cache.update(sw1);
//
//            evt = grab.event;
//
//            Assert.assertTrue(commandCounter.count == 3);
//
//            Assert.assertTrue(evt.getSource().equals("my event"));
//            Assert.assertTrue(evt.getSourceID() == 999);
//            Assert.assertTrue(
//                "Expected 'on', got '" + evt.getValue() + "'",
//                evt.getValue().equals("on")
//            );
//            Assert.assertTrue(evt.serialize().equals("on"));
//            Assert.assertTrue(evt instanceof Switch);
//            Assert.assertTrue(evt.equals(sw1));
//            Assert.assertTrue(evt == sw1);
//
//            swevt = (Switch) evt;
//
//            Assert.assertTrue(swevt.getState().equals(Switch.State.ON));
//        } finally {
//            if (cache != null) {
//                cache.shutdown();
//            }
//        }
//    }
//
//
//    /**
//     * Test implementation behavior when controller configuration for finding/reading
//     * rules is incorrect.
//     */
//    @Test
//    public void testWhenNoRuleDir() {
//        String newResourcePath = AllTests.getAbsoluteFixturePath()
//            .resolve("statuscache/rules/no-rule-dir/").toString();
//
//        config.setResourcePath(newResourcePath);
//
//        RuleEngine rules = new RuleEngine();
//
//        try {
//            rules.init();
//            rules.start(new LifeCycleEvent(null));
//
//            Assert.fail("Should not get here...");
//        } catch (InitializationException e) {
//            // Expected -- controller configuration is incorrect if no rule dir is present
//        }
//
//    }
//
//
//    // Helpers --------------------------------------------------------------------------------------
//
//
//    private StatusCache createCache(List<EventProcessor> processors,
//                                    String protocolType, CommandBuilder commandBuilder,
//                                    String commandName) throws Exception {
//        ChangedStatusTable cst = new ChangedStatusTable();
//        EventProcessorChain epc = new EventProcessorChain();
//
//        epc.setEventProcessors(processors);
//        epc.init();
//
//        StatusCache cache = new StatusCache(cst, epc);
//
//        Map<String, CommandBuilder> builders = new HashMap<String, CommandBuilder>();
//        builders.put(protocolType, commandBuilder);
//
//        CommandFactory cf = new CommandFactory(builders);
//
//        Element cmdElement = new Element("command", ModelBuilder.SchemaVersion.OPENREMOTE_NAMESPACE);
//        cmdElement.setAttribute("id", "10");
//        cmdElement.setAttribute("protocol", protocolType);
//
//        Element nameProp = new Element("property", ModelBuilder.SchemaVersion.OPENREMOTE_NAMESPACE);
//        nameProp.setAttribute("name", "name");
//        nameProp.setAttribute("value", commandName);
//
//        Set<Element> content = new HashSet<Element>();
//        content.add(nameProp);
//
//        cmdElement.addContent(content);
//
//        Version20CommandBuilder cmdBuilder = new Version20CommandBuilder(cf);
//        Command cmd = cmdBuilder.build(cmdElement);
//
//        Set<Command> commands = new HashSet<Command>();
//        commands.add(cmd);
//
//        cache.initializeEventContext(commands);
//
//        cache.start();
//
//        return cache;
//    }
//
//
//    // Nested Classes -------------------------------------------------------------------------------
//

//    private static class CommandCounter implements CommandBuilder {
//        private int count = 0;
//        private String lastValue = "<nothing>";
//
//
//        @Override
//        public org.openremote.controller.command.Command build(Element el) {
//            count++;
//
//            Attribute attribute = el.getAttribute(
//                org.openremote.controller.command.Command.DYNAMIC_VALUE_ATTR_NAME
//            );
//
//            if (attribute != null) {
//                lastValue = attribute.getValue();
//            }
//
//            return new ExecutableCommand() {
//                @Override
//                public void send() {
//
//                }
//            };
//        }
//    }
//

}

