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
package org.openremote.manager.server.assets;

import org.openremote.container.Container;
import org.openremote.manager.shared.ngsi.Attribute;

import java.util.List;

public interface ContextProvider {

    void configure(Container container) throws Exception;

    void stop();

    String getContextProviderUri();

    boolean registerAssetProvider(String assetType, String assetId, List<Attribute> attributes, AssetProvider provider);

    void unregisterAssetProvider(String assetType, String assetId, AssetProvider provider);

    /**
     * Get the refresh interval duration of this context providers registrations
     * @return Registration refresh interval in seconds
     */
    int getRefreshInterval();

    /**
     * Set the refresh interval of this context providers registrations
     */
    void setRefreshInterval(int seconds);
}
