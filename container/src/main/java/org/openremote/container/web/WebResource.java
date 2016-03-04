package org.openremote.container.web;

import org.openremote.container.Container;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

public class WebResource {

    @Context
    protected Application application;

    public WebApplication getApplication() {
        return (WebApplication) application;
    }

    public Container getContainer() {
        return getApplication().getContainer();
    }
}
