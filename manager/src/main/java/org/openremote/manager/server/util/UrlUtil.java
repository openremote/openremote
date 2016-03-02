package org.openremote.manager.server.util;

import io.mikael.urlbuilder.UrlBuilder;
import io.vertx.ext.web.RoutingContext;

public class UrlUtil {

    /**
     * Resets the path on the absolute request URL, starting with the context base path followed by the segments.
     */
    public static UrlBuilder url(RoutingContext routingContext, String context, String... pathSegments) {
        return UrlBuilder.fromString(routingContext.request().absoluteURI())
            .withPath(getPath(UrlBuilder.empty().withPath(context), pathSegments));
    }

    /**
     * Appends the path segments on the base URL.
     */
    public static UrlBuilder url(String baseUrl, String... pathSegments) {
        UrlBuilder baseUrlBuilder = UrlBuilder.fromString(baseUrl);
        return UrlBuilder.fromString(baseUrl)
            .withPath(getPath(baseUrlBuilder, pathSegments));
    }

    /**
     * Resets the path with provided segments on the base URL.
     */
    public static UrlBuilder urlWithPath(String baseUrl, String... pathSegments) {
        return UrlBuilder.fromString(baseUrl)
            .withPath(getPath(pathSegments));
    }

    public static UrlBuilder url(String scheme, String host) {
        return UrlBuilder.empty()
            .withScheme(scheme)
            .withHost(host);
    }

    public static UrlBuilder url(String scheme, String host, Integer port, String... pathSegments) {
        return url(scheme, host)
            .withPort(port == null || port == 80 || port == 443 ? null : port)
            .withPath(getPath(pathSegments));
    }

    public static String getPath(String... pathSegments) {
        return getPath(null, pathSegments);
    }

    public static String getPath(UrlBuilder basePathBuilder, String... pathSegments) {
        StringBuilder path = new StringBuilder();
        if (basePathBuilder != null) {
            String basePath = basePathBuilder.path;
            if (basePath != null && basePath.length() > 0)
                path.append(!basePath.startsWith("/") ? "/" : "").append(basePath);
        }

        if (pathSegments != null) {
            for (String pathSegment : pathSegments) {
                path.append(!pathSegment.startsWith("/") ? "/" : "").append(pathSegment);
            }
        }
        return path.toString();
    }
}
