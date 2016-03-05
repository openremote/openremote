package org.openremote.container.security;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * Add Bearer authentication header if request context is configured with accessToken.
 */
public class BearerAuthClientRequestFilter implements ClientRequestFilter {
    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String accessToken = (String) requestContext.getConfiguration().getProperty("accessToken");
        if (accessToken != null) {
            String authorization = "Bearer " + accessToken;
            requestContext.getHeaders().add(AUTHORIZATION, authorization);
        }
    }
}

