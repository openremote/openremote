package org.openremote.container.security;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Provider
@Priority(Priorities.AUTHENTICATION)
@RequestScoped
public class JWTAuthenticationFilter implements ContainerRequestFilter {

    // Inject a service to resolve JWT validation keys based on realm
    // @Inject
    // private KeyResolverService keyResolverService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String realm = requestContext.getHeaderString("X-Realm");
        if (realm == null) {
            // Or handle as a default realm
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).entity("X-Realm header is missing").build());
            return;
        }

        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // TODO: Use keyResolverService to get the key for the realm and verify the signature
            // JWSVerifier verifier = new MACVerifier(keyResolverService.getKey(realm));
            // if (!signedJWT.verify(verifier)) {
            //     requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            //     return;
            // }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();

            // TODO: Validate claims (issuer, audience, expiration)

            Principal principal = () -> claimsSet.getSubject();
            List<String> rolesList = claimsSet.getStringListClaim("roles");
            Set<String> roles = new HashSet<>(rolesList);

            SecurityContext originalContext = requestContext.getSecurityContext();
            TokenSecurityContext securityContext = new TokenSecurityContext(principal, roles, originalContext.isSecure(), "JWT");
            requestContext.setSecurityContext(securityContext);

        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}
