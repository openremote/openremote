package org.openremote.app.client;

import elemental2.dom.HTMLElement;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import org.openremote.app.client.rest.Requests;
import org.openremote.app.client.toast.Toasts;
import java.util.function.Consumer;
import org.openremote.model.security.Tenant;
import org.openremote.model.interop.Runnable;

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
    public native Requests getRequests();

    public native void addServiceMessageConsumer(Consumer<String> message);

    public native void addServiceConnectionCloseListener(Runnable listener);

    public native void sendServiceMessage(String message);

    public native void set(String property, Any value);
}
