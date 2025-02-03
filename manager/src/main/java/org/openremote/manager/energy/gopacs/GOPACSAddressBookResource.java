package org.openremote.manager.energy.gopacs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;

@Path("v2/participants/DSO")
public interface GOPACSAddressBookResource {
    @GET
    @Consumes(APPLICATION_XML)
    Response fetchParticipants(@QueryParam("contractedEan") String contractedEan);
}
