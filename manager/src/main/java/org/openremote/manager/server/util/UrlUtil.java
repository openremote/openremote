package org.openremote.manager.server.util;

import io.mikael.urlbuilder.UrlBuilder;
import io.vertx.ext.web.RoutingContext;

public class UrlUtil {

    public static UrlBuilder url(RoutingContext routingContext, String context, String... pathSegments) {
        return UrlBuilder.fromString(routingContext.request().absoluteURI())
            .withPath(getPath(context, pathSegments));
    }

    public static UrlBuilder url(String context, String... pathSegments) {
        return UrlBuilder.empty()
            .withPath(getPath(context, pathSegments));
    }


    public static UrlBuilder url(String baseUrl, String context, String... pathSegments) {
        return UrlBuilder.fromString(baseUrl)
            .withPath(getPath(context, pathSegments));
    }

    public static UrlBuilder url(String scheme, String host) {
        return UrlBuilder.empty()
            .withScheme(scheme)
            .withHost(host);
    }

    public static UrlBuilder url(String scheme, String host, Integer port, String context, String... pathSegments) {
        return UrlBuilder.empty()
            .withScheme(scheme)
            .withHost(host)
            .withPort(port)
            .withPath(getPath(context, pathSegments));
    }

    public static String getPath(String context, String... pathSegments) {
        StringBuilder path = new StringBuilder();
        if (context != null) {
            path.append(!context.startsWith("/") ? "/" : "").append(context);
        }
        if (pathSegments != null) {
            for (String pathSegment : pathSegments) {
                path.append(!pathSegment.startsWith("/") ? "/" : "").append(pathSegment);
            }
        }
        return path.toString();
    }
}
