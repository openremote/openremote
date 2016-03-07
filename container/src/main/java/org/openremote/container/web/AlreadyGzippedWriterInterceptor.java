package org.openremote.container.web;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;
import java.util.Arrays;

@Provider
@Priority(Priorities.ENTITY_CODER+1)
public class AlreadyGzippedWriterInterceptor implements WriterInterceptor {

    // TODO configurable
    public static final String[] alreadyZippedMediaTypes = new String[] {
        "application/vnd.mapbox-vector-tile"
    };

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        if (context.getMediaType() != null)  {
            if (Arrays.asList(alreadyZippedMediaTypes).contains(context.getMediaType().toString())) {
                context.getHeaders().putSingle("Content-Encoding", "gzip");
            }
        }
        context.proceed();
    }
}