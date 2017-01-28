package org.openremote.test.rules;

import org.openremote.agent.command.Command;
import org.openremote.agent.command.CommandBuilder;
import org.openremote.agent.command.PullCommand;
import org.openremote.agent.command.builtin.DateTimeCommand;
import org.openremote.agent.deploy.CommandDefinition;
import org.openremote.agent.sensor.Sensor;

import java.util.logging.Logger;

public class CustomTimeCommandBuilder extends CommandBuilder {

    private static final Logger LOG = Logger.getLogger(CustomTimeCommandBuilder.class.getName());

    public String currentTime;

    interface MyCustomCommand extends PullCommand {
        @Override
        default int getPollingInterval() {
            return 1000; // We don't want polling to happen in "quick" tests (we get initial state poll)
        }
    }

    @Override
    public Command build(CommandDefinition commandDefinition) {
        if (commandDefinition.getProtocolType().equals("datetime")) {
            return new MyCustomCommand() {
                DateTimeCommand dtcmd = new DateTimeCommand(commandDefinition);

                @Override
                public String read(Sensor sensor) {
                    return dtcmd.calculateData(currentTime);
                }
            };

        }
        return super.build(commandDefinition);
    }
}