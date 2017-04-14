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

import org.openremote.model.AttributeRef;
import org.openremote.model.Meta;
import org.openremote.model.util.AttributeUtil;

/**
 * This is a wrapper for AssetAttribute that can provide convenience
 * methods for getting/setting the attribute value and/or meta items
 * that should be in the dynamic part of the attribute class.
 *
 * It is deliberately minimal as any standard attribute manipulation
 * should be done on the attribute class itself.
 *
 */
public abstract class AbstractAttributeWrapper<CHILD extends AbstractAttributeWrapper> {

    AssetAttribute attribute;

    /**
     * Default constructor will also initialise the attribute, subclasses
     * should provide both constructors for convenience and consistency.
     *
     */
    protected AbstractAttributeWrapper(AssetAttribute attribute) {
        this(attribute, true);
    }

    /**
     * Should optionally initialise the attribute by calling initialise method.
     */
    protected AbstractAttributeWrapper(AssetAttribute attribute, boolean initialise) {
        this.attribute = attribute;
        if (initialise) {
            initialise();
        }
    }

    public AssetAttribute getAttribute() {
        return attribute;
    }

    public AttributeRef getAttributeReference() {
        return attribute.getReference();
    }

    public Meta getMeta() {
        return attribute.getMeta();
    }

    public void setMeta(Meta meta) {
        attribute.setMeta(meta);
    }

    /**
     * Indicates whether or not the attribute is valid for this wrapper type. Should be overridden in sub classes
     * where needed.
     */
    public boolean isValid() {
        return isValid(getAttribute());
    }

    /**
     * Initialise this attribute with all the static data relating to
     * this attribute wrapper type.
     */
    public abstract void initialise();

    /**
     * @return a static filter for this wrapper type
     */
    public abstract AttributeWrapperFilter<CHILD> getFilter();

    public static boolean isValid(AssetAttribute assetAttribute) {
        return AttributeUtil.nameIsValid(assetAttribute.getName()) && assetAttribute.getType() != null;
    }
}
