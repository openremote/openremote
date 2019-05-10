/*
 * Copyright 2018, OpenRemote Inc.
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
package org.openremote.model.rules.json;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.function.Function;

@JsonSubTypes({
        @JsonSubTypes.Type(value = RuleActionWait.class, name = "wait"),
        @JsonSubTypes.Type(value = RuleActionWriteAttribute.class, name = "write-attribute"),
        @JsonSubTypes.Type(value = RuleActionNotification.class, name = "notification"),
        @JsonSubTypes.Type(value = RuleActionUpdateAttribute.class, name = "update-attribute")
})
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "action"
)
public abstract class RuleAction {

    public RuleActionTarget target;
}
