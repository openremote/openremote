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
package org.openremote.manager.client.app;

import com.google.gwt.place.shared.Place;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.openremote.manager.client.assets.AssetDetailActivity;
import org.openremote.manager.client.assets.AssetsPlace;
import org.openremote.manager.client.flows.FlowsActivity;
import org.openremote.manager.client.flows.FlowsPlace;
import org.openremote.manager.client.map.MapActivity;
import org.openremote.manager.client.map.MapPlace;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.mvp.AppActivityMapper;

public class MainContentActivityMapper implements AppActivityMapper {

    private final Provider<AssetDetailActivity> assetsActivityProvider;
    private final Provider<MapActivity> mapActivityProvider;
    private final Provider<FlowsActivity> flowsActivityProvider;

    @Inject
    public MainContentActivityMapper(Provider<AssetDetailActivity> assetsActivityProvider,
                                     Provider<MapActivity> mapActivityProvider,
                                     Provider<FlowsActivity> flowsActivityProvider) {
        this.assetsActivityProvider = assetsActivityProvider;
        this.mapActivityProvider = mapActivityProvider;
        this.flowsActivityProvider = flowsActivityProvider;
    }

    public AppActivity getActivity(Place place) {
        if (place instanceof AssetsPlace) {
            return assetsActivityProvider.get().init((AssetsPlace) place);
        }
        if (place instanceof MapPlace) {
            return mapActivityProvider.get().init((MapPlace) place);
        }
        if (place instanceof FlowsPlace) {
            return flowsActivityProvider.get().init((FlowsPlace) place);
        }
        return null;
    }
}
