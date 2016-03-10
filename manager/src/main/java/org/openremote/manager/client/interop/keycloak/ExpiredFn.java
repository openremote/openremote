package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsFunction;

@FunctionalInterface
@JsFunction
public interface ExpiredFn {
    void expired();
}
