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

import org.openremote.model.event.shared.SharedEvent;

public class AssetModifiedEvent extends SharedEvent {

    public enum Cause {
        CREATE,
        UPDATE,
        DELETE,
        CHILDREN_MODIFIED
    }

    protected String assetId;
    protected Cause cause;

    protected AssetModifiedEvent() {
    }

    public AssetModifiedEvent(Asset asset, Cause cause) {
        this(asset.getId(), cause);
    }

    public AssetModifiedEvent(String assetId, Cause cause) {
        this.assetId = assetId;
        this.cause = cause;
    }

    public String getAssetId() {
        return assetId;
    }

    public Cause getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetId='" + assetId + '\'' +
            ", cause=" + cause +
            "}";
    }
}
