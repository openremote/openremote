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
package org.openremote.manager.client.assets.asset;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.assets.AssetArrayMapper;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.AssetsDashboardPlace;
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.assets.browser.AssetBrowsingActivity;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetResource;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class AssetActivity
    extends AssetBrowsingActivity<AssetView, AssetPlace>
    implements AssetView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetActivity.class.getName());

    final PlaceController placeController;

    @Inject
    public AssetActivity(EventBus eventBus,
                         ManagerMessages managerMessages,
                         RequestService requestService,
                         PlaceController placeController,
                         AssetView view,
                         AssetBrowser.Presenter assetBrowserPresenter,
                         AssetResource assetResource,
                         AssetArrayMapper assetArrayMapper,
                         AssetMapper assetMapper) {
        super(eventBus, managerMessages, requestService, view, assetBrowserPresenter, assetResource, assetArrayMapper, assetMapper);
        this.placeController = placeController;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        super.start(container, eventBus, registrations);
    }

    @Override
    protected void startCreateAsset() {
        super.startCreateAsset();

        asset = new Asset();
        asset.setName("My New Asset");
        onAssetReady();
    }

    @Override
    protected void onAssetReady() {
        view.setName(asset.getName());
        // TODO Write data
    }

    @Override
    protected void onAssetsDeselected() {
        placeController.goTo(new AssetsDashboardPlace());
    }

    @Override
    protected void onAssetSelectionChange(Asset newSelection) {
        placeController.goTo(new AssetPlace(newSelection.getId()));
    }

    @Override
    protected void onBeforeAssetLoad() {
        view.setFormBusy(true);
    }
}
