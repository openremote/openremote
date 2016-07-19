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
package org.openremote.manager.client.assets;

import com.google.gwt.place.shared.PlaceController;
import org.openremote.manager.client.assets.asset.Asset;
import org.openremote.manager.client.assets.asset.AssetPlace;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowsingActivity;
import org.openremote.manager.client.event.bus.EventBus;

import javax.inject.Inject;

public class AssetsDashboardActivity
    extends AssetBrowsingActivity<AssetsDashboard, AssetsDashboardPlace>
    implements AssetsDashboard.Presenter {

    final PlaceController placeController;
    final EventBus eventBus;

    @Inject
    public AssetsDashboardActivity(AssetsDashboard view,
                                   AssetBrowser.Presenter assetBrowserPresenter,
                                   PlaceController placeController,
                                   EventBus eventBus) {
        super(view, assetBrowserPresenter);
        this.placeController = placeController;
        this.eventBus = eventBus;
    }

    @Override
    protected void startCreateAsset() {
        assetBrowserPresenter.selectAsset(null);
    }

    @Override
    protected void onAssetReady() {
    }

    @Override
    protected void onAssetsDeselected() {
    }

    @Override
    protected void onAssetSelectionChange(Asset newSelection) {
        placeController.goTo(new AssetPlace(newSelection.getId()));
    }
}
