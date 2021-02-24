/*
 * Copyright 2020, OpenRemote Inc.
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
package org.openremote.model.asset.impl;

import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

import static org.openremote.model.Constants.*;

@Entity
public class RoomAsset extends Asset<RoomAsset> {

    public static final AttributeDescriptor<Integer> AREA = new AttributeDescriptor<>("area", ValueType.POSITIVE_INTEGER)
        .withUnits(UNITS_METRE, UNITS_SQUARED);
    public static final AttributeDescriptor<Integer> ROOM_NUMBER = new AttributeDescriptor<>("roomNumber", ValueType.POSITIVE_INTEGER);

    public static final AssetDescriptor<RoomAsset> DESCRIPTOR = new AssetDescriptor<>("door", "2eaaa2", RoomAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected RoomAsset() {
    }
    
    public RoomAsset(String name) {
        super(name);
    }

    public Optional<Integer> getArea() {
        return getAttributes().getValue(AREA);
    }

    public Optional<Integer> getRoomNumber() {
        return getAttributes().getValue(ROOM_NUMBER);
    }
}
