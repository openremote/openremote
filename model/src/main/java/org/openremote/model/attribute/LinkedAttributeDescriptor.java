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

public class LinkedAttributeDescriptor {

    protected final String name;
    protected final String displayName;
    protected final AttributeValueDescriptor attributeValueDescriptor;
    protected final boolean readOnly;
    protected final boolean executable;
    protected final MetaItem[] metaItems;

    public LinkedAttributeDescriptor(
        String name,
        String displayName,
        AttributeValueType attributeValueTypeDescriptor,
        boolean readOnly,
        boolean executable,
        MetaItem[] metaItems) {
        this.name = name;
        this.displayName = displayName;
        this.attributeValueDescriptor = attributeValueTypeDescriptor;
        this.readOnly = readOnly;
        this.executable = executable;
        this.metaItems = metaItems;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public AttributeValueDescriptor getAttributeValueDescriptor() {
        return attributeValueDescriptor;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public boolean isExecutable() {
        return executable;
    }

    public MetaItem[] getMetaItems() {
        return metaItems;
    }
}
