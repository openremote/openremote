package org.openremote.manager.energy.gopacs;

import jakarta.ws.rs.*;
import org.lfenergy.shapeshifter.api.PayloadMessageType;
import org.lfenergy.shapeshifter.core.model.OutgoingUftpMessage;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static jakarta.ws.rs.core.MediaType.TEXT_XML;

@Path("shapeshifter/api/v3/message")
public interface GOPACSClientResource {
    @POST
    @Consumes({APPLICATION_XML, TEXT_XML})
    @Produces({APPLICATION_XML, TEXT_XML})
    void outMessage(String message);
}
