package org.openremote.container.security;

import io.undertow.security.api.*;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public class JwtBearerAuthenticationMechanism implements AuthenticationMechanism {

    private final JwtValidator validator; // your Nimbus-based validator
    private final String realmHeader;
    private final String expectedAuthScheme;

    public JwtBearerAuthenticationMechanism(JwtValidator validator, String realmHeader) {
        this.validator = validator;
        this.realmHeader = realmHeader;
        this.expectedAuthScheme = "Bearer";
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext context) {
        var authz = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
        if (authz == null || !authz.regionMatches(true, 0, expectedAuthScheme, 0, expectedAuthScheme.length())) {
            return AuthenticationMechanismOutcome.NOT_ATTEMPTED;
        }
        var realm = exchange.getRequestHeaders().getFirst(realmHeader);
        if (realm == null || realm.isBlank()) {
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
        String token = authz.substring(expectedAuthScheme.length()).trim();
        try {
            var identity = validator.validate(realm, token); // subject + roles
            var account = new JwtAccount(identity.subject, identity.roles);
            context.authenticationComplete(account, "JWT", false);
            return AuthenticationMechanismOutcome.AUTHENTICATED;
        } catch (Exception e) {
            return AuthenticationMechanismOutcome.NOT_AUTHENTICATED;
        }
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext context) {
        // Advertise Bearer; you can also add error details if you want
        exchange.getResponseHeaders().put(Headers.WWW_AUTHENTICATE, "Bearer");
        return new ChallengeResult(true, StatusCodes.UNAUTHORIZED);
    }
}
