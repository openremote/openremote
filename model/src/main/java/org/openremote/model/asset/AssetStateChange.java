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

import org.openremote.model.Attribute;
import org.openremote.model.AttributeState;
import org.openremote.model.AttributeStateChange;

public class AssetStateChange<A extends Asset> extends AttributeStateChange {

    final protected A asset;

    public AssetStateChange(A asset, Attribute attribute, AttributeState originalState) {
        super(attribute, originalState);
        this.asset = asset;
    }

    public A getAsset() {
        return asset;
    }
}
