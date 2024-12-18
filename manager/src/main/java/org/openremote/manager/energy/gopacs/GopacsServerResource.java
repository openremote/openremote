package org.openremote.manager.energy.gopacs;

import jakarta.ws.rs.*;
import org.lfenergy.shapeshifter.api.PayloadMessageType;
import org.lfenergy.shapeshifter.core.model.IncomingUftpMessage;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;

@Path("shapeshifter/api/v3")
public interface GopacsServerResource {

    @POST
    @Path("message")
    @Consumes(APPLICATION_XML)
    @Produces(APPLICATION_XML)
    void inMessage(IncomingUftpMessage<? extends PayloadMessageType> message);


}
