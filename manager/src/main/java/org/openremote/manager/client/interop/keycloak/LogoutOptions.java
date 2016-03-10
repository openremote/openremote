package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType
public class LogoutOptions {
    @JsProperty
    public String redirectUri;
}
