package org.openremote.manager.client.service;

import com.google.inject.Inject;
import elemental.client.Browser;
import elemental.html.Location;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import org.openremote.manager.shared.http.Callback;
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

        public static void setDefaults(String realm) {
            Location location = Browser.getWindow().getLocation();
            REST.apiURL = "//" + location.getHostname() + ":" + location.getPort() + "/" + realm;
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
    public <T> RequestParams<T> createRequestParams(Callback<T> callback) {
        return new RequestParams<T>(callback)
            .withBearerAuth(securityService.getToken())
            .setXsrfToken(securityService.getXsrfToken());
    }

}
