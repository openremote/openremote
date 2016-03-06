package org.openremote.manager.client.interop.resteasy;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "", name = "REST")
public class REST {
    @JsProperty
    public static String apiURL;
}
