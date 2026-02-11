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
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.openremote.model.Constants;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class JWTAuthenticationFilter implements Filter {

    public static final String NAME = "JWTAuthFilter";
    public static final String AUTH_TYPE = "JWT";
    private final KeyResolverService keyResolverService;

    public JWTAuthenticationFilter(KeyResolverService keyResolverService) {
        this.keyResolverService = keyResolverService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String realm = httpRequest.getHeader(Constants.REALM_PARAM_NAME);
        if (realm == null) {
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, Constants.REALM_PARAM_NAME + " header is missing");
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Anonymous - If the resource is protected, the container or application logic will handle the 401/403.
            chain.doFilter(request, response);
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

            AtomicReference<TokenPrincipal> principalRef = new AtomicReference<>();

            // 4. **CRITICAL**: Set expected claims. This is essential for security.
            // The issuer for a Keycloak realm is typically 'https://<host>/realms/<realm>'
            // You should also validate the audience ('aud' claim).
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

            // Process the token. This verifies the signature and validates the claims.
            jwtProcessor.process(token, null);

            // Wrap the request to provide security context
            HttpServletRequestWrapper authenticatedRequest = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public Principal getUserPrincipal() {
                    return principalRef.get();
                }

                @Override
                public boolean isUserInRole(String role) {
                    return principalRef.get().isUserInRole(role);
                }

                @Override
                public String getAuthType() {
                    return AUTH_TYPE;
                }
            };

            chain.doFilter(authenticatedRequest, response);

        } catch (Exception e) {
            httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }
}
