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
package org.openremote.manager.client.assets.browser;

import com.google.gwt.place.shared.PlaceController;
import org.openremote.manager.client.assets.Asset;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.shared.Consumer;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class AssetBrowserPresenter implements AssetBrowser.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetBrowserPresenter.class.getName());

    final AssetBrowser view;
    final PlaceController placeController;
    final EventBus eventBus;

    @Inject
    public AssetBrowserPresenter(AssetBrowser view,
                                 PlaceController placeController,
                                 EventBus eventBus) {
        this.view = view;
        this.placeController = placeController;
        this.eventBus = eventBus;

        view.setPresenter(this);
    }

    @Override
    public AssetBrowser getView() {
        return view;
    }

    @Override
    public void loadAssetChildren(Asset parent, Consumer<List<Asset>> consumer) {
        if (Asset.isRoot(parent)) {
            consumer.accept(Arrays.asList(
                new Asset("composite:gateways", Asset.Type.COMPOSITE.name(), "Gateways", null),
                new Asset("composite:buildings", Asset.Type.COMPOSITE.name(), "Buildings", null),
                new Asset("composite:rooms", Asset.Type.COMPOSITE.name(), "Rooms", null),
                new Asset("composite:thermostats", Asset.Type.COMPOSITE.name(), "Thermostats", null)
            ));
            return;
        } else if (parent.getType().equals(Asset.Type.COMPOSITE.name())) {
            if (parent.getId().equals("composite:gateways")) {
                consumer.accept(Arrays.asList(
                    new Asset("1", Asset.Type.COMPOSITE.name(), "Gateway A", "123.123"),
                    new Asset("2", Asset.Type.COMPOSITE.name(), "Gateway B", "123.123"),
                    new Asset("3", Asset.Type.COMPOSITE.name(), "Gateway C", "123.123"),
                    new Asset("4", Asset.Type.COMPOSITE.name(), "Gateway D", "123.123"),
                    new Asset("5", Asset.Type.COMPOSITE.name(), "Gateway E", "123.123")
                ));
                return;
            }

            if (parent.getId().equals("1")) {
                consumer.accept(Arrays.asList(
                    new Asset("11", Asset.Type.SENSOR.name(), "Sensor 1", "123.123"),
                    new Asset("22", Asset.Type.SENSOR.name(), "Sensor 2", "123.123"),
                    new Asset("33", Asset.Type.SENSOR.name(), "Sensor 3", "123.123")
                ));
                return;
            }
        }
        consumer.accept(new ArrayList<>());
    }

    @Override
    public void onAssetSelected(Asset asset) {
        view.setSelectedAsset(asset.getId());
    }
}
