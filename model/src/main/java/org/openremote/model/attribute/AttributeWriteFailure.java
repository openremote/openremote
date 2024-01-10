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
package org.openremote.model.attribute;

public enum AttributeWriteFailure {

    /**
     * The asset does not exist.
     */
    ASSET_NOT_FOUND,

    /**
     * The attribute does not exist.
     */
    ATTRIBUTE_NOT_FOUND,

    /**
     * Realm configuration or user privileges do not allow update (e.g. realm inactive, user is missing required role,
     * attribute is read-only).
     */
    INSUFFICIENT_ACCESS,

    /**
     * Value has incorrect type or has failed validation rules.
     */
    INVALID_VALUE,

    /**
     * Any unknown error that is thrown by an interceptor.
     */
    INTERCEPTOR_FAILURE,

    /**
     * Writing the asset attribute state to database failed.
     */
    STATE_STORAGE_FAILED,

    /**
     * The event interceptor/consumer cannot process the event.
     */
    CANNOT_PROCESS,

    /**
     * The attribute event processor queue is full.
     */
    QUEUE_FULL,

    /**
     * Fallback failure when no other value makes sense.
     */
    UNKNOWN
}
