package org.openremote.container.security;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.ws.rs.core.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Priority(1000) // ensure it runs early
public class JwtHttpAuthMechanism implements HttpAuthenticationMechanism {

    @Inject
    IdentityStoreHandler identityStoreHandler;

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response, HttpMessageContext context) {
        String authz = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authz == null || !authz.regionMatches(true, 0, "Bearer ", 0, 7)) {
            // No token: keep unauthenticated; constraints can still deny later
            return context.doNothing();
        }

        String token = authz.substring(7).trim();
        if (token.isEmpty()) {
            return context.responseUnauthorized(); // 401 with WWW-Authenticate
        }

        CredentialValidationResult result = identityStoreHandler.validate(new JwtCredential(token));
        if (result.getStatus() == CredentialValidationResult.Status.VALID) {
            return context.notifyContainerAboutLogin(result.getCallerPrincipal(), result.getGroups());
        } else {
            return context.responseUnauthorized();
        }
    }
}

