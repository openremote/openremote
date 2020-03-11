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

public class MetaPredicate {

    public StringPredicate itemNamePredicate;
    public ValuePredicate itemValuePredicate;

    public MetaPredicate() {
    }

    public MetaPredicate(StringPredicate itemNamePredicate) {
        this.itemNamePredicate = itemNamePredicate;
    }

    public MetaPredicate(MetaItemDescriptor metaItemDescriptor) {
        this.itemNamePredicate = new StringPredicate(metaItemDescriptor.getUrn());
    }

    public MetaPredicate(ValuePredicate itemValuePredicate) {
        this.itemValuePredicate = itemValuePredicate;
    }

    public MetaPredicate(StringPredicate itemNamePredicate, ValuePredicate itemValuePredicate) {
        this.itemNamePredicate = itemNamePredicate;
        this.itemValuePredicate = itemValuePredicate;
    }

    public MetaPredicate(MetaItemDescriptor metaItemDescriptor, ValuePredicate itemValuePredicate) {
        this(new StringPredicate(metaItemDescriptor.getUrn()), itemValuePredicate);
    }

    public MetaPredicate itemName(StringPredicate itemNamePredicate) {
        this.itemNamePredicate = itemNamePredicate;
        return this;
    }

    public MetaPredicate itemName(MetaItemDescriptor metaItemDescriptor) {
        return itemName(new StringPredicate(metaItemDescriptor.getUrn()));
    }

    public MetaPredicate itemValue(ValuePredicate itemValuePredicate) {
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
