package org.openremote.controller.event;

import org.openremote.controller.model.CommandDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CommandFacade {

    private static final Logger LOG = Logger.getLogger(CommandFacade.class.getName());

    final protected Map<String, CommandDefinition> namedCommands = new HashMap<>();

    public CommandFacade(Collection<CommandDefinition> commandDefinitions) {
        for (CommandDefinition cmd : commandDefinitions) {
            String cmdName = cmd.getProperty(CommandDefinition.NAME_PROPERTY);
            if (cmdName != null && !cmdName.equals("")) {
                namedCommands.put(cmdName, cmd);
            } else {
                LOG.warning("Ignoring command definition without command name: " + cmd);
            }
        }
    }

    public void command(String name) {
        command(name, null);    // null == no param
    }

    public void command(String name, String param) {
        if (name == null || name.equals("")) {
            LOG.info("Empty command name, ignoring execution (are all your rules executing commands with proper names?)");
            return;
        }

        CommandDefinition cmd = namedCommands.get(name);

        if (cmd == null) {
            LOG.warning("Command definition not found, ignoring execution: " + name);
            return;
        }

        LOG.fine("Preparing and executing: " + cmd);
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

