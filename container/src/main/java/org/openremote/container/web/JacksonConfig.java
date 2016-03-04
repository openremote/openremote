package org.openremote.container.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openremote.container.ContainerRuntime;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return ContainerRuntime.JSON;
    }
}