/*
 * Copyright 2019, OpenRemote Inc.
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * This event is used in response to a {@link ReadAssetsEvent}.
 */
public class AssetsEvent extends SharedEvent {

    protected List<Asset<?>> assets;

    @JsonCreator
    public AssetsEvent(@JsonProperty("assets") List<Asset<?>> assets) {
        this.assets = assets;
    }

    public List<Asset<?>> getAssets() {
        return assets;
    }

    @Override
    public String toString() {
        return AssetsEvent.class.getSimpleName() + "{" +
            ", assets=" + (assets == null ? "null" : assets.stream().map(Asset::getId).collect(Collectors.joining())) +
            '}';
    }
}
