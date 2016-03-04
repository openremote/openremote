package org.openremote.container.web;

import org.openremote.container.Container;

import javax.ws.rs.core.Application;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class WebApplication extends Application {

    private static final Logger LOG = Logger.getLogger(WebApplication.class.getName());

    protected final Container container;
    protected final Set<Class<?>> classes;
    protected final Set<Object> singletons;

    public WebApplication(Container container, Collection<Class<?>> apiClasses, Collection<Object> apiSingletons) {
        this.container = container;
        this.classes = apiClasses != null ? new HashSet<>(apiClasses) : null;
        this.singletons = apiSingletons != null ? new HashSet<>(apiSingletons) : null;
    }

    @Override
    public Set<Class<?>> getClasses() {
        return classes;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    public Container getContainer() {
        return container;
    }
}

