package org.openremote.container.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.security.enterprise.AuthenticationException;
import org.openremote.model.Constants;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class TokenVerifierService implements TokenVerifier {
    private final KeyResolver keyResolverService;

    public TokenVerifierService(String keycloakUrl) {
        keyResolverService = new KeyResolver(keycloakUrl);
    }

    @Override
    public TokenPrincipal verify(String realm, String token) throws AuthenticationException {

        // 1. Get the JWK source for the realm from our service
        JWKSource<SecurityContext> keySource = keyResolverService.getJwkSource(realm);

        // 2. Create a JWT processor
        ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

        // 3. Configure the processor with a key selector for the appropriate JWS algorithm (e.g., RS256)
        // The key selector will use the JWK source to find the right public key by 'kid'
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
        jwtProcessor.setJWSKeySelector(keySelector);
        AtomicReference<TokenPrincipal> principalRef = new AtomicReference<>();

        // Uses DefaultJWTClaimsVerifier to verify exp, not before, issuer, audience but then also does super
        // user cross realm checks
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(new JWTClaimsSet.Builder()
                .issuer(keyResolverService.getKeycloakBaseUrl() + "/realms/" + realm)
                .audience(Constants.KEYCLOAK_CLIENT_ID)
                .build(), Collections.emptySet()) {

            @Override
            public void verify(JWTClaimsSet claimsSet, SecurityContext context) throws BadJWTException {
                super.verify(claimsSet, context);

                // Populate principal with claims now
                TokenPrincipal principal;
                try {
                    principal = new TokenPrincipal(claimsSet);
                    principalRef.set(principal);
                } catch (Exception e) {
                    throw new BadJWTException("Invalid JWT claims", e);
                }

                // Only allow super users to cross realms
                String authRealm = principal.getRealm();
                if (!Objects.equals(authRealm, realm)) {
                    if (!principal.isUserInRealmRole(Constants.SUPER_USER_REALM_ROLE)) {
                        throw new BadJWTException("Invalid token realm");
                    }
                }
            }
        });

        // Process the token. This verifies the signature and validates the claims
        try {
            jwtProcessor.process(token, null);
        } catch (Exception e) {
            throw new AuthenticationException("Invalid token: " + e.getMessage());
        }
        return principalRef.get();
    }
}
