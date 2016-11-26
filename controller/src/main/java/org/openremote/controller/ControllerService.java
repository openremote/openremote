package org.openremote.controller;

import org.openremote.container.Container;
import org.openremote.container.ContainerService;
import org.openremote.controller.event.EventProcessorChain;
import org.openremote.controller.statuscache.ChangedStatusTable;
import org.openremote.controller.statuscache.StatusCache;

public class ControllerService implements ContainerService {

    final protected EventProcessorChain eventProcessorChain;

    protected StatusCache cache;

    public ControllerService(EventProcessorChain eventProcessorChain) {
        this.eventProcessorChain =eventProcessorChain;
    }

    @Override
    public void init(Container container) throws Exception {
    }

    @Override
    public void configure(Container container) throws Exception {
        ChangedStatusTable changeStatusTable = new ChangedStatusTable();
        cache = new StatusCache(changeStatusTable, eventProcessorChain);
    }

    @Override
    public void start(Container container) throws Exception {
        if (cache != null)
            cache.start();
    }

    @Override
    public void stop(Container container) throws Exception {
        if (cache != null) {
            cache.shutdown();
        }
    }

    public StatusCache getCache() {
        return cache;
    }
}
