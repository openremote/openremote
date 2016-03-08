package org.openremote.manager.client.security;

public class KeycloakUtil {

    public static native String getAccessToken() /*-{
        return $wnd.keycloak.token;
    }-*/;

}
