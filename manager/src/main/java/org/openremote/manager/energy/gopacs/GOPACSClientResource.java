package org.openremote.manager.energy.gopacs;

import jakarta.ws.rs.*;
import org.lfenergy.shapeshifter.api.PayloadMessageType;
import org.lfenergy.shapeshifter.core.model.OutgoingUftpMessage;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static jakarta.ws.rs.core.MediaType.TEXT_XML;

@Path("shapeshifter/api/v3")
public interface GOPACSClientResource {
    @POST
    @Path("message")
    @Consumes({APPLICATION_XML, TEXT_XML})
    @Produces({APPLICATION_XML, TEXT_XML})
    void outMessage(String message);
}
