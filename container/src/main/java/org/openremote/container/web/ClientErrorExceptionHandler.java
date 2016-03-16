package org.openremote.container.web;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

@Provider
public class ClientErrorExceptionHandler implements ExceptionMapper<ClientErrorException> {

    private static final Logger LOG = Logger.getLogger(ClientErrorExceptionHandler.class.getName());

    @Override
    public Response toResponse(ClientErrorException exception) {
        // We must build a new Response, and not just pass on the exception's ("Apache HttpClient") response
        return Response.status(exception.getResponse().getStatus()).build();
    }
}
