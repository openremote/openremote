package org.openremote.container.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;
import com.nimbusds.jwt.proc.JWTProcessor;
import jakarta.security.enterprise.AuthenticationException;
import org.openremote.model.Constants;
import org.openremote.model.syslog.SyslogCategory;

import java.text.ParseException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class TokenVerifierImpl implements TokenVerifier {
    protected static final Logger LOG = SyslogCategory.getLogger(SyslogCategory.API, TokenVerifierImpl.class.getName());
    protected final KeyResolver keyResolverService;
    // The public URL of the keycloak server (must match the issuer in the generated tokens)
    protected final String keycloakPublicUrl;
    protected final Map<String, JWTProcessor<SecurityContext>> processorCache = new ConcurrentHashMap<>();

    public TokenVerifierImpl(String keycloakUrl, String keycloakPublicUrl) {
        keyResolverService = new KeyResolver(keycloakUrl);
        this.keycloakPublicUrl = keycloakPublicUrl;
    }

    protected JWTProcessor<SecurityContext> getJwtProcessor(String realm) {
        return processorCache.computeIfAbsent(realm, this::initialiseProcessor);
    }

    protected JWTProcessor<SecurityContext> initialiseProcessor(String realm) {
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();

        // 1. Get the JWK source for the realm from our service
        JWKSource<SecurityContext> keySource = keyResolverService.getJwkSource(realm);

        // 3. Configure the processor with a key selector for the appropriate JWS algorithm (e.g., RS256)
        // The key selector will use the JWK source to find the right public key by 'kid'
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
        processor.setJWSKeySelector(keySelector);

        final String expectedIssuer = keycloakPublicUrl + "/realms/" + realm;
        final String expectedClientId = Constants.KEYCLOAK_CLIENT_ID;

        JWTClaimsSetVerifier<SecurityContext> verifier = (claims, context) -> {
            // 1) issuer check (keep/remove depending on your policy)
            if (claims.getIssuer() == null || !expectedIssuer.equals(claims.getIssuer())) {
                throw new BadJWTException("Invalid token issuer");
            }

            // Keycloak uses azp to store client ID by default but we support aud for non keycloak auth servers
            String azp = null;
            try {
                azp = claims.getStringClaim("azp");
            } catch (ParseException ignored) {}

            if (azp != null && !azp.isBlank()) {
                if (!expectedClientId.equals(azp)) {
                    throw new BadJWTException("Invalid token azp (authorized party)");
                }
                return;
            }

            // Fall back to aud
            if (claims.getAudience() == null || !claims.getAudience().contains(expectedClientId)) {
                throw new BadJWTException("Invalid token audience");
            }
        };

        processor.setJWTClaimsSetVerifier(verifier);
        return processor;
    }

    @Override
    public TokenPrincipal verify(String realm, String token) throws AuthenticationException {

        if (realm == null || realm.isBlank()) {
            throw new AuthenticationException("Invalid token");
        }
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Invalid token");
        }

        JWTProcessor<SecurityContext> jwtProcessor = getJwtProcessor(realm);
        JWTClaimsSet claimsSet;
        TokenPrincipal principal;

        // Process the token. This verifies the signature and validates the claims
        // We also perform super user cross realm checks
        try {
            claimsSet = jwtProcessor.process(token, null);
            principal = new TokenPrincipal(claimsSet);

            // Only allow super users to cross realms
            String authRealm = principal.getRealm();
            if (!Objects.equals(authRealm, realm)) {
                if (!principal.isUserInRealmRole(Constants.SUPER_USER_REALM_ROLE)) {
                    throw new AuthenticationException("Invalid token realm");
                }
            }
        } catch (Exception e) {
            String msg = "Invalid token: " + e.getMessage();
            LOG.info(msg);
            throw new AuthenticationException(msg);
        }

        return principal;
    }
}
