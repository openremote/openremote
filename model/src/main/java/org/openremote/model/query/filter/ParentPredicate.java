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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.query.AssetQuery;

public class ParentPredicate {

    public String id;
    @JsonSerialize(converter = AssetQuery.AssetClassToStringConverter.class)
    @JsonDeserialize(converter = AssetQuery.StringToAssetClassConverter.class)
    public Class<? extends Asset<?>> type;
    public String name;
    public boolean noParent;

    public ParentPredicate() {
    }

    public ParentPredicate(String id) {
        this.id = id;
    }

    public ParentPredicate(boolean noParent) {
        this.noParent = noParent;
    }

    public ParentPredicate id(String id) {
        this.id = id;
        return this;
    }

    public ParentPredicate type(Class<? extends Asset<?>> type) {
        this.type = type;
        return this;
    }

    public ParentPredicate type(AssetDescriptor<?> type) {
        return type(type.getType());
    }

    public ParentPredicate name(String name) {
        this.name = name;
        return this;
    }

    public ParentPredicate noParent(boolean noParent) {
        this.noParent = noParent;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "id='" + id + '\'' +
            ", type='" + type + '\'' +
            ", noParent=" + noParent +
            '}';
    }
}
