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
import org.openremote.manager.client.assets.asset.AssetPlace;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowsingActivity;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetResource;

import javax.inject.Inject;

public class AssetsDashboardActivity
    extends AssetBrowsingActivity<AssetsDashboard, AssetsDashboardPlace>
    implements AssetsDashboard.Presenter {

    final PlaceController placeController;
    final EventBus eventBus;

    @Inject
    public AssetsDashboardActivity(EventBus eventBus,
                                   ManagerMessages managerMessages,
                                   RequestService requestService,
                                   PlaceController placeController,
                                   AssetsDashboard view,
                                   AssetBrowser.Presenter assetBrowserPresenter,
                                   AssetResource assetResource,
                                   AssetMapper assetMapper) {
        super(eventBus, managerMessages, requestService, view, assetBrowserPresenter, assetResource, assetMapper);
        this.placeController = placeController;
        this.eventBus = eventBus;
    }

    @Override
    protected void onAssetLoaded() {
    }

    @Override
    protected void onAssetsDeselected() {
    }

    @Override
    protected void onAssetSelectionChange(String selectedAssetId) {
        placeController.goTo(new AssetPlace(selectedAssetId));
    }
}
