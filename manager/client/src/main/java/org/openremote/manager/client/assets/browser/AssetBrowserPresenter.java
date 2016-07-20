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
import org.openremote.manager.client.assets.SampleAssets;
import org.openremote.manager.client.assets.asset.Asset;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventListener;
import org.openremote.manager.client.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.List;
import java.util.logging.Logger;

public class AssetBrowserPresenter implements AssetBrowser.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetBrowserPresenter.class.getName());

    final EventBus internalEventBus;
    final AssetBrowser view;
    final PlaceController placeController;

    protected Asset selectedAsset;

    @Inject
    public AssetBrowserPresenter(AssetBrowser view, PlaceController placeController) {
        this.internalEventBus = new EventBus();
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
        List<Asset> result = SampleAssets.queryAll(parent);
        Timer t = new Timer() {
            public void run() {
                display.setRowData(0, result);
                display.setRowCount(result.size(), true);
                onAssetsRefreshed(parent, result);
            }
        };
        // TODO: Simulates network delay
        t.schedule(25);
        //t.run();
    }

    @Override
    public void onAssetSelected(Asset asset) {
        if (asset == null) {
            selectedAsset = null;
            internalEventBus.dispatch(new AssetSelectedEvent(null));
        } else if (selectedAsset == null || !selectedAsset.getId().equals(asset.getId())) {
            selectedAsset = asset;
            internalEventBus.dispatch(new AssetSelectedEvent(selectedAsset));
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
        return internalEventBus.register(AssetSelectedEvent.class, listener);
    }

    @Override
    public void removeRegistration(EventRegistration registration) {
        internalEventBus.remove(registration);
    }

    protected void updateViewSelection(boolean scrollIntoView) {
        // Find the last selected asset after a data refresh and select it again
        if (selectedAsset != null) {
            view.showAndSelectAsset(
                SampleAssets.getSelectedAssetPath(selectedAsset.getId()),
                selectedAsset.getId(),
                scrollIntoView
            );
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
}
