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
package org.openremote.app.client.rest;

import com.google.gwt.core.client.GWT;
import elemental2.core.Global;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import org.openremote.model.http.*;
import org.openremote.model.interop.BiConsumer;
import org.openremote.model.interop.Consumer;
import org.openremote.model.interop.Function;
import org.openremote.model.interop.Runnable;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import static com.google.gwt.http.client.Response.*;
import static org.openremote.model.http.BadRequestError.VIOLATION_EXCEPTION_HEADER;

@JsType
public class Requests {

    private static final Logger LOG = Logger.getLogger(Requests.class.getName());

    public static final double ANY_STATUS_CODE = -1;

    // This consumer gets request params, sets credentials to authorize the request, then calls the given runnable
    final protected BiConsumer<RequestParams, Runnable> requestAuthorizer;

    final protected Runnable beforeRequest;

    final protected Runnable afterRequest;

    final protected Consumer<RequestError> onException;

    final protected Function<String, ConstraintViolationReport> constraintViolationReader;

    public Requests(BiConsumer<RequestParams, Runnable> requestAuthorizer,
                    Runnable beforeRequest,
                    Runnable afterRequest,
                    Consumer<RequestError> onException,
                    Function<String, ConstraintViolationReport> constraintViolationReader) {
        this.requestAuthorizer = requestAuthorizer;
        this.beforeRequest = beforeRequest;
        this.afterRequest = afterRequest;
        this.onException = onException;
        this.constraintViolationReader = constraintViolationReader;
    }

    public void configure(String apiUrl) {
        REST.apiURL = apiUrl;
        REST.loglevel = LOG.isLoggable(Level.FINEST) ? 1 : 0;
    }

    @JsIgnore
    public void send(Consumer<RequestParams<Void, Void>> onRequest,
                     int expectedStatusCode,
                     Runnable onResponse) {
        execute(null, null, onRequest, new double[]{expectedStatusCode}, out -> onResponse.run(), null);
    }

    public void send(Consumer<RequestParams<Void, Void>> onRequest,
                     double expectedStatusCode,
                     Runnable onResponse) {
        if (!GWT.isClient())
            throw new UnsupportedOperationException("This method can only be called in a JS runtime environment");
        execute(null, null, onRequest, new double[]{expectedStatusCode}, out -> onResponse.run(), null);
    }

    @JsIgnore
    public <OUT> void sendAndReturn(EntityReader<OUT> entityReader,
                                    Consumer<RequestParams<Void, OUT>> onRequest,
                                    double[] expectedStatusCodes,
                                    Consumer<OUT> onResponse) {
        execute(entityReader::read, null, onRequest, expectedStatusCodes, onResponse, null);
    }

    @JsIgnore
    public <OUT> void sendAndReturn(EntityReader<OUT> entityReader,
                                    Consumer<RequestParams<Void, OUT>> onRequest,
                                    int expectedStatusCode,
                                    Consumer<OUT> onResponse) {
        execute(entityReader::read, null, onRequest, new double[]{expectedStatusCode}, onResponse, null);
    }

    public void sendAndReturn(Consumer<RequestParams<Void, Any>> onRequest,
                              double expectedStatusCode,
                              Consumer<Any> onResponse) {
        if (!GWT.isClient())
            throw new UnsupportedOperationException("This method can only be called in a JS runtime environment");
        execute(object -> (Any) Global.JSON.parse(object), null, onRequest, new double[]{expectedStatusCode}, onResponse, null);
    }

    @JsIgnore
    public <IN> void sendWith(EntityWriter<IN> entityWriter,
                              Consumer<RequestParams<IN, Void>> onRequest,
                              int expectedStatusCode,
                              Runnable onResponse,
                              Consumer<ConstraintViolation[]> onConstraintViolation) {
        execute(null, entityWriter::write, onRequest, new double[]{expectedStatusCode}, out -> onResponse.run(), onConstraintViolation);
    }

    @JsIgnore
    public <IN> void sendWith(EntityWriter<IN> entityWriter,
                              Consumer<RequestParams<IN, Void>> onRequest,
                              int expectedStatusCode,
                              Runnable onResponse) {
        execute(null, entityWriter::write, onRequest, new double[]{expectedStatusCode}, out -> onResponse.run(), null);
    }

    public <IN> void sendWith(Function<IN, String> entityWriter,
                              Consumer<RequestParams<IN, Void>> onRequest,
                              double expectedStatusCode,
                              Runnable onResponse,
                              Consumer<ConstraintViolation[]> onConstraintViolation) {
        if (!GWT.isClient())
            throw new UnsupportedOperationException("This method can only be called in a JS runtime environment");
        execute(null, entityWriter, onRequest, new double[]{expectedStatusCode}, out -> onResponse.run(), onConstraintViolation);
    }

