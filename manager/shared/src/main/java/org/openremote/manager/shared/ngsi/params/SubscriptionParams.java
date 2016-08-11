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
package org.openremote.manager.shared.ngsi.params;

import com.fasterxml.jackson.annotation.JsonInclude;

public class SubscriptionParams {
    @JsonInclude
    protected BasicEntityParams entities;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    protected Condition condition;

    public SubscriptionParams() {
    }

    public SubscriptionParams(BasicEntityParams entities, Condition condition) {
        this.entities = entities;
        this.condition = condition;
    }

    public BasicEntityParams getEntities() {
        return entities;
    }

    public void setEntities(BasicEntityParams entities) {
        this.entities = entities;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }
}
