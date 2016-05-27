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
package org.openremote.manager.shared.http;

import org.openremote.manager.shared.Consumer;
import org.openremote.manager.shared.validation.ConstraintViolationReport;

import static org.openremote.manager.shared.validation.ConstraintViolationReport.VIOLATION_EXCEPTION_HEADER;

public abstract class AbstractCallback<T> implements Callback<T> {

    public static final int ANY_STATUS_CODE = -1;

    final protected int expectedStatusCode;
    final protected Consumer<T> onSuccess;
    final protected Consumer<RequestException> onFailure;

    public AbstractCallback(Consumer<T> onSuccess, Consumer<RequestException> onFailure) {
        this(ANY_STATUS_CODE, onSuccess, onFailure);
    }

    public AbstractCallback(int expectedStatusCode, Consumer<T> onSuccess, Consumer<RequestException> onFailure) {
        this.expectedStatusCode = expectedStatusCode;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    public void call(int responseCode, Request.XMLHttpRequest request, Object entity) {
        if (responseCode == 0) {
            onFailure.accept(new RequestException(0, "No response"));
            return;
        }

        if (handleUnauthorizedRequest() && responseCode == 401) {
            onFailure.accept(new UnauthorizedRequestException(401));
            return;
        }

        if (handleBadRequest() && responseCode == 400) {
            String validationException = request.getResponseHeader(VIOLATION_EXCEPTION_HEADER);
            if (validationException != null && validationException.equals("true")) {
                ConstraintViolationReport report = getConstraintViolationReport(entity);
                onFailure.accept(new BadRequestException(400, report));
            } else {
                onFailure.accept(new BadRequestException(400));
            }
            return;
        }

        if (expectedStatusCode != ANY_STATUS_CODE && responseCode != expectedStatusCode) {
            onFailure.accept(new RequestException(
                responseCode,
                "Expected response status code " + expectedStatusCode + " but received " + responseCode + ".")
            );
            return;
        }

        onSuccess.accept(readMessageBody(responseCode, entity));
    }

    protected boolean handleUnauthorizedRequest() {
        return true;
    }

    protected boolean handleBadRequest() {
        return true;
    }

    protected ConstraintViolationReport getConstraintViolationReport(Object entity) {
        return null;
    }

    /**
     * The object returned by RESTEasy JavaScript API is either
     * <p>
     * <ul>
     * <li>a JavascriptObject if the content type is JSON</li>
     * <li>a Document if the content type is XML</li>
     * <li>or a String</li>
     * </ul>
     * <p>
     * This method converts to the desired type.
     */
    protected abstract T readMessageBody(int responseCode, Object entity);
}
