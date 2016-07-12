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
import com.google.gwt.user.client.Timer;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import org.openremote.manager.client.assets.asset.Asset;
import org.openremote.manager.client.assets.asset.AssetPlace;
import org.openremote.manager.client.event.bus.EventBus;

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

    String selectedAssetId;

    @Inject
    public AssetBrowserPresenter(AssetBrowser view,
                                 PlaceController placeController,
                                 EventBus eventBus) {
        this.view = view;
        this.placeController = placeController;
        this.eventBus = eventBus;

        view.setPresenter(this);

        this.eventBus.register(AssetsRefreshedEvent.class, event -> {
            // Find the last selected asset after a data refresh and select it again
            if (selectedAssetId != null) {
                // TODO: We must build the asset path here, by finding the asset first, then its parents recursively
                List<String> path = new ArrayList<>();
                switch (selectedAssetId) {
                    case "composite:gateways":
                        path.add("composite:gateways");
                        break;
                    case "composite:buildings":
                        path.add("composite:buildings");
                        break;
                    case "composite:rooms":
                        path.add("composite:rooms");
                        break;
                    case "composite:thermostats":
                        path.add("composite:thermostats");
                        break;
                    case "0":
                        path.add("composite:gateways");
                        path.add("0");
                        break;
                    case "1":
                        path.add("composite:gateways");
                        path.add("1");
                        break;
                    case "2":
                        path.add("composite:gateways");
                        path.add("2");
                        break;
                    case "3":
                        path.add("composite:gateways");
                        path.add("3");
                        break;
                    case "11":
                        path.add("composite:gateways");
                        path.add("1");
                        path.add("11");
                        break;
                    case "22":
                        path.add("composite:gateways");
                        path.add("1");
                        path.add("22");
                        break;
                    case "33":
                        path.add("composite:gateways");
                        path.add("1");
                        path.add("33");
                        break;
                }
                view.showAndSelectAsset(path, selectedAssetId);
            }
        });
    }

    @Override
    public AssetBrowser getView() {
        return view;
    }

    @Override
    public void loadAssetChildren(Asset parent, HasData<Asset> display) {
        final Range range = display.getVisibleRange();
        // TODO: Real assets query
        Timer t = null;
        if (Asset.isRoot(parent)) {
            t = new Timer() {
                public void run() {
                    display.setRowData(0, Arrays.asList(
                        new Asset("composite:gateways", Asset.Type.COMPOSITE.name(), "Gateways", null),
                        new Asset("composite:buildings", Asset.Type.COMPOSITE.name(), "Buildings", null),
                        new Asset("composite:rooms", Asset.Type.COMPOSITE.name(), "Rooms", null),
                        new Asset("composite:thermostats", Asset.Type.COMPOSITE.name(), "Thermostats", null)
                    ));
                    eventBus.dispatch(new AssetsRefreshedEvent(parent, display.getRowCount()));
                }
            };
        } else if (parent.getType().equals(Asset.Type.COMPOSITE.name())) {
            if (parent.getId().equals("composite:gateways")) {
                t = new Timer() {
                    public void run() {
                        List<Asset> list = new ArrayList<>();
                        for (int i = 0; i < 100; i++) {
                            list.add(
                                new Asset(Integer.toString(i), Asset.Type.COMPOSITE.name(), "Gateway " + i, "123.123")
                            );
                        }
                        display.setRowData(0, list);
                        eventBus.dispatch(new AssetsRefreshedEvent(parent, display.getRowCount()));
                    }
                };
            }

            if (parent.getId().equals("1")) {
                t = new Timer() {
                    public void run() {
                        List<Asset> assets = Arrays.asList(
                            new Asset("11", Asset.Type.SENSOR.name(), "Sensor 1", "123.123"),
                            new Asset("22", Asset.Type.SENSOR.name(), "Sensor 2", "123.123"),
                            new Asset("33", Asset.Type.SENSOR.name(), "Sensor 3", "123.123")
                        );
                        display.setRowData(0, assets);
                        eventBus.dispatch(new AssetsRefreshedEvent(parent, display.getRowCount()));
                    }
                };
            }
        }

        if (t != null) {

            // TODO: Simulates network delay
            t.schedule(25);
            //t.run();

        } else {
            display.setRowData(0, new ArrayList<>());
            display.setRowCount(0, true);
            eventBus.dispatch(new AssetsRefreshedEvent(parent, display.getRowCount()));
        }
    }

    @Override
    public void onAssetSelected(Asset asset) {
        if (!asset.getId().equals(selectedAssetId)) {
            selectAsset(asset.getId());
            placeController.goTo(new AssetPlace(asset.getId()));
        }
    }

    @Override
    public void selectAsset(String id) {
        this.selectedAssetId = id;
    }
}
