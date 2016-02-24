package org.openremote.manager.server.util;

import io.mikael.urlbuilder.UrlBuilder;
import io.vertx.ext.web.RoutingContext;

public class UrlUtil {

    public static UrlBuilder url(RoutingContext routingContext, String realm, String... pathSegments) {
        return UrlBuilder.fromString(routingContext.request().absoluteURI())
            .withPath(getPath(realm, pathSegments));
    }

    public static UrlBuilder url(String realm, String... pathSegments) {
        return UrlBuilder.empty()
            .withPath(getPath(realm, pathSegments));
    }


    public static UrlBuilder url(String baseUrl, String realm, String... pathSegments) {
        return UrlBuilder.fromString(baseUrl)
            .withPath(getPath(realm, pathSegments));
    }

    public static UrlBuilder url(String scheme, String host) {
        return UrlBuilder.empty()
            .withScheme(scheme)
            .withHost(host);
    }

    public static UrlBuilder url(String scheme, String host, Integer port, String realm, String... pathSegments) {
        return UrlBuilder.empty()
            .withScheme(scheme)
            .withHost(host)
            .withPort(port)
            .withPath(getPath(realm, pathSegments));
    }

    protected static String getPath(String realm, String... pathSegments) {
        StringBuilder path = new StringBuilder();
        path.append("/").append(realm);
        if (pathSegments != null) {
            for (String pathSegment : pathSegments) {
                path
                    .append(pathSegment.startsWith("/") ? "" : "/")
                    .append(pathSegment);
            }
        }
        return path.toString();
    }
}
