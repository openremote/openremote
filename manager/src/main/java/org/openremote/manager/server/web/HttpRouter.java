package org.openremote.manager.server.web;

import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.RouterImpl;

public class HttpRouter extends RouterImpl {

    public static final String CONTEXT_PARAM_REALM = HttpRouter.class.getName() + ".CONTEXT_PARAM_REALM";

    public HttpRouter(Vertx vertx) {
        super(vertx);
    }

    protected String getRealm(RoutingContext context) {
        String realm = context.get(CONTEXT_PARAM_REALM);
        if (realm == null || realm.length() == 0)
            throw new IllegalStateException("Empty realm in routing context");
        return realm;
    }
}
