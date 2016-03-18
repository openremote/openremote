package org.openremote.container.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openremote.container.Container;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    final protected Container container;

    public JacksonConfig(Container container) {
        this.container = container;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return container.JSON;
    }
}