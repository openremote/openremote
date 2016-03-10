package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType
public class LoginOptions {
    @JsProperty
    public String redirectUri;

    @JsProperty
    public String prompt;

    @JsProperty
    public String loginHint;

    @JsProperty
    public String action;

    @JsProperty
    public String locale;
}
