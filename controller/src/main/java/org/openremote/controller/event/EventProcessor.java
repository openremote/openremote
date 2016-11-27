package org.openremote.controller.event;

import org.openremote.controller.rules.CommandFacade;

public abstract class EventProcessor {

    public void start(CommandFacade commandFacade) throws Exception{
    }

    public void stop() {
    }

    public abstract void process(EventProcessingContext ctx);

    public abstract String getName();
}

