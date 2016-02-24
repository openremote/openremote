package org.openremote.manager.client.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * Created by Richard on 24/02/2016.
 */
public interface AssetRestService {
    @GET
    @Path("/")
    void getAssets();
}
