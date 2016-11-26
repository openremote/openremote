package org.openremote.controller.command;

/**
 * A tagging interface between two different types of event producers -- a pull
 * command (an explicit request for device state) and a push command (for 'active'
 * devices that broadcast their state).
 */
public interface EventProducerCommand extends Command {
}
