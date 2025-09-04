package org.openremote.container.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.DefaultJWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSetCache;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;

import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class JwtIdentityStore implements IdentityStore {

    // Configuration via env/system properties
    private final String issuer = get("JWT_ISSUER", "jwt.issuer");
    private final String audience = get("JWT_AUDIENCE", "jwt.audience");
    private final String jwksUrl = get("JWT_JWKS_URL", "jwt.jwks.url");
    private final int clockSkewSec = Integer.parseInt(getOrDefault("JWT_CLOCK_SKEW", "jwt.clock.skew", "30"));

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    public JwtIdentityStore() {
        try {
            // HTTP retriever with sane timeouts
            var retriever = new DefaultResourceRetriever(3000, 3000, 512 * 1024);
            // Cache JWKS for 5 minutes, refresh allowed every 1 minute
            JWKSetCache cache = new DefaultJWKSetCache(Duration.ofMinutes(5), Duration.ofMinutes(1));
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(jwksUrl), retriever, cache);

            // Accept RS256 (you can widen this set if needed)
            var keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);

            var proc = new DefaultJWTProcessor<SecurityContext>();
            proc.setJWSKeySelector(keySelector);

            // Claims verification: iss, aud, exp/nbf handled here
            var expectedAud = audience == null || audience.isBlank()
                    ? null
                    : Set.of(audience);

            var verifier = new DefaultJWTClaimsVerifier<>(
                    issuer == null || issuer.isBlank() ? null : issuer,
                    expectedAud,
                    // Required claims (always include exp)
                    new HashSet<>(List.of("exp")),
                    // Prohibited claims (none)
                    null
            );
            verifier.setMaxClockSkew(clockSkewSec);
            proc.setJWTClaimsSetVerifier(verifier);

            this.processor = proc;

        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize JWT processor", e);
        }
    }

    @Override
    public CredentialValidationResult validate(jakarta.security.enterprise.credential.Credential credential) {
        if (!(credential instanceof JwtCredential jwt)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
        try {
            JWTClaimsSet claims = processor.process(jwt.token(), null);

            String subject = Optional.ofNullable(claims.getSubject()).orElse("anonymous");
            Set<String> groups = extractRoles(claims);

            return new CredentialValidationResult(subject, groups);

        } catch (Exception e) {
            return CredentialValidationResult.INVALID_RESULT;
        }
    }

    // Role extraction for common Keycloak tokens
    @SuppressWarnings("unchecked")
    private static Set<String> extractRoles(JWTClaimsSet c) {
        Set<String> roles = new HashSet<>();

        Object realmAccess = c.getClaim("realm_access");
        if (realmAccess instanceof Map<?, ?> m) {
            Object rr = m.get("roles");
            if (rr instanceof Collection<?> col) col.forEach(r -> roles.add(String.valueOf(r)));
        }
        Object resourceAccess = c.getClaim("resource_access");
        if (resourceAccess instanceof Map<?, ?> ra) {
            for (Object v : ra.values()) {
                if (v instanceof Map<?, ?> vm) {
                    Object rs = vm.get("roles");
                    if (rs instanceof Collection<?> col) col.forEach(r -> roles.add(String.valueOf(r)));
                }
            }
        }
        Object rolesClaim = c.getClaim("roles");
        if (rolesClaim instanceof Collection<?> col) col.forEach(r -> roles.add(String.valueOf(r)));

        Object scopeObj = c.getClaim("scope");
        if (scopeObj instanceof String scope && !scope.isBlank()) {
            roles.addAll(Arrays.stream(scope.split("\\s+")).filter(s -> !s.isBlank()).collect(Collectors.toSet()));
        }
        return roles;
    }

    private static String get(String envKey, String sysKey) {
        String v = System.getenv(envKey);
        if (v == null || v.isBlank()) v = System.getProperty(sysKey);
        if (v == null) v = "";
        return v;
    }

    private static String getOrDefault(String envKey, String sysKey, String def) {
        String v = get(envKey, sysKey);
        return v.isBlank() ? def : v;
    }
}
