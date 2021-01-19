/*
 * Copyright 2020, OpenRemote Inc.
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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openremote.model.util.TsIgnore;

@Deprecated
public class MetaValidationFailure extends AttributeValidationFailure {

    protected String metaItem;

    public MetaValidationFailure(String attributeName, String metaItem, ReasonString reason) {
        super(attributeName, reason);
        this.metaItem = metaItem;
    }

    @JsonCreator
    public MetaValidationFailure(@JsonProperty("attribute") String attribute, @JsonProperty("metaItem") String metaItem, @JsonProperty("reason") ReasonString reason, @JsonProperty("parameter") String parameter) {
        super(attribute, reason, parameter);
        this.metaItem = metaItem;
    }

    public String getMetaItem() {
        return metaItem;
    }

    @Override
    public String toString() {
        return MetaValidationFailure.class.getSimpleName() + "{" +
            "metaItem='" + metaItem + '\'' +
            ", parameter='" + parameter + '\'' +
            ", reason=" + reason +
            ", attribute='" + attribute + '\'' +
            '}';
    }
}
