/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.manager.alert;

import org.openremote.model.alert.Alert;

/**
 * The reason why sending an {@link Alert} failed.
 */
public class AlertProcessingException extends RuntimeException {

    public enum Reason {

        /**
         * Missing {@link Alert}.
         */
        MISSING_ALERT,

        /**
         * Missing {@link Alert}.
         */
        MISSING_CONTENT,

        /**
         * Missing {@link Alert.Trigger}.
         */
        MISSING_TRIGGER
    }

    final protected Reason reason;
    final protected String message;

    public AlertProcessingException(Reason reason) {
        this(reason, null);
    }

    public AlertProcessingException(Reason reason, String message) {
        this.reason = reason;
        this.message = message;
    }

    public Reason getReason() {
        return reason;
    }

    public String getReasonPhrase() {
        return getReason() + (message != null ? " (" + message + ")": "");
    }
}
