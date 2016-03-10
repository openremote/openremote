package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsType;
import org.openremote.manager.client.interop.Consumer;
import org.openremote.manager.client.interop.Runnable;

@JsType
public interface KeycloakCallback {
    KeycloakCallback success(Consumer<Boolean> successFn);

    KeycloakCallback error(Runnable errorFn);
}
