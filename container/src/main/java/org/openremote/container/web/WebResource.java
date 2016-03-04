package org.openremote.container.web;

import org.openremote.container.Container;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class WebResource {

    @Context
    protected Application application;

    @Context
    UriInfo uriInfo;

    public WebApplication getApplication() {
        return (WebApplication) application;
    }

    public Container getContainer() {
        return getApplication().getContainer();
    }

    public String getRealm() {
        String realm = uriInfo.getQueryParameters().getFirst("realm");
        if (realm == null || realm.length() == 0) {
            throw new WebApplicationException("Missing realm parameter", BAD_REQUEST);
        }
        return realm;
    }
}
