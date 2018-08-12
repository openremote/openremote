package org.openremote.app.client;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.openremote.model.http.RequestParams;
import java.util.function.Consumer;
import org.openremote.model.interop.Runnable;

/**
 * The singleton of {@code <or-app-security>}.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "AppSecurity")
public interface AppSecurity {

    @JsProperty
    boolean isAuthenticated();

    @JsProperty
    String getUser();

    String getFullName();

    @JsProperty
    String getRealm();

    void logout();

    boolean isSuperUser();

    boolean hasRealmRole(String role);

    boolean hasResourceRole(String role, String resource);

    boolean hasResourceRoleOrIsSuperUser(String role, String resource);

    boolean isUserTenantAdminEnabled();

    /**
     * Sets credentials on request params, this can happen asynchronously.
     */
    void authorizeRequestParams(RequestParams requestParams, Runnable onComplete);

    /**
     * Sets credentials on URL, this can happen asynchronously.
     */
    void authorizeUrl(String serviceUrl, Consumer<String> onComplete);

}
