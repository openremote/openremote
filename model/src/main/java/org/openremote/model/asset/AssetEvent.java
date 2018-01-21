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
package org.openremote.model.asset;

import org.openremote.model.rules.TemporaryFact;
import org.openremote.model.util.TimeUtil;

import java.util.Optional;

/**
 * An asset attribute value change as a punctual event in rules processing.
 */
public class AssetEvent extends AbstractAssetUpdate implements TemporaryFact {

    final protected long expirationMilliseconds;

    public AssetEvent(AbstractAssetUpdate that, String expires) {
        super(that);
        this.expirationMilliseconds = TimeUtil.parseTimeString(expires);
    }

    @Override
    public long getExpirationMilliseconds() {
        return expirationMilliseconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AssetEvent that = (AssetEvent) o;

        return getAttributeRef().equals(that.getAttributeRef()) &&
            getAttribute().getValue().equals(that.getAttribute().getValue()) &&
            getTimestamp() == that.getTimestamp() &&
            Optional.ofNullable(getOldValue()).equals(Optional.ofNullable(that.getOldValue())) &&
            getOldValueTimestamp() == that.getOldValueTimestamp();
    }

    @Override
    public int hashCode() {
        return getAttributeRef().hashCode()
            + getAttribute().getValue().hashCode()
            + Long.hashCode(getTimestamp())
            + Optional.ofNullable(getOldValue()).hashCode()
            + Long.hashCode(getOldValueTimestamp());
    }
}
