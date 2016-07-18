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
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventListener;
import org.openremote.manager.client.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class AssetBrowserPresenter implements AssetBrowser.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetBrowserPresenter.class.getName());

    final EventBus internalEventbus;
    final AssetBrowser view;
    final PlaceController placeController;

    protected Asset selectedAsset;

    @Inject
    public AssetBrowserPresenter(AssetBrowser view,
                                 PlaceController placeController) {

        this.internalEventbus = new EventBus();

        this.view = view;
        this.placeController = placeController;

        view.setPresenter(this);
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
                    List<Asset> assets = Arrays.asList(
                        new Asset("composite:gateways", Asset.Type.COMPOSITE.name(), "Gateways", null),
                        new Asset("composite:buildings", Asset.Type.COMPOSITE.name(), "Buildings", null),
                        new Asset("composite:rooms", Asset.Type.COMPOSITE.name(), "Rooms", null),
                        new Asset("composite:thermostats", Asset.Type.COMPOSITE.name(), "Thermostats", null)
                    );
                    display.setRowData(0, assets);
                    onAssetsRefreshed(parent, assets);
                }
            };
        } else if (parent.getType().equals(Asset.Type.COMPOSITE.name())) {
            if (parent.getId().equals("composite:gateways")) {
                t = new Timer() {
                    public void run() {
                        List<Asset> assets = new ArrayList<>();
                        for (int i = 1000; i < 1100; i++) {
                            assets.add(
                                new Asset(Integer.toString(i), Asset.Type.COMPOSITE.name(), "Gateway " + i, "123.123")
                            );
                        }
                        display.setRowData(0, assets);
                        onAssetsRefreshed(parent, assets);
                    }
                };
            }

            if (parent.getId().equals("1000")) {
                t = new Timer() {
                    public void run() {
                        List<Asset> assets = Arrays.asList(
                            new Asset("11", Asset.Type.SENSOR.name(), "Sensor 1", "123.123"),
                            new Asset("22", Asset.Type.SENSOR.name(), "Sensor 2", "123.123"),
                            new Asset("33", Asset.Type.SENSOR.name(), "Sensor 3", "123.123")
                        );
                        display.setRowData(0, assets);
                        onAssetsRefreshed(parent, assets);
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
            onAssetsRefreshed(parent, new ArrayList<>());
        }
    }

    @Override
    public void onAssetSelected(Asset asset) {
        if (asset == null) {
            selectedAsset = null;
            internalEventbus.dispatch(new AssetSelectedEvent(null));
        } else if (selectedAsset == null || !selectedAsset.getId().equals(asset.getId())) {
            selectedAsset = asset;
            internalEventbus.dispatch(new AssetSelectedEvent(selectedAsset));
        }
    }

    @Override
    public void selectAsset(Asset asset) {
        onAssetSelected(asset);
        if (selectedAsset != null) {
            updateViewSelection(true);
        } else {
            view.deselectAssets();
        }
    }

    @Override
    public EventRegistration<AssetSelectedEvent> onSelection(EventListener<AssetSelectedEvent> listener) {
        return internalEventbus.register(AssetSelectedEvent.class, listener);
    }

    @Override
    public void removeRegistration(EventRegistration registration) {
        internalEventbus.remove(registration);
    }

    protected void updateViewSelection(boolean scrollIntoView) {
        // Find the last selected asset after a data refresh and select it again
        if (selectedAsset != null) {
            view.showAndSelectAsset(getSelectedAssetPath(), selectedAsset.getId(), scrollIntoView);
        }
    }

    protected void onAssetsRefreshed(Asset parent, List<Asset> children) {
        if (selectedAsset != null) {
            // Only scroll the view if the selected asset was loaded
            boolean scroll = false;
            for (Asset childAsset : children) {
                if (childAsset.getId().equals(selectedAsset.getId()))
                    scroll = true;
            }
            updateViewSelection(scroll);
        }
    }

    protected List<String> getSelectedAssetPath() {
        List<String> path = new ArrayList<>();
        // TODO: We must build the asset path here, by finding the asset first, then its parents recursively
        switch (selectedAsset.getId()) {
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
            case "11":
                path.add("composite:gateways");
                path.add("1000");
                path.add("11");
                break;
            case "22":
                path.add("composite:gateways");
                path.add("1000");
                path.add("22");
                break;
            case "33":
                path.add("composite:gateways");
                path.add("1000");
                path.add("33");
                break;
            default:
                path.add("composite:gateways");
                path.add(selectedAsset.getId());
                break;
        }
        return path;
    }
}
