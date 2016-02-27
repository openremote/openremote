package org.openremote.manager.client.auth;

public class KeycloakUtil {

    public static native String getAccessToken() /*-{
        return $wnd.keycloak.token;
    }-*/;

}
