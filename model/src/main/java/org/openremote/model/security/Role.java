/*
 * Copyright 2016, OpenRemote Inc.
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
package org.openremote.model.security;

import java.util.Arrays;

/**
 * Represents a role; depending on the context then different values will be populated. If getting roles related to a user
 * then the {@link #isAssigned} will indicate whether the role is assigned to the user or not and {@link #isComposite}
 * will indicate if it is a composite role. If getting roles for a client then if {@link #isComposite} then the
 */
public class Role {

    protected String id;
    protected String name;
    protected String description;
    protected boolean composite;
    protected Boolean assigned;
    protected String[] compositeRoleIds;

    public Role() {
    }

    public Role(String id, String name, boolean composite, Boolean assigned, String[] compositeRoleIds) {
        this.id = id;
        this.name = name;
        this.composite = composite;
        this.assigned = assigned;
        this.compositeRoleIds = compositeRoleIds;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public Role setDescription(String description) {
        this.description = description;
        return this;
    }

    public void setAssigned(Boolean assigned) {
        this.assigned = assigned;
    }

    public Boolean isAssigned() {
        return assigned;
    }

    public void setAssigned(boolean assigned) {
        this.assigned = assigned;
    }

    public boolean isComposite() {
        return composite;
    }

    public void setComposite(boolean composite) {
        this.composite = composite;
    }

    public String[] getCompositeRoleIds() {
        return compositeRoleIds;
    }

    public void setCompositeRoleIds(String[] compositeRoleIds) {
        this.compositeRoleIds = compositeRoleIds;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", composite=" + composite +
            ", compositeRoleNames=" + Arrays.toString(compositeRoleIds) +
            ", assigned=" + assigned +
            '}';
    }
}
