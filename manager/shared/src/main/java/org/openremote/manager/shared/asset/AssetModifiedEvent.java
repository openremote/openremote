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
package org.openremote.manager.shared.asset;

import org.openremote.manager.shared.event.Event;

public class AssetModifiedEvent extends Event {

    public enum Cause {
        CREATE,
        UPDATE,
        DELETE,
        CHILDREN_MODIFIED
    }

    protected AssetInfo assetInfo;
    protected Cause cause;

    public AssetModifiedEvent() {
    }

    public AssetModifiedEvent(Asset asset, Cause cause) {
        this(new AssetInfo(asset), cause);
    }

    public AssetModifiedEvent(AssetInfo assetInfo, Cause cause) {
        this.assetInfo = assetInfo;
        this.cause = cause;
    }

    public AssetInfo getAssetInfo() {
        return assetInfo;
    }

    public Cause getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "assetInfo='" + assetInfo+ '\'' +
            ", cause=" + cause +
            "}";
    }
}
