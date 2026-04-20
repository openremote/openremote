/*
 * Copyright 2021, OpenRemote Inc.
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

public class AttributeWriteResult {

    protected AttributeRef ref;
    protected AttributeWriteFailure failure;

    public AttributeWriteResult(AttributeRef ref) {
        this.ref = ref;
    }

    @JsonCreator
    public AttributeWriteResult(@JsonProperty("ref") AttributeRef ref, @JsonProperty("failure") AttributeWriteFailure failure) {
        this.ref = ref;
        this.failure = failure;
    }

    public AttributeRef getRef() {
        return ref;
    }

    public AttributeWriteFailure getFailure() {
        return failure;
    }
}
