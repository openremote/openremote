package org.openremote.components.client;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.openremote.model.http.RequestParams;

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

    @JsProperty(name = "tenant")
    String getAuthenticatedRealm();

    void logout();

    boolean isSuperUser();

    boolean hasRealmRole(String role);

    boolean hasResourceRole(String role, String resource);

    boolean hasResourceRoleOrIsSuperUser(String role, String resource);

    boolean isUserTenantAdminEnabled();

    void setCredentialsOnRequestParams(RequestParams requestParams);

    String setCredentialsOnUrl(String serviceUrl);

}
