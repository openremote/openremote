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

    final protected Asset asset;
    final protected Cause cause;
    final protected boolean forceRootRefresh;

    public AssetModifiedEvent(Asset asset, Cause cause) {
        this(asset, cause, false);
    }

    public AssetModifiedEvent(Asset asset, Cause cause, boolean forceRootRefresh) {
        this.asset = asset;
        this.cause = cause;
        this.forceRootRefresh = forceRootRefresh;
    }

    public Asset getAsset() {
        return asset;
    }

    public Cause getCause() {
        return cause;
    }

    public boolean isForceRootRefresh() {
        return forceRootRefresh;
    }
}
