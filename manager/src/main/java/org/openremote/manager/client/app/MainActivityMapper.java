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
import org.openremote.manager.client.user.UserAccountActivity;
import org.openremote.manager.client.user.UserAccountPlace;

import java.util.logging.Logger;

public class MainActivityMapper implements AppActivityMapper {

    private static final Logger LOG = Logger.getLogger(MainActivityMapper.class.getName());

    protected final Provider<AssetDetailActivity> assetsActivityProvider;
    protected final Provider<MapActivity> mapActivityProvider;
    protected final Provider<FlowsActivity> flowsActivityProvider;
    protected final Provider<UserAccountActivity> userProfileActivityProvider;

    @Inject
    public MainActivityMapper(Provider<AssetDetailActivity> assetsActivityProvider,
                              Provider<MapActivity> mapActivityProvider,
                              Provider<FlowsActivity> flowsActivityProvider,
                              Provider<UserAccountActivity> userProfileActivityProvider) {
        this.assetsActivityProvider = assetsActivityProvider;
        this.mapActivityProvider = mapActivityProvider;
        this.flowsActivityProvider = flowsActivityProvider;
        this.userProfileActivityProvider = userProfileActivityProvider;
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
        if (place instanceof UserAccountPlace) {
            return userProfileActivityProvider.get().init((UserAccountPlace)place);
        }
        return null;
    }
}
