package org.openremote.container.security;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.inject.Inject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class KeyResolverService {

    // Inject the base URL for Keycloak from a configuration file (e.g., microprofile-config.properties)
    @Inject
    private String keycloakBaseUrl;

    private final ConcurrentMap<String, JWKSource<SecurityContext>> jwkSources = new ConcurrentHashMap<>();

    /**
     * Returns a JWKSource for the given Keycloak realm.
     * The source handles caching of the JSON Web Key Set (JWKS).
     *
     * @param realm The tenant realm.
     * @return A JWKSource capable of providing keys for the realm.
     */
    public JWKSource<SecurityContext> getJwkSource(String realm) {
        // computeIfAbsent ensures the source is created only once per realm in a thread-safe way.
        return jwkSources.computeIfAbsent(realm, r -> {
            try {
                // The standard URL for the JWKS endpoint in Keycloak
                URL jwksUrl = new URL(keycloakBaseUrl + "/realms/" + r + "/protocol/openid-connect/certs");
                return new RemoteJWKSet<>(jwksUrl);
            } catch (MalformedURLException e) {
                throw new IllegalStateException("Invalid Keycloak JWKS URL for realm: " + r, e);
            }
        });
    }
}
