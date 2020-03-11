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

import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.MetaItemDescriptor;

public class RefPredicate extends MetaPredicate {

    public RefPredicate() {
    }

    public RefPredicate(AttributeRef attributeRef) {
        this(attributeRef.getEntityId(), attributeRef.getAttributeName());
    }

    public RefPredicate(String entityId, String attributeName) {
        super(
            new StringArrayPredicate(
                new StringPredicate(entityId),
                new StringPredicate(attributeName)
            ));
    }

    public RefPredicate(String name, String entityId, String attributeName) {
        super(
            new StringPredicate(name),
            new StringArrayPredicate(
                new StringPredicate(entityId),
                new StringPredicate(attributeName)
            ));
    }

    public RefPredicate(MetaItemDescriptor metaItemDescriptor, String entityId, String attributeName) {
        this(metaItemDescriptor.getUrn(), entityId, attributeName);
    }

    public RefPredicate(StringPredicate name, AttributeRef attributeRef) {
        super(
            name,
            new StringArrayPredicate(
                new StringPredicate(attributeRef.getEntityId()),
                new StringPredicate(attributeRef.getAttributeName())
            ));
    }

    public RefPredicate(MetaItemDescriptor metaItemDescriptor, AttributeRef attributeRef) {
        this(new StringPredicate(metaItemDescriptor.getUrn()), attributeRef);
    }
}
