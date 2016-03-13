package org.openremote.manager.client.interop.keycloak;

import jsinterop.annotations.JsType;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.Runnable;

// We have GWT compiler erasure problems if this is an interface, so let's use an abstract class
@JsType
public abstract class KeycloakCallback {

    abstract public KeycloakCallback success(Consumer<Boolean> successFn);

    abstract public KeycloakCallback error(Runnable errorFn);
}
