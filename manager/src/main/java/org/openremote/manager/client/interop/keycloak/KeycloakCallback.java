package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Runnable;

@JsType
public interface KeycloakCallback {
    KeycloakCallback success(Consumer<Boolean> successFn);

    KeycloakCallback error(Runnable errorFn);
}
