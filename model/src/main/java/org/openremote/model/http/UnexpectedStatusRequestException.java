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

import java.util.Arrays;

@JsType
public class UnexpectedStatusRequestException extends RequestException {

    protected double[] expectedStatusCodes;

    public UnexpectedStatusRequestException(double statusCode, double[] expectedStatusCodes) {
        super(statusCode, "Unexpected response status");
        this.expectedStatusCodes = expectedStatusCodes;
    }

    public double[] getExpectedStatusCodes() {
        return expectedStatusCodes;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "expectedStatusCodes=" + Arrays.toString(expectedStatusCodes) +
            ", statusCode=" + statusCode +
            '}';
    }
}
