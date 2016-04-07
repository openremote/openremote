/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
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
            .withBearerAuth(securityService.getToken());
    }

}
