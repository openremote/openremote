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
package org.openremote.model.query.filter;

import org.openremote.model.attribute.MetaItemDescriptor;
import org.openremote.model.query.BaseAssetQuery;

public class AttributeMetaPredicate {

    public StringPredicate itemNamePredicate;
    public ValuePredicate itemValuePredicate;

    public AttributeMetaPredicate() {
    }

    public AttributeMetaPredicate(StringPredicate itemNamePredicate) {
        this.itemNamePredicate = itemNamePredicate;
    }

    public AttributeMetaPredicate(MetaItemDescriptor metaItemDescriptor) {
        this.itemNamePredicate = new StringPredicate(metaItemDescriptor.getUrn());
    }

    public AttributeMetaPredicate(ValuePredicate itemValuePredicate) {
        this.itemValuePredicate = itemValuePredicate;
    }

    public AttributeMetaPredicate(StringPredicate itemNamePredicate, ValuePredicate itemValuePredicate) {
        this.itemNamePredicate = itemNamePredicate;
        this.itemValuePredicate = itemValuePredicate;
    }

    public AttributeMetaPredicate(MetaItemDescriptor metaItemDescriptor, ValuePredicate itemValuePredicate) {
        this(new StringPredicate(metaItemDescriptor.getUrn()), itemValuePredicate);
    }

    public AttributeMetaPredicate itemName(StringPredicate itemNamePredicate) {
        this.itemNamePredicate = itemNamePredicate;
        return this;
    }

    public AttributeMetaPredicate itemName(MetaItemDescriptor metaItemDescriptor) {
        return itemName(new StringPredicate(metaItemDescriptor.getUrn()));
    }

    public AttributeMetaPredicate itemValue(ValuePredicate itemValuePredicate) {
        this.itemValuePredicate = itemValuePredicate;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "itemNamePredicate=" + itemNamePredicate +
            ", itemValuePredicate=" + itemValuePredicate +
            '}';
    }
}
