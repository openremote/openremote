package org.openremote.components.client;

import elemental2.dom.HTMLElement;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/**
 * The singleton of {@code <or-app>}.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "OpenRemoteApp")
public abstract class OpenRemoteApp extends HTMLElement {

    public native AppSecurity getAppSecurity();

}
