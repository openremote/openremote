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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openremote.model.event.Event;
import org.openremote.model.event.RespondableEvent;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.query.AssetQuery;
import org.openremote.model.util.TextUtil;

import java.util.function.Consumer;

/**
 * A client sends this event to the server to read the specified asset, optionally restricting which attributes
 * expecting the server to answer "soon" with an {@link AssetEvent}. If the server decides that the client doesn't have
 * the right permissions, or if anything else is not in order (e.g. the asset doesn't exist), the server might not react
 * at all.
 */
public class ReadAssetEvent extends SharedEvent implements HasAssetQuery, RespondableEvent {

    protected String assetId;
    protected AssetQuery assetQuery;
    @JsonIgnore
    protected Consumer<Event> responseConsumer;

    @JsonCreator
    public ReadAssetEvent(@JsonProperty("assetId") String assetId) {
        this.assetId = assetId;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public AssetQuery getAssetQuery() {
        if (assetQuery == null) {
            if (!TextUtil.isNullOrEmpty(assetId)) {
                assetQuery = new AssetQuery().ids(assetId);
            }
        }
        return assetQuery;
    }

    @Override
    public Consumer<Event> getResponseConsumer() {
        return responseConsumer;
    }

    @Override
    public void setResponseConsumer(Consumer<Event> responseConsumer) {
        this.responseConsumer = responseConsumer;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetId='" + assetId + '\'' +
            '}';
    }
}
