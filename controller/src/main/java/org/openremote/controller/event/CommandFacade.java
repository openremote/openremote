package org.openremote.controller.event;

import org.openremote.controller.model.CommandDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CommandFacade {

    final protected Map<String, CommandDefinition> namedCommands = new HashMap<>();

    public CommandFacade(Collection<CommandDefinition> commandDefinitions) {
        for (CommandDefinition cmd : commandDefinitions) {
            String cmdName = cmd.getProperty(CommandDefinition.COMMAND_NAME_PROPERTY);
            if (cmdName != null && !cmdName.equals("")) {
                namedCommands.put(cmdName, cmd);
            }
        }
    }

    public void command(String name) {
        command(name, null);    // null == no param
    }

    public void command(String name, String param) {
        if (name == null || name.equals("")) {
            // TODO log
            return;
        }

        CommandDefinition cmd = namedCommands.get(name);

        if (cmd == null) {
            // TODO log
            return;
        }

        if (param == null) {
            cmd.execute();
        } else {
            cmd.execute(param);
        }
    }

    public void command(String name, int value) {
        command(name, Integer.toString(value));
    }

}

