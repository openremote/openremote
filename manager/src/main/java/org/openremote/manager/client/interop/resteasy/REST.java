package org.openremote.manager.client.interop.resteasy;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "REST")
public class REST {
    @JsProperty
    public static String apiURL;
}
