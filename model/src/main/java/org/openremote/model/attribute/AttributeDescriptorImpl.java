/*
 * Copyright 2019, OpenRemote Inc.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.value.Value;

public class AttributeDescriptorImpl implements AttributeDescriptor {

    protected String name;
    protected String attributeName;
    protected AttributeValueDescriptor valueDescriptor;
    protected MetaItemDescriptor[] metaItemDescriptors;
    protected Value initialValue;

    @JsonCreator
    public AttributeDescriptorImpl(@JsonProperty("name") String name,
                                   @JsonProperty("attributeName") String attributeName,
                                   @JsonProperty("valueDescriptor") AttributeValueDescriptor valueDescriptor,
                                   @JsonProperty("metaItemDescriptors") MetaItemDescriptor[] metaItemDescriptors,
                                   @JsonProperty("initialValue") Value initialValue) {
        this.name = name;
        this.attributeName = attributeName;
        this.valueDescriptor = valueDescriptor;
        this.metaItemDescriptors = metaItemDescriptors;
        this.initialValue = initialValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public AttributeValueDescriptor getValueDescriptor() {
        return valueDescriptor;
    }

    @Override
    public MetaItemDescriptor[] getMetaItemDescriptors() {
        return metaItemDescriptors;
    }

    @Override
    public Value getInitialValue() {
        return initialValue;
    }
}
