package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType
public class KeycloakConfig {
    @JsProperty
    public String url;

    @JsProperty
    public String realm;

    @JsProperty
    public String clientId;
}
