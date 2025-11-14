package org.openremote.container.web;

import org.openremote.model.util.Config;
import org.openremote.model.util.TextUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.openremote.container.web.WebService.*;

/**
 * A simple configuration container for configuring CORS behaviour
 */
public class CORSConfig {
    public static final int DEFAULT_CORS_MAX_AGE = 600;
    public static final String DEFAULT_CORS_ALLOW_ALL = "*";
    public static final boolean DEFAULT_CORS_ALLOW_CREDENTIALS = true;
    protected Set<String> corsAllowedOrigins;
    protected String corsAllowedMethods;
    protected String corsAllowedHeaders;
    protected String corsExposedHeaders;
    protected int corsMaxAge = DEFAULT_CORS_MAX_AGE;
    protected boolean corsAllowCredentials = DEFAULT_CORS_ALLOW_CREDENTIALS;

    public CORSConfig() {
        corsAllowedOrigins = Config.isDevMode() ? Collections.singleton(DEFAULT_CORS_ALLOW_ALL) : getCorsAllowedOriginsFromConfig();
        corsAllowedMethods = Config.isDevMode() ? DEFAULT_CORS_ALLOW_ALL : Config.getString(OR_WEBSERVER_ALLOWED_METHODS, DEFAULT_CORS_ALLOW_ALL);
        corsAllowedHeaders = Config.isDevMode() ? DEFAULT_CORS_ALLOW_ALL : Config.getString(OR_WEBSERVER_ALLOWED_HEADERS, DEFAULT_CORS_ALLOW_ALL);
        corsExposedHeaders = Config.getString(OR_WEBSERVER_EXPOSED_HEADERS, null);
    }

    public Set<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public CORSConfig setCorsAllowedOrigins(Set<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
        return this;
    }

    public CORSConfig addCorsAllowedOrigin(String origin) {
        corsAllowedOrigins.add(origin);
        return this;
    }

    public String getCorsAllowedMethods() {
        return corsAllowedMethods;
    }

    public CORSConfig setCorsAllowedMethods(String corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods;
        return this;
    }

   public String getCorsAllowedHeaders() {
      return corsAllowedHeaders;
   }

   public CORSConfig setCorsAllowedHeaders(String corsAllowedHeaders) {
      this.corsAllowedHeaders = corsAllowedHeaders;
      return this;
   }

   public String getCorsExposedHeaders() {
        return corsExposedHeaders;
    }

    public CORSConfig setCorsExposedHeaders(String corsExposedHeaders) {
        this.corsExposedHeaders = corsExposedHeaders;
        return this;
    }

    public int getCorsMaxAge() {
        return corsMaxAge;
    }

    public CORSConfig setCorsMaxAge(int corsMaxAge) {
        this.corsMaxAge = corsMaxAge;
        return this;
    }

    public boolean isCorsAllowCredentials() {
        return corsAllowCredentials;
    }

    public CORSConfig setCorsAllowCredentials(boolean corsAllowCredentials) {
        this.corsAllowCredentials = corsAllowCredentials;
        return this;
    }

    public static Set<String> getCorsAllowedOriginsFromConfig() {
        // Set allowed origins using external hostnames and WEBSERVER_ALLOWED_ORIGINS
        Set<String> allowedOrigins = new HashSet<>(
                getExternalHostnames()
                        .stream().map(hostname -> "https://" + hostname).toList()
        );
        String allowedOriginsStr = Config.getString(OR_WEBSERVER_ALLOWED_ORIGINS, null);

        if (!TextUtil.isNullOrEmpty(allowedOriginsStr)) {
            allowedOrigins.addAll(Arrays.stream(allowedOriginsStr.split(",")).toList());
        }

        return allowedOrigins;
    }
}
