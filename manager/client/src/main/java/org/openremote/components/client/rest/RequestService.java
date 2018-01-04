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
package org.openremote.components.client.rest;

import com.google.gwt.core.client.GWT;
import elemental.client.Browser;
import elemental.html.Location;
import elemental2.core.Global;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import org.openremote.components.client.AppSecurity;
import org.openremote.model.http.*;
import org.openremote.model.interop.Consumer;
import org.openremote.model.interop.Function;
import org.openremote.model.interop.Runnable;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static com.google.gwt.http.client.Response.*;
import static org.openremote.model.http.ConstraintViolationReport.VIOLATION_EXCEPTION_HEADER;

@JsType
public class RequestService {

    private static final Logger LOG = Logger.getLogger(RequestService.class.getName());

    double ANY_STATUS_CODE = -1;

    final protected Consumer<RequestParams> authenticator;
    final protected Function<String, ConstraintViolationReport> constraintViolationReader;

    public RequestService(Consumer<RequestParams> authenticator,
                          Function<String, ConstraintViolationReport> constraintViolationReader) {
        this.authenticator = authenticator;
        this.constraintViolationReader = constraintViolationReader;
    }

    @JsIgnore
    public RequestService(AppSecurity appSecurity,
                          EntityReader<ConstraintViolationReport> constraintViolationReader) {
        this(appSecurity::setCredentialsOnRequestParams, constraintViolationReader::read);
        if (GWT.isClient()) {
            configure(appSecurity.getAuthenticatedRealm());
        }
    }

    public void configure(String realm) {
        Location location = Browser.getWindow().getLocation();
        REST.apiURL = "//" + location.getHostname() + ":" + location.getPort() + "/" + realm;
        REST.loglevel = LOG.isLoggable(Level.FINE) ? 1 : 0;
    }

    @JsIgnore
    public void send(Consumer<RequestParams<Void, Void>> onRequest,
                     int expectedStatusCode,
                     Runnable onResponse,
                     Consumer<RequestException> onException) {
        execute(null, null, onRequest, new double[]{expectedStatusCode}, out -> onResponse.run(), onException);
    }

    public void send(Consumer<RequestParams<Void, Void>> onRequest,
                     double expectedStatusCode,
                     Runnable onResponse,
                     Consumer<RequestException> onException) {
        if (!GWT.isClient())
            throw new UnsupportedOperationException("This method can only be called in a JS runtime environment");
        execute(null, null, onRequest, new double[]{expectedStatusCode}, out -> onResponse.run(), onException);
    }

    @JsIgnore
    public <OUT> void sendAndReturn(EntityReader<OUT> entityReader,
                                    Consumer<RequestParams<Void, OUT>> onRequest,
                                    int expectedStatusCode,
                                    Consumer<OUT> onResponse,
                                    Consumer<RequestException> onException) {
        execute(entityReader::read, null, onRequest, new double[]{expectedStatusCode}, onResponse, onException);
    }

    public void sendAndReturn(Consumer<RequestParams<Void, Any>> onRequest,
                              double expectedStatusCode,
                              Consumer<Any> onResponse,
                              Consumer<RequestException> onException) {
        if (!GWT.isClient())
            throw new UnsupportedOperationException("This method can only be called in a JS runtime environment");
        execute(object -> (Any) Global.JSON.parse(object), null, onRequest, new double[]{expectedStatusCode}, onResponse, onException);
    }

    @JsIgnore
    public <IN> void sendWith(EntityWriter<IN> entityWriter,
                              Consumer<RequestParams<IN, Void>> onRequest,
                              int expectedStatusCode,
                              Runnable onResponse,
                              Consumer<RequestException> onException) {
        execute(null, entityWriter::write, onRequest, new double[]{expectedStatusCode}, out -> onResponse.run(), onException);
    }


    public <IN> void sendWith(Function<IN, String> entityWriter,
                              Consumer<RequestParams<IN, Void>> onRequest,
                              double expectedStatusCode,
                              Runnable onResponse,
                              Consumer<RequestException> onException) {
        if (!GWT.isClient())
            throw new UnsupportedOperationException("This method can only be called in a JS runtime environment");
        execute(null, entityWriter, onRequest, new double[]{expectedStatusCode}, out -> onResponse.run(), onException);
    }

