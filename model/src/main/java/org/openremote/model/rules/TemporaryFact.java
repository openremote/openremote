/*
 * Copyright 2018, OpenRemote Inc.
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
package org.openremote.model.rules;

/**
 * A rule fact that has a timestamp, it expires after a certain time.
 */
public interface TemporaryFact {

    /**
     * This value defines the periodic firing of the rules engines when temporary
     * facts are present, and therefore has an impact on system load. If a temporary
     * fact has a shorter expiration time, it's not guaranteed to be removed within
     * that time.
     */
    int GUARANTEED_MIN_EXPIRATION_MILLIS = 3000;

    long getTimestamp();

    long getExpirationMilliseconds();

    default boolean isExpired(long currentTimestamp) {
        return getTimestamp() + getExpirationMilliseconds() < currentTimestamp;
    }
}
