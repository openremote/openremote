package org.openremote.controller.event;

import org.openremote.controller.rules.CommandFacade;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A chain of event processors that incoming events (values) are passing through before
 * a value might change in the data context.
 *
 * Event processors may modify the existing event payload value, discard events
 * entirely or spawn multiple other events that are interacting with the data context.
 *
 * TODO Or, as we call it now, a Camel route.
 */
public class EventProcessorChain {

    private static final Logger LOG = Logger.getLogger(EventProcessorChain.class.getName());

    final protected CommandFacade commandFacade;

    /**
     * Contains the ordered list of configured event processors for this chain.
     */
    final protected List<EventProcessor> processors;

    public EventProcessorChain(CommandFacade commandFacade, EventProcessor... processors) {
        this.processors = Arrays.asList(processors);
        this.commandFacade = commandFacade;
    }

    public void start() {
        for (EventProcessor ep : processors) {
            try {
                LOG.fine("Starting event processor: " + ep.getName());
                ep.start(commandFacade);
                LOG.info("Started event processor: " + ep.getName());
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Cannot start event processor: " + ep.getName(), t);
            }
        }
    }

    public void stop() {
        for (EventProcessor ep : processors) {
            try {
                LOG.fine("Stopping event processor: " + ep.getName());
                ep.stop();
                LOG.info("Stopped event processor: " + ep.getName());
            } catch (Throwable t) {
                LOG.log(Level.SEVERE, "Cannot stop event processor: " + ep.getName(), t);
            }
        }
    }

    /**
     * Pushes an event through a stack of event processors. The returned event is the result of
     * modifications of all configured event processors (specifically as returned by the last
     * event processor in the stack).
     */
    public void push(EventProcessingContext ctx) {
        for (EventProcessor processor : processors) {
            LOG.fine("Processing: " + ctx.getEvent());
            processor.process(ctx);
            if (ctx.hasTerminated()) {
                LOG.log(Level.FINE, "Ignoring original event, context was terminated (new event?), chain stopped at: " + processor);
                return;
            } else {
                LOG.fine("Event '" + ctx.getEvent() + "' processed by: " + processor.getName());
            }
        }
        LOG.fine("Processing of event complete: " + ctx.getEvent());
    }
}

