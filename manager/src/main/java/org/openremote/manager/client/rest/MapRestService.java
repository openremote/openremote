package org.openremote.manager.client.rest;

import org.fusesource.restygwt.client.MethodCallback;
import org.fusesource.restygwt.client.RestService;
import org.fusesource.restygwt.client.TextCallback;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Created by Richard on 19/02/2016.
 */
public interface MapRestService extends RestService {

    @GET
    @Path("/map")
    void getOptions(TextCallback getOptionsCallback);

}