    @JsIgnore
    public <IN, OUT> void sendWithAndReturn(EntityReader<OUT> entityReader,
                                            EntityWriter<IN> entityWriter,
                                            Consumer<RequestParams<IN, OUT>> onRequest,
                                            int expectedStatusCode,
                                            Consumer<OUT> onResponse,
                                            Consumer<ConstraintViolation[]> onConstraintViolation) {
        execute(entityReader::read, entityWriter::write, onRequest, new double[]{expectedStatusCode}, onResponse, onConstraintViolation);
    }


    @JsIgnore
    public <IN, OUT> void sendWithAndReturn(EntityReader<OUT> entityReader,
                                            EntityWriter<IN> entityWriter,
                                            Consumer<RequestParams<IN, OUT>> onRequest,
                                            int expectedStatusCode,
                                            Consumer<OUT> onResponse) {
        execute(entityReader::read, entityWriter::write, onRequest, new double[]{expectedStatusCode}, onResponse, null);
    }

    public <IN, OUT> void sendWithAndReturn(Function<String, OUT> entityReader,
                                            Function<IN, String> entityWriter,
                                            Consumer<RequestParams<IN, OUT>> onRequest,
                                            double expectedStatusCode,
                                            Consumer<OUT> onResponse,
                                            Consumer<ConstraintViolation[]> onConstraintViolation) {
        if (!GWT.isClient())
            throw new UnsupportedOperationException("This method can only be called in a JS runtime environment");
        execute(entityReader, entityWriter, onRequest, new double[]{expectedStatusCode}, onResponse, onConstraintViolation);
    }

    protected <IN, OUT> void execute(Function<String, OUT> entityReader,
                                     Function<IN, String> entityWriter,
                                     Consumer<RequestParams<IN, OUT>> onRequest,
                                     double[] expectedStatusCodes,
                                     Consumer<OUT> onResponse,
                                     Consumer<ConstraintViolation[]> onConstraintViolation) {

        if (beforeRequest != null) {
            beforeRequest.run();
        }

        RequestParams<IN, OUT> requestParams = new RequestParams<>((responseCode, request, responseText) -> {

            if (afterRequest != null) {
                afterRequest.run();
            }

            switch (responseCode) {
                case 0:
                    onException.accept(new NoResponseError(0));
                    return;
                case SC_BAD_REQUEST:
                    String validationException = request.getResponseHeader(VIOLATION_EXCEPTION_HEADER);
                    if (validationException != null && validationException.equals("true")) {
                        ConstraintViolationReport report = constraintViolationReader.apply(responseText);
                        if (onConstraintViolation != null) {
                            onConstraintViolation.accept(report.getAllViolations());
                        } else {
                            onException.accept(new BadRequestError(report));
                        }
                    } else {
                        onException.accept(new BadRequestError());
                    }
                    return;
                case SC_UNAUTHORIZED:
                    onException.accept(new UnauthorizedRequestError());
                    return;
                case SC_NOT_FOUND:
                    onException.accept(new NotFoundRequestError());
                    return;
                case SC_CONFLICT:
                    // A 409 conflict should be recoverable, so treat it like a constraint violation
                    if (onConstraintViolation != null) {
                        ConstraintViolation conflictViolation = new ConstraintViolation();
                        conflictViolation.setConstraintType(ConstraintViolation.Type.CONFLICT);
                        onConstraintViolation.accept(new ConstraintViolation[] { conflictViolation });
                    } else {
                        onException.accept(new ConflictRequestError());
                    }
                    return;
                default:
                    List<Double> expectedStatusCodesList = DoubleStream.of(expectedStatusCodes).boxed().collect(Collectors.toList());
                    if (!expectedStatusCodesList.contains(ANY_STATUS_CODE) && !expectedStatusCodesList.contains((double) responseCode)) {
                        onException.accept(new UnexpectedStatusRequestError(responseCode, expectedStatusCodes));
                        return;
                    }

                    OUT out = null;
                    if (responseText != null && responseText.length() > 0 && entityReader != null) {
                        try {
                            out = entityReader.apply(responseText);
                        } catch (Exception ex) {
                            onException.accept(new EntityMarshallingRequestError(responseCode, ex));
                            return;
                        }
                    }
                    onResponse.accept(out);
            }
        });

        if (onRequest != null) {
            if (entityWriter != null) {
                requestParams.setEntityWriter(entityWriter);
            }
            requestAuthorizer.accept(requestParams, () -> onRequest.accept(requestParams));
        }
    }
}
