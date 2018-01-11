package org.openremote.app.client;

import elemental2.dom.HTMLElement;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import org.openremote.app.client.rest.RequestService;
import org.openremote.app.client.toast.Toasts;
import org.openremote.model.security.Tenant;

/**
 * The singleton of {@code <or-app>}.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "OpenRemoteApp")
public abstract class OpenRemoteApp extends HTMLElement {

    public native AppSecurity getSecurity();

    @JsProperty
    public native Tenant getTenant();

    @JsProperty
    public native Toasts getToasts();

    @JsProperty
    public native RequestService getRequestService();

    public native void set(String property, Any value);

}
