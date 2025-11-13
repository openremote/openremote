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
package org.openremote.model.value;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.attribute.MetaItem;
import org.openremote.model.attribute.MetaMap;

import java.util.Optional;

public interface MetaHolder {

    @JsonProperty
    MetaMap getMeta();

    default <U> Optional<U> getMetaValue(MetaItemDescriptor<U> metaItemDescriptor) {
        return getMeta() != null ? getMeta().getValue(metaItemDescriptor) : Optional.empty();
    }

    default boolean hasMeta(MetaItemDescriptor<?> metaItemDescriptor) {
        return getMeta() != null && getMeta().has(metaItemDescriptor);
    }

    default boolean hasMeta(String metaItemName) {
        return getMeta() != null && getMeta().has(metaItemName);
    }

    default <U> Optional<MetaItem<U>> getMetaItem(MetaItemDescriptor<U> metaItemDescriptor) {
        return getMeta() != null ? getMeta().get(metaItemDescriptor) : Optional.empty();
    }
}
