package org.openremote.container.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION)
@RequestScoped
public class JWTAuthenticationFilter implements ContainerRequestFilter {

    @Inject
    private KeyResolverService keyResolverService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String realm = requestContext.getHeaderString("X-Realm");
        if (realm == null) {
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity("X-Realm header is missing").build());
            return;
        }

        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Let the request pass. If a resource requires authentication and there's no
            // security context, JAX-RS will correctly return a 401 Unauthorized.
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        try {
            // 1. Get the JWK source for the realm from our service
            JWKSource<SecurityContext> keySource = keyResolverService.getJwkSource(realm);

            // 2. Create a JWT processor
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();

            // 3. Configure the processor with a key selector for the appropriate JWS algorithm (e.g., RS256)
            // The key selector will use the JWK source to find the right public key by 'kid'
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
            jwtProcessor.setJWSKeySelector(keySelector);

            // 4. **CRITICAL**: Set expected claims. This is essential for security.
            // The issuer for a Keycloak realm is typically 'https://<host>/realms/<realm>'
            // You should also validate the audience ('aud' claim).
            // jwtProcessor.setJWTClaimsSetVerifier((claims, context) -> {
            //     final String expectedIssuer = keycloakBaseUrl + "/realms/" + realm;
            //     if (!expectedIssuer.equals(claims.getIssuer())) {
            //          throw new BadJWTException("Invalid token issuer");
            //     }
            //     // Add audience validation, expiration is checked by default
            // });

            // 5. Process the token. This verifies the signature and validates the claims.
            JWTClaimsSet claimsSet = jwtProcessor.process(token, null);

            Principal principal = () -> claimsSet.getSubject();
            List<String> rolesList = claimsSet.getStringListClaim("roles");
            Set<String> roles = (rolesList != null) ? new HashSet<>(rolesList) : Collections.emptySet();

            SecurityContext originalContext = requestContext.getSecurityContext();
            TokenSecurityContext securityContext = new TokenSecurityContext(principal, roles, originalContext.isSecure(), "JWT");
            requestContext.setSecurityContext(securityContext);

        } catch (Exception e) {
            // In a real app, log the exception for debugging.
            // e.g., LOG.warn("JWT validation failed", e);
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid or expired token").build());
        }
    }
}
