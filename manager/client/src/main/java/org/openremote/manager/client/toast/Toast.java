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
package org.openremote.manager.client.toast;

public class Toast {

    public static final int INFO_DEFAULT_MAX_AGE = 5000;

    public enum Type {
        INFO,
        FAILURE,
        DURABLE_FAILURE
    }

    final Type type;
    final String text;
    final long timestamp;
    final long maxAgeMillis;

    public Toast(Type type, String text, int maxAgeMillis) {
        this.type = type;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.maxAgeMillis = maxAgeMillis;
    }

    public Type getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getMaxAgeMillis() {
        return maxAgeMillis;
    }

    public boolean isDurableFailure() {
        return getText() != null && getType().equals(Type.DURABLE_FAILURE);
    }

    public boolean isExpired() {
        return !isDurableFailure() && (getTimestamp() + getMaxAgeMillis()) <= System.currentTimeMillis();
    }
}
