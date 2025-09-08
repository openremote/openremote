package org.openremote.container.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.text.ParseException;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JwtValidator {
    private final RealmConfigService realms;
    private final JwkSourceRegistry jwkSources;
    private final JWSAlgorithm expectedAlg;

    public JwtValidator(RealmConfigService realms, JwkSourceRegistry jwkSources, JWSAlgorithm expectedAlg) {
        this.realms = realms;
        this.jwkSources = jwkSources;
        this.expectedAlg = expectedAlg;
    }

    public ValidatedIdentity validate(String realm, String token) throws Exception {
        RealmConfig cfg = realms.findByRealm(realm);

        SignedJWT jwt = SignedJWT.parse(token);

        ConfigurableJWTProcessor<SecurityContext> proc = new DefaultJWTProcessor<>();
        proc.setJWSKeySelector(new JWSVerificationKeySelector<>(expectedAlg, jwkSources.getOrCreate(cfg)));

        var claims = proc.process(jwt, null);

        // Validate iss
        if (!cfg.issuer.equals(claims.getIssuer())) {
            throw new SecurityException("Invalid issuer");
        }
        // Validate aud
        if (cfg.acceptedAud != null && !cfg.acceptedAud.isEmpty()) {
            List<String> aud = claims.getAudience();
            if (aud == null || aud.stream().noneMatch(cfg.acceptedAud::contains)) {
                throw new SecurityException("Invalid audience");
            }
        }
        // exp/nbf are validated by Nimbus if you set a clock skew; else check here:
        Instant now = Instant.now();
        if (claims.getExpirationTime() == null || claims.getExpirationTime().toInstant().isBefore(now)) {
            throw new SecurityException("Token expired");
        }
        if (claims.getNotBeforeTime() != null && claims.getNotBeforeTime().toInstant().isAfter(now)) {
            throw new SecurityException("Token not valid yet");
        }

        String subject = claims.getSubject();
        Set<String> roles = extractRoles(claims.getClaim(cfg.rolesClaim));
        return new ValidatedIdentity(subject, roles, realm, claims.getClaims());
    }

    @SuppressWarnings("unchecked")
    private Set<String> extractRoles(Object claim) throws ParseException {
        Set<String> roles = new HashSet<>();
        if (claim instanceof Collection<?> c) {
            for (Object o : c) roles.add(String.valueOf(o));
        } else if (claim instanceof String s) {
            for (String part : s.split(" ")) roles.add(part.trim());
        } else if (claim instanceof java.util.Map<?, ?> m) {
            Object nested = m.get("roles");
            if (nested instanceof Collection<?> c2) for (Object o : c2) roles.add(String.valueOf(o));
        }
        return roles;
    }

    public static final class ValidatedIdentity {
        public final String subject;
        public final Set<String> roles;
        public final String realm;
        public final java.util.Map<String,Object> claims;

        public ValidatedIdentity(String subject, Set<String> roles, String realm, java.util.Map<String,Object> claims) {
            this.subject = subject;
            this.roles = roles;
            this.realm = realm;
            this.claims = claims;
        }
    }
}

