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
import org.openremote.model.attribute.AttributeExecuteStatus;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.MetaItemType;
import org.openremote.model.value.ValueFormat;
import org.openremote.model.value.ValueType;

import javax.persistence.Entity;
import java.util.Optional;

@Entity
public class DoorAsset extends Asset<DoorAsset> {

    public static final AttributeDescriptor<Boolean> POSITION = new AttributeDescriptor<>("position", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withFormat(ValueFormat.BOOLEAN_AS_OPEN_CLOSED());

    public static final AttributeDescriptor<Boolean> LOCKED = new AttributeDescriptor<>("locked", ValueType.BOOLEAN,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withOptional(true);

    public static final AttributeDescriptor<String> LAST_ACCESS = new AttributeDescriptor<>("lastAccess", ValueType.TEXT,
        new MetaItem<>(MetaItemType.READ_ONLY)
    ).withOptional(true);

    public static final AttributeDescriptor<AttributeExecuteStatus> UNLOCK = new AttributeDescriptor<>("unlock", ValueType.EXECUTION_STATUS)
        .withOptional(true);

    public static final AssetDescriptor<DoorAsset> DESCRIPTOR = new AssetDescriptor<>("door", "ae2eb6", DoorAsset.class);

    /**
     * For use by hydrators (i.e. JPA/Jackson)
     */
    protected DoorAsset() {
    }

    public DoorAsset(String name) {
        super(name);
    }

    public Optional<Boolean> getPosition() {
        return getAttributes().getValue(POSITION);
    }

    public Optional<Boolean> getLocked() {
        return getAttributes().getValue(LOCKED);
    }

    public Optional<String> getLastAccess() {
        return getAttributes().getValue(LAST_ACCESS);
    }
}
