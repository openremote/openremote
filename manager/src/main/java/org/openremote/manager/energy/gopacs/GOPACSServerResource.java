package org.openremote.manager.energy.gopacs;

import jakarta.ws.rs.*;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static jakarta.ws.rs.core.MediaType.TEXT_XML;

@Path("message")
public interface GOPACSServerResource {

    @POST
    @Consumes({APPLICATION_XML, TEXT_XML})
    void inMessage(String transportXml);
}
