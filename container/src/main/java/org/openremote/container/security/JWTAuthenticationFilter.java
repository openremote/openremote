package org.openremote.container.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.openremote.model.Constants;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

            TokenPrincipal principal = new TokenPrincipal(claimsSet);

            // Wrap the request to provide security context
            HttpServletRequestWrapper authenticatedRequest = new HttpServletRequestWrapper(httpRequest) {
                @Override
                public Principal getUserPrincipal() {
                    return principal;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return principal.isUserInRole(role);
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
