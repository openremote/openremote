package org.openremote.manager.client.auth;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestException;
import org.fusesource.restygwt.client.Dispatcher;
import org.fusesource.restygwt.client.Method;

public class BearerAuthorizationDispatcher implements Dispatcher {

    public static final BearerAuthorizationDispatcher INSTANCE = new BearerAuthorizationDispatcher();

    public Request send(Method method, RequestBuilder builder) throws RequestException {
        // TODO Token refresh, asynchronous, etc.
        String accessToken = KeycloakUtil.getAccessToken();
        if (accessToken != null) {
            builder.setHeader("Authorization", "Bearer " + accessToken);
        }
        return builder.send();
    }
}