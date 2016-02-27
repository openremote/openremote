package org.openremote.manager.server.web;

import com.google.common.base.Preconditions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouterImpl;
import org.keycloak.RSATokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.openremote.manager.server.identity.ClientInstall;
import org.openremote.manager.server.identity.IdentityService;

import java.util.logging.Level;
import java.util.logging.Logger;

import static io.vertx.core.http.HttpHeaders.AUTHORIZATION;
import static org.openremote.manager.server.Constants.MANAGER_CLIENT_ID;

public class HttpRouter extends RouterImpl {

    private static final Logger LOG = Logger.getLogger(HttpRouter.class.getName());

    public static final String CONTEXT_PARAM_REALM = HttpRouter.class.getName() + ".CONTEXT_PARAM_REALM";
    public static final String CONTEXT_PARAM_ACCESS_TOKEN = HttpRouter.class.getName() + ".ACCESS_TOKEN";
    public static final String CONTEXT_PARAM_ACCESS_TOKEN_RAW = HttpRouter.class.getName() + ".ACCESS_TOKEN_RAW";

    final protected IdentityService identityService;

    public HttpRouter(Vertx vertx, IdentityService identityService) {
        super(vertx);
        this.identityService = identityService;

        route().handler(createAuthenticationHandler(vertx));
    }

    public IdentityService getIdentityService() {
        return identityService;
    }

    protected String getRealm(RoutingContext rc) {
        String realm = rc.get(CONTEXT_PARAM_REALM);
        if (realm == null || realm.length() == 0)
            throw new ResponseException(400, "Missing realm");
        return realm;
    }

    protected AccessToken getAccessToken(RoutingContext rc) {
        AccessToken accessToken = rc.get(CONTEXT_PARAM_ACCESS_TOKEN);
        if (accessToken == null)
            throw new ResponseException(403, "Valid bearer access token required");
        return accessToken;
    }

    protected String getAccessTokenRaw(RoutingContext rc) {
        String accessTokenRaw = rc.get(CONTEXT_PARAM_ACCESS_TOKEN_RAW);
        if (accessTokenRaw == null)
            throw new ResponseException(403, "Valid bearer access token required");
        return accessTokenRaw;
    }

    protected void checkManagerAccess(RoutingContext rc, String role) {
        checkAccess(rc, MANAGER_CLIENT_ID, role);
    }

    protected void checkAccess(RoutingContext rc, String resource, String role) {
        Preconditions.checkNotNull(resource, "Resource required");
        Preconditions.checkNotNull(resource, "Role required");

        if (identityService.isDisableAPISecurity()) {
            LOG.warning("API security disabled! Granting role '" + role + "' access!");
            return;
        }

        LOG.fine("Checking user has role: " + role);
        AccessToken accessToken = getAccessToken(rc);

        if (accessToken.getRealmAccess() != null && accessToken.getRealmAccess().isUserInRole("admin")) {
            LOG.fine("User '" + accessToken.getPreferredUsername() + "' is realm admin, granting full access");
            return;
        }

        AccessToken.Access resourceAccess = accessToken.getResourceAccess(resource);
        if (resourceAccess != null && resourceAccess.isUserInRole(role)) {
            LOG.fine("User '" + accessToken.getPreferredUsername() + "' has required role, granting access: " + role);
            return;
        }

        throw new ResponseException(403, "Additional permissions required");
    }

    protected Handler<RoutingContext> createAuthenticationHandler(Vertx vertx) {
        return rc -> {
            HttpServerRequest request = rc.request();

            String authorizationHeader = request.getHeader(AUTHORIZATION);
            if (authorizationHeader == null || authorizationHeader.length() == 0) {
                // No authentication credentials present, silently ignore
                rc.next();
                return;
            }

            String tokenString = null;
            String[] split = authorizationHeader.trim().split("\\s+");
            if (split.length != 2 || !split[0].equalsIgnoreCase("Bearer")) {
                LOG.fine("Invalid bearer token authorization header: " + request.method() + " " + request.absoluteURI());
                rc.next();
                return;
            }
            tokenString = split[1];

            String realm = getRealm(rc);
            String clientId = MANAGER_CLIENT_ID;
            ClientInstall clientInstall = getIdentityService().getClientInstall(realm, clientId);
            if (clientInstall == null) {
                LOG.fine("No client install for bearer token validation: " + request.method() + " " + request.absoluteURI());
                rc.next();
                return;
            }

            AccessToken token;
            try {
                token = RSATokenVerifier.verifyToken(
                    tokenString,
                    clientInstall.getPublicKey(),
                    clientInstall.getRealmInfoUrl()
                );
            } catch (VerificationException ex) {
                LOG.log(Level.INFO, "Bearer token verification failed: " + request.method() + " " + request.absoluteURI(), ex);
                rc.next();
                return;
            }

            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Request " + request.method() + " " + request.absoluteURI() + " with valid access token:\n" + Json.encodePrettily(token));
            }
            rc.put(CONTEXT_PARAM_ACCESS_TOKEN, token);
            rc.next();
        };
    }
}
