package org.openremote.manager.energy.gopacs;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.lfenergy.shapeshifter.api.PayloadMessageType;
import org.lfenergy.shapeshifter.core.model.IncomingUftpMessage;

import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;

@Path("shapeshifter/api/v3")
public interface GoPacsResource {

    @POST
    @Path("message")
    @Consumes(APPLICATION_XML)
    @Produces(APPLICATION_XML)
    void message(IncomingUftpMessage<? extends PayloadMessageType> message);
}
