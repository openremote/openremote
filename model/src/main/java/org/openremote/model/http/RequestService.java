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
package org.openremote.model.http;

import jsinterop.annotations.JsType;
import org.openremote.model.interop.Consumer;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.gwt.http.client.Response.*;
import static org.openremote.model.http.ConstraintViolationReport.VIOLATION_EXCEPTION_HEADER;

@JsType
public class RequestService {

    private static final Logger LOG = Logger.getLogger(RequestService.class.getName());
    int ANY_STATUS_CODE = -1;

    final protected Consumer<RequestParams> authenticator;
    final protected EntityReader<ConstraintViolationReport> constraintViolationReader;

    @Inject
    public RequestService(Consumer<RequestParams> authenticator,
                          EntityReader<ConstraintViolationReport> constraintViolationReader) {
        this.authenticator = authenticator;
        this.constraintViolationReader = constraintViolationReader;
    }

    public void send(Consumer<RequestParams<Void>> onRequest,
                     int expectedStatusCode,
                     Runnable onResponse,
                     Consumer<RequestException> onException) {
        sendWith(null, onRequest, expectedStatusCode, onResponse, onException);
    }

    public <OUT> void sendAndReturn(EntityReader<OUT> entityReader,
                                    Consumer<RequestParams<OUT>> onRequest,
                                    int expectedStatusCode,
                                    Consumer<OUT> onResponse,
                                    Consumer<RequestException> onException) {
        execute(entityReader, null, onRequest, new Integer[]{expectedStatusCode}, onResponse, onException);
    }

    public <IN> void sendWith(EntityWriter<IN> entityWriter,
                              Consumer<RequestParams<Void>> onRequest,
                              int expectedStatusCode,
                              Runnable onResponse,
                              Consumer<RequestException> onException) {
        execute(null, entityWriter, onRequest, new Integer[]{expectedStatusCode}, out -> onResponse.run(), onException);
    }

    public <IN, OUT> void sendWithAndReturn(EntityReader<OUT> entityReader,
                                            EntityWriter<IN> entityWriter,
                                            Consumer<RequestParams<OUT>> onRequest,
                                            int expectedStatusCode,
                                            Consumer<OUT> onResponse,
                                            Consumer<RequestException> onException) {
        execute(entityReader, entityWriter, onRequest, new Integer[]{expectedStatusCode}, onResponse, onException);
    }

    protected <IN, OUT> void execute(EntityReader<OUT> entityReader,
                                     EntityWriter<IN> entityWriter,
                                     Consumer<RequestParams<OUT>> onRequest,
                                     Integer[] expectedStatusCodes,
                                     Consumer<OUT> onResponse,
                                     Consumer<RequestException> onException) {

        RequestParams<OUT> requestParams = new RequestParams<>(
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

                List<Integer> expectedStatusCodesList = Arrays.asList(expectedStatusCodes);
                if (!expectedStatusCodesList.contains(ANY_STATUS_CODE) && !expectedStatusCodesList.contains(responseCode)) {
                    onException.accept(new UnexpectedStatusRequestException(responseCode, expectedStatusCodes));
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
        return constraintViolationReader.read(responseText);
    }
}
