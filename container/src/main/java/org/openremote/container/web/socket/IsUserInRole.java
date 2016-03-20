package org.openremote.container.web.socket;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;

public class IsUserInRole implements Predicate {

    final protected String[] roles;

    public IsUserInRole(String... roles) {
        this.roles = roles;
    }

    @Override
    public boolean matches(Exchange exchange) {
        if (roles == null)
            return true;
        WebsocketAuth auth = exchange.getIn().getHeader(WebsocketConstants.AUTH, WebsocketAuth.class);
        for (String role : roles) {
            if (!auth.isUserInRole(role))
                return false;
        }
        return true;
    }
}
