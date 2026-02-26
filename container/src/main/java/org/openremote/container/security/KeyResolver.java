package org.openremote.container.security;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class KeyResolver {

    private String keycloakBaseUrl;
    private final ConcurrentMap<String, JWKSource<SecurityContext>> jwkSources = new ConcurrentHashMap<>();

    public KeyResolver(String keycloakBaseUrl) {
        this.keycloakBaseUrl = keycloakBaseUrl;
    }

    /**
     * Returns a JWKSource for the given Keycloak realm.
     * The source handles caching of the JSON Web Key Set (JWKS).
     */
    public JWKSource<SecurityContext> getJwkSource(String realm) {
        // computeIfAbsent ensures the source is created only once per realm in a thread-safe way.
        return jwkSources.computeIfAbsent(realm, r -> {
            try {
                // The standard URL for the JWKS endpoint in Keycloak
                URL jwksUrl = URI.create(keycloakBaseUrl + "/realms/" + r + "/protocol/openid-connect/certs").toURL();
                return JWKSourceBuilder.create(jwksUrl).build();
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Invalid Keycloak JWKS URL for realm: " + r, e);
            }
        });
    }

    public String getKeycloakBaseUrl() {
        return keycloakBaseUrl;
    }
}
