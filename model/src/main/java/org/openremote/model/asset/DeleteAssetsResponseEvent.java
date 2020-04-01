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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Arrays;
import java.util.List;

/**
 * A client sends this event in response to a {@link DeleteAssetsRequestEvent} indicating if the delete
 * was successful.
 */
public class DeleteAssetsResponseEvent extends SharedEvent {

    protected String name;
    protected boolean deleted;

    @JsonCreator
    public DeleteAssetsResponseEvent(@JsonProperty("name") String name, @JsonProperty("deleted") boolean deleted) {
        this.name = name;
        this.deleted = deleted;
    }

    public DeleteAssetsResponseEvent(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "name='" + name + '\'' +
            "deleted='" + deleted + '\'' +
            '}';
    }
}
