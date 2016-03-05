package org.openremote.container.security;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * Add Basic authentication header if request context is configured with clientId and clientSecret.
 */
public class ClientSecretRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        String clientId = (String) requestContext.getConfiguration().getProperty("clientId");
        String clientSecret = (String) requestContext.getConfiguration().getProperty("clientSecret");
        if (clientSecret != null) {
            try {
                String authorization = "Basic " + Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes("utf-8")
                );
                requestContext.getHeaders().add(AUTHORIZATION, authorization);
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}

