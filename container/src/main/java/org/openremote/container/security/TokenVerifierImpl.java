/*
 * Copyright 2026, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.container.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier;
import com.nimbusds.jwt.proc.JWTProcessor;
import jakarta.security.enterprise.AuthenticationException;
import org.openremote.model.Constants;
import org.openremote.model.security.User;
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

        // 2. Configure the processor with a key selector for the appropriate JWS algorithm (e.g., RS256)
        // The key selector will use the JWK source to find the right public key by 'kid'
        JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
        processor.setJWSKeySelector(keySelector);

        final String expectedIssuer = keycloakPublicUrl + "/realms/" + realm;
        final String expectedClientId = Constants.KEYCLOAK_CLIENT_ID;
        final JWTClaimsSetVerifier<SecurityContext> defaultClaimsVerifier = new DefaultJWTClaimsVerifier<>(null, null);

        JWTClaimsSetVerifier<SecurityContext> verifier = (claims, context) -> {
            // Preserve Nimbus built-in checks for exp / nbf validation.
            defaultClaimsVerifier.verify(claims, context);

            // Issuer check
            if (claims.getIssuer() == null || !expectedIssuer.equals(claims.getIssuer())) {
                throw new BadJWTException("Invalid token issuer");
            }

            // Keycloak uses azp to store client ID by default but we support aud for non keycloak auth servers
            String azp = null;
            try {
                azp = claims.getStringClaim("azp");
            } catch (ParseException ignored) {}

            if (azp != null && !azp.isBlank()) {
                if (expectedClientId.equals(azp)) {
                    return;
                }
            }

            // Fall back to aud - service users put the client in the aud claim not the azp claim
            if (claims.getAudience() == null || !claims.getAudience().contains(expectedClientId)) {
               // TODO: Remove this once https://github.com/openremote/openremote/issues/2642 is implemented
               try {
                  String preferredUsername = claims.getStringClaim("preferred_username");
                  if (preferredUsername == null || !preferredUsername.startsWith(User.SERVICE_ACCOUNT_PREFIX)) {
                     throw new BadJWTException("Invalid token audience");
                  }
               } catch (ParseException ignored) {}
            }
        };

        processor.setJWTClaimsSetVerifier(verifier);
        return processor;
    }

    @Override
    public TokenPrincipal verify(String realm, String token) throws AuthenticationException {

        if (realm == null || realm.isBlank()) {
            throw new AuthenticationException("Invalid realm");
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
                if (!principal.hasRealmRole(Constants.SUPER_USER_REALM_ROLE)) {
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
