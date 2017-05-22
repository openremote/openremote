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
package org.openremote.manager.shared.asset;

import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.asset.AssetType;
import org.openremote.model.attribute.AttributeExecuteStatus;

/**
 * The reason why processing an {@link org.openremote.model.attribute.AttributeEvent} failed.
 */
public class AssetProcessingException extends RuntimeException {

    public enum Reason {

        /**
         * Missing {@link org.openremote.model.attribute.AttributeEvent.Source}.
         */
        MISSING_SOURCE,

        /**
         * The source of the event does not match the expected source (e.g. expected client message
         * but received sensor read).
         */
        ILLEGAL_SOURCE,

        /**
         * The asset does not exist.
         */
        ASSET_NOT_FOUND,

        /**
         * The attribute does not exist.
         */
        ATTRIBUTE_NOT_FOUND,

        /**
         * A sensor update must be for an attribute with a valid agent link.
         */
        INVALID_AGENT_LINK,

        /**
         * Attributes of an {@link AssetType#AGENT} can not be individually updated.
         */
        ILLEGAL_AGENT_UPDATE,

        /**
         * Invalid {@link AttributeExecuteStatus} for {@link AssetAttribute#isExecutable()} attribute,
         * must be {@link AttributeExecuteStatus#isWrite()}.
         */
        INVALID_ATTRIBUTE_EXECUTE_STATUS,

        /**
         * No authentication/authorization context available.
         */
        NO_AUTH_CONTEXT,

        /**
         * Realm configuration or user privileges do not allow update (e.g. realm inactive, user is
         * missing required role, attribute is read-only).
         */
        INSUFFICIENT_ACCESS,

        /**
         * The event timestamp is later than the processing time.
         */
        EVENT_IN_FUTURE,

        /**
         * The event timestamp is older than the last updated timestamp of the attribute.
         */
        EVENT_OUTDATED,

        /**
         * Applying the event violates constraints of the attribute.
         */
        ATTRIBUTE_VALIDATION_FAILURE,
    }

    final protected Reason reason;
    final protected String message;

    public AssetProcessingException(Reason reason) {
        this(reason, null);
    }

    public AssetProcessingException(Reason reason, String message) {
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
