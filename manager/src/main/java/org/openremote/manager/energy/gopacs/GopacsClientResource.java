package org.openremote.manager.energy.gopacs;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.lfenergy.shapeshifter.api.PayloadMessageType;
import org.lfenergy.shapeshifter.core.model.OutgoingUftpMessage;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;

@Path("shapeshifter/api/v3")

public interface GopacsClientResource {
    @POST
    @Path("message")
    @Consumes(APPLICATION_XML)
    @Produces(APPLICATION_XML)
    void outMessage(OutgoingUftpMessage<? extends PayloadMessageType> message);

    @GET
    @Path("participants/DSO")
    @Consumes(APPLICATION_XML)
    Response fetchParticipants(@QueryParam("contractedEan") String contractedEan);
}
