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

import org.openremote.model.asset.AssetType;

public class ParentPredicate {

    public String id;
    public String type;
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

    public ParentPredicate type(String type) {
        this.type = type;
        return this;
    }

    public ParentPredicate type(AssetType type) {
        return type(type.getValue());
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
