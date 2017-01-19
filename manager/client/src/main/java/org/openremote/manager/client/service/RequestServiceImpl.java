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
import org.openremote.model.Consumer;
import org.openremote.manager.shared.http.*;
import org.openremote.manager.shared.validation.ConstraintViolationReport;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.openremote.manager.shared.validation.ConstraintViolationReport.VIOLATION_EXCEPTION_HEADER;

public class RequestServiceImpl implements RequestService {

    private static final Logger LOG = Logger.getLogger(RequestServiceImpl.class.getName());

    public static class Configuration {

        @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "REST")
        public static class REST {
            @JsProperty
            public static String apiURL;

            @JsProperty
            public static boolean debug;

            @JsProperty
            public static boolean antiBrowserCache;

            @JsProperty
            public static int loglevel;

            // Enable debug to fill this value
            @JsProperty
            public static Request.XMLHttpRequest lastRequest;
        }

        public static void setDefaults(String realm) {
            Location location = Browser.getWindow().getLocation();
            REST.apiURL = "//" + location.getHostname() + ":" + location.getPort() + "/" + realm;
            REST.loglevel = LOG.isLoggable(Level.FINE) ? 1 : 0;
        }
    }

    final protected SecurityService securityService;
    final protected EntityReader<ConstraintViolationReport> constraintViolationReader;

    @Inject
    public RequestServiceImpl(SecurityService securityService,
                              EntityReader<ConstraintViolationReport> constraintViolationReader) {
        this.securityService = securityService;
        this.constraintViolationReader = constraintViolationReader;
    }

    @Override
    public void execute(Consumer<RequestParams<Void>> onRequest,
                        int expectedStatusCode,
                        Runnable onResponse,
                        Consumer<RequestException> onException) {
        execute(null, onRequest, expectedStatusCode, onResponse, onException);

    }

    @Override
    public <OUT> void execute(EntityReader<OUT> entityReader,
                              Consumer<RequestParams<OUT>> onRequest,
                              int expectedStatusCode,
                              Consumer<OUT> onResponse,
                              Consumer<RequestException> onException) {
        execute(entityReader, null, onRequest, expectedStatusCode, onResponse, onException);
    }

    @Override
    public <IN> void execute(EntityWriter<IN> entityWriter,
                             Consumer<RequestParams<Void>> onRequest,
                             int expectedStatusCode,
                             Runnable onResponse,
                             Consumer<RequestException> onException) {
        this.execute(null, entityWriter, onRequest, expectedStatusCode, out -> onResponse.run(), onException);
    }

    @Override
    public <IN, OUT> void execute(EntityReader<OUT> entityReader,
                                  EntityWriter<IN> entityWriter,
                                  Consumer<RequestParams<OUT>> onRequest,
                                  int expectedStatusCode,
                                  Consumer<OUT> onResponse,
                                  Consumer<RequestException> onException) {

            RequestParams<OUT> requestParams = new RequestParams<>(
                (responseCode, request, responseText) -> {

                    if (responseCode == 0) {
                        onException.accept(new NoResponseException());
                        return;
                    }

                    if (responseCode == 400) {
                        String validationException = request.getResponseHeader(VIOLATION_EXCEPTION_HEADER);
                        if (validationException != null && validationException.equals("true")) {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("Received 400 status with constraint violation report: " + responseText);
                            }
                            ConstraintViolationReport report = getConstraintViolationReport(responseText);
                            onException.accept(new BadRequestException(400, report));
                        } else {
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.fine("Received 400 status without constraint violation report");
                            }
                            onException.accept(new BadRequestException(400));
                        }
                        return;
                    }

                    if (responseCode == 401) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Received 401, logging out...");
                        }
                        securityService.logout();
                        return;
                    }

                    if (responseCode == 409) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Received 409 conflict");
                        }
                        onException.accept(new ConflictRequestException());
                        return;
                    }

                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Received response status: " + responseCode);
                    }

                    if (expectedStatusCode != ANY_STATUS_CODE && responseCode != expectedStatusCode) {
                        onException.accept(new UnexpectedStatusRequestException(responseCode, expectedStatusCode));
                        return;
                    }

                    OUT out = null;
                    if (responseText != null && responseText.length() > 0 && entityReader != null) {
                        if (LOG.isLoggable(Level.FINE))
                            LOG.fine("Reading response text, length: " + responseText.length());
                        try {
                            out = entityReader.read(responseText);
                        } catch (Exception ex) {
                            if (LOG.isLoggable(Level.FINE))
                                LOG.log(Level.FINE, "Response marshalling error", ex);
                            onException.accept(new EntityMarshallingRequestException(ex));
                            return;
                        }
                    } else {
                        LOG.fine("No response text or response entity reader");
                    }
                    onResponse.accept(out);
                }
            );

            requestParams.withBearerAuth(securityService.getToken());

            if (entityWriter != null) {
                requestParams.setEntityWriter(entityWriter);
            }

            onRequest.accept(requestParams);
    }

    protected ConstraintViolationReport getConstraintViolationReport(String responseText) {
        return constraintViolationReader.read(responseText);
    }
}
