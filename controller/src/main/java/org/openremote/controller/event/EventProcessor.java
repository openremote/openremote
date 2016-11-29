package org.openremote.controller.event;

import org.openremote.controller.model.Deployment;

/**
 * Process events before they are reaching the controller context.
 */
public abstract class EventProcessor {

    public void start(Deployment deployment) throws Exception{
    }

    public void stop() {
    }

    public abstract void process(EventProcessingContext ctx);

    public abstract String getName();
}

