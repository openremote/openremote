package org.openremote.controller.event;

public abstract class EventProcessor {

    public void init() throws Exception{
    }

    public void start(CommandFacade commandFacade) throws Exception{
    }

    public void stop() {
    }

    public abstract void push(EventContext ctx);

    public abstract String getName();
}

