/*
 * Copyright 2021, OpenRemote Inc.
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
package org.openremote.agent.protocol.bluetooth.mesh.utils;

import org.bouncycastle.crypto.InvalidCipherTextException;

public class ExtendedInvalidCipherTextException extends InvalidCipherTextException {

    private final String tag;
    private final String message;
    private final Throwable cause;

    /**
     * Constructs ExtendedInvalidCipherTextException
     *
     * @param message Exception message
     * @param cause   Throwable cause
     * @param tag     class tag name
     */
    public ExtendedInvalidCipherTextException(final String message, final Throwable cause, final String tag) {
        this.message = message;
        this.cause = cause;
        this.tag = tag;

    }

    /**
     * Returns the tag name of the class.
     * <p>
     * Using the tag we can find out on which class the exception occurred.
     * </p>
     */
    public String getTag() {
        return tag;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable getCause() {
        return cause;
    }
}

