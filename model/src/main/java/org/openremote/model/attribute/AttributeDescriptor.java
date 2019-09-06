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
package org.openremote.model.attribute;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.openremote.model.value.Value;

/**
 * Describes an {@link Attribute} that can be added to an {@link org.openremote.model.asset.Asset};
 * the {@link #getAttributeName} is the unique identifier.
 * <p>
 * A custom project can add its own descriptors through {@link org.openremote.model.asset.AssetModelProvider}.
 * <p>
 */
@JsonDeserialize(as = AttributeDescriptorImpl.class)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public interface AttributeDescriptor {

    @JsonProperty
    String getAttributeName();

    @JsonProperty
    AttributeValueDescriptor getValueDescriptor();

    @JsonProperty
    MetaItemDescriptor[] getMetaItemDescriptors();

    @JsonProperty
    Value getInitialValue();
}
