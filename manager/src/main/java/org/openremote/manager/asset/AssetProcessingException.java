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
package org.openremote.manager.asset;

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
         * An actuator update must be for an attribute with a valid agent link.
         */
        INVALID_AGENT_LINK,

        /**
         * An attribute is linked to another attribute, but the link is invalid.
         */
        INVALID_ATTRIBUTE_LINK,

        /**
         * An attribute is linked to another attribute, but the linked attribute can't be written
         * because conversion to the target attribute's value failed.
         */
        LINKED_ATTRIBUTE_CONVERSION_FAILURE,

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

        /**
         * Any other error, typically other runtime exceptions thrown by a processor.
         */
        PROCESSOR_FAILURE,

        /**
         * Writing the asset attribute state to database failed.
         */
        STATE_STORAGE_FAILED,

        /**
         * The event value is not the excepted value for the attribute
         */
        INVALID_VALUE_FOR_WELL_KNOWN_ATTRIBUTE
    }

    final protected Reason reason;

    public AssetProcessingException(Reason reason) {
        this(reason, null);
    }

    public AssetProcessingException(Reason reason, String message) {
        this(reason, message, null);
    }

    public AssetProcessingException(Reason reason, String message, Throwable cause) {
        super(reason + (message != null ? " (" + message + ")": ""), cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
