package org.openremote.manager.client.rest;

import org.fusesource.restygwt.client.RestService;
import org.fusesource.restygwt.client.TextCallback;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by Richard on 19/02/2016.
 */
public interface MapRestService extends RestService {

    @GET
    @Path("/map")
    @Produces(MediaType.APPLICATION_JSON)
    void getOptions(TextCallback optionsCallback);

}
