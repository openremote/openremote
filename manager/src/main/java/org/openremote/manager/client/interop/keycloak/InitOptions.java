package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType
public class InitOptions {
    @JsProperty
    public String onLoad;

    @JsProperty
    public String token;

    @JsProperty
    public String refreshToken;

    @JsProperty
    public String idToken;

    @JsProperty
    public int timeSkew;

    @JsProperty
    public boolean checkLoginIframe;

    @JsProperty
    public int checkLoginIframeInterval;

    @JsProperty
    public String responseMode;

    @JsProperty
    public String flow;
}
