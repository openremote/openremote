package org.openremote.container.web;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

import static javax.ws.rs.core.HttpHeaders.CONTENT_ENCODING;

/**
 * If content is already zipped we need a way to disable RESTEasy's stupid automatic zipping.
 */
@Provider
@Priority(Priorities.ENTITY_CODER)
public class GZIPPEDEncodingInterceptor implements WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        Object encoding = context.getHeaders().getFirst(CONTENT_ENCODING);

        if (encoding != null && encoding.toString().equalsIgnoreCase("gzipped")) {
            // It's already zipped... just add the header
            context.getHeaders().putSingle(CONTENT_ENCODING, "gzip");
        }
        context.proceed();
    }
}