    @JsIgnore
    public <IN, OUT> void sendWithAndReturn(EntityReader<OUT> entityReader,
                                            EntityWriter<IN> entityWriter,
                                            Consumer<RequestParams<IN, OUT>> onRequest,
                                            int expectedStatusCode,
                                            Consumer<OUT> onResponse,
                                            Consumer<RequestException> onException) {
        execute(entityReader::read, entityWriter::write, onRequest, new double[]{expectedStatusCode}, onResponse, onException);
    }

    public <IN, OUT> void sendWithAndReturn(Function<String, OUT> entityReader,
                                            Function<IN, String> entityWriter,
                                            Consumer<RequestParams<IN, OUT>> onRequest,
                                            double expectedStatusCode,
                                            Consumer<OUT> onResponse,
                                            Consumer<RequestException> onException) {
        if (!GWT.isClient())
            throw new UnsupportedOperationException("This method can only be called in a JS runtime environment");
        execute(entityReader, entityWriter, onRequest, new double[]{expectedStatusCode}, onResponse, onException);
    }

    protected <IN, OUT> void execute(Function<String, OUT> entityReader,
                                     Function<IN, String> entityWriter,
                                     Consumer<RequestParams<IN, OUT>> onRequest,
                                     double[] expectedStatusCodes,
                                     Consumer<OUT> onResponse,
                                     Consumer<RequestException> onException) {

        RequestParams<IN, OUT> requestParams = new RequestParams<>(
            (responseCode, request, responseText) -> {

                if (responseCode == 0) {
                    onException.accept(new NoResponseException(0));
                    return;
                }

                if (responseCode == SC_BAD_REQUEST) {
                    String validationException = request.getResponseHeader(VIOLATION_EXCEPTION_HEADER);
                    if (validationException != null && validationException.equals("true")) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Received 400 status with constraint violation report: " + responseText);
                        }
                        ConstraintViolationReport report = getConstraintViolationReport(responseText);
                        onException.accept(new BadRequestException(SC_BAD_REQUEST, report));
                    } else {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Received 400 status without constraint violation report");
                        }
                        onException.accept(new BadRequestException(SC_BAD_REQUEST, null));
                    }
                    return;
                }

                if (responseCode == SC_UNAUTHORIZED) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Received 401, logging out...");
                    }
                    onException.accept(new UnauthorizedRequestException());
                    return;
                }

                if (responseCode == SC_CONFLICT) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Received 409 conflict");
                    }
                    onException.accept(new ConflictRequestException());
                    return;
                }

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("Received response status: " + responseCode);
                }

                List<Double> expectedStatusCodesList = DoubleStream.of(expectedStatusCodes).boxed().collect(Collectors.toList());
                if (!expectedStatusCodesList.contains(ANY_STATUS_CODE) && !expectedStatusCodesList.contains((double) responseCode)) {
                    onException.accept(new UnexpectedStatusRequestException(responseCode, expectedStatusCodes));
                    return;
                }

                OUT out = null;
                if (responseText != null && responseText.length() > 0 && entityReader != null) {
                    if (LOG.isLoggable(Level.FINE))
                        LOG.fine("Reading response text, length: " + responseText.length());
                    try {
                        out = entityReader.apply(responseText);
                    } catch (Exception ex) {
                        if (LOG.isLoggable(Level.FINE))
                            LOG.log(Level.FINE, "Response marshalling error", ex);
                        onException.accept(new EntityMarshallingRequestException(responseCode, ex));
                        return;
                    }
                } else {
                    LOG.fine("No response text or response entity reader");
                }
                onResponse.accept(out);
            }
        );

        if (authenticator != null) {
            authenticator.accept(requestParams);
        }

        if (entityWriter != null) {
            requestParams.setEntityWriter(entityWriter);
        }

        if (onRequest != null) {
            onRequest.accept(requestParams);
        }
    }

    protected ConstraintViolationReport getConstraintViolationReport(String responseText) {
        return constraintViolationReader.apply(responseText);
    }
}
