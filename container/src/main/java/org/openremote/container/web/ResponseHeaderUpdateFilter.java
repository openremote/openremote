package org.openremote.container.web;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A filter for updating response headers.
 */
public class ResponseHeaderUpdateFilter implements ClientResponseFilter {

    protected Set<Map.Entry<String, List<String>>> headers;

    public ResponseHeaderUpdateFilter(Map<String, List<String>> headers) {
        this.headers = headers.entrySet();
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        headers.forEach(headerAndValues -> {
            responseContext.getHeaders().remove(headerAndValues.getKey());
            if (!headerAndValues.getValue().isEmpty()) {
                responseContext.getHeaders().addAll(headerAndValues.getKey(), headerAndValues.getValue());
            }
        });
    }
}
