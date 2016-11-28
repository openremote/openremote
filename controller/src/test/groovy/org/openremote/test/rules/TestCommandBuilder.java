package org.openremote.test.rules;

import org.openremote.controller.command.Command;
import org.openremote.controller.command.CommandBuilder;
import org.openremote.controller.command.ExecutableCommand;
import org.openremote.controller.command.PullCommand;
import org.openremote.controller.deploy.CommandDefinition;

import java.util.logging.Logger;

public class TestCommandBuilder extends CommandBuilder {

    private static final Logger LOG = Logger.getLogger(TestCommandBuilder.class.getName());

    public String lastExecutionArgument;

    interface TestPullCommand extends PullCommand {
        @Override
        default int getPollingInterval() {
            return 10000; // We don't want polling to happen in "quick" tests (we get initial state poll)
        }
    }

    @Override
    public Command build(CommandDefinition commandDefinition) {
        Command command = super.build(commandDefinition);
        if (command != null)
            return command;

        if (!commandDefinition.getProtocolType().equals("test"))
            throw new IllegalArgumentException("Don't know how to build protocol: " + commandDefinition.getProtocolType());

        switch(commandDefinition.getProperty("what-are-we-testing")) {
            case "a-test-custom-state":
                return (TestPullCommand) sensor -> "foo";
            case "a-test-switch":
                return (TestPullCommand) sensor -> "on";
            case "a-test-range":
                return (TestPullCommand) sensor -> "123";
            case "a-test-level":
                return (TestPullCommand) sensor -> "55";
            case "an-executable-command":
                return (ExecutableCommand) value -> {
                    LOG.info("### Executing " + commandDefinition + " with arg: " + value);
                    lastExecutionArgument = value;
                };
            default:
                throw new UnsupportedOperationException("Can't build test command: " + commandDefinition);
        }
    }
}
