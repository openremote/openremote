package org.openremote.manager.client.service;

import com.google.gwt.user.client.Window;
import com.google.inject.Inject;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.http.Request;
import org.openremote.manager.shared.http.RequestParams;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

public class RequestServiceImpl implements RequestService {

    public static class Configuration {

        @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "REST")
        public static class REST {
            @JsProperty
            public static String apiURL;
        }

        public static void setDefaults(SecurityService securityService) {
            REST.apiURL = "//" + Window.Location.getHostName() + ":" + Window.Location.getPort() + "/" + securityService.getRealm();
        }
    }

    final protected SecurityService securityService;

    @Inject
    public RequestServiceImpl(SecurityService securityService) {
        this.securityService = securityService;
    }

    @Override
    public <T> Request<T> createRequest(boolean withBearerAuthorization) {
        Request<T> request = new Request<>();
        if (withBearerAuthorization) {
            request.addHeader(AUTHORIZATION, "Bearer " + securityService.getToken());
            // TODO xsrf?
        }
        return request;
    }

    @Override
    public <T> RequestParams<T> createRequestParams(int expectedStatusCode, Consumer<T> onSuccess, Consumer<Exception> onFailure) {
        return new RequestParams<T>(expectedStatusCode, onSuccess, onFailure)
            .withBearerAuth(securityService.getToken())
            .setXsrfToken(securityService.getXsrfToken());
    }

}
