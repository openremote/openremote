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
import org.openremote.manager.client.assets.browser.AssetBrowser;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.service.RequestService;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class AssetActivity
    extends AppActivity<AssetPlace>
    implements AssetView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetActivity.class.getName());

    final AssetView view;
    final AssetBrowser.Presenter assetBrowserPresenter;
    final PlaceController placeController;
    final EventBus eventBus;
    final RequestService requestService;

    protected String assetId;
    protected Asset asset;

    @Inject
    public AssetActivity(AssetView view,
                         AssetBrowser.Presenter assetBrowserPresenter,
                         PlaceController placeController,
                         EventBus eventBus,
                         RequestService requestService) {
        this.view = view;
        this.assetBrowserPresenter = assetBrowserPresenter;
        this.placeController = placeController;
        this.eventBus = eventBus;
        this.requestService = requestService;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:assets"};
    }

    @Override
    protected AppActivity<AssetPlace> init(AssetPlace place) {
        this.assetId = place.getAssetId();
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        if (assetId != null) {
            loadAsset();
        } else {
            asset = new Asset();
            asset.setDisplayName("My New Asset");
            writeToView();
        }
    }

    @Override
    public AssetBrowser getAssetBrowser() {
        return assetBrowserPresenter.getView();
    }

    protected void loadAsset() {
        view.setFormBusy(true);

        LOG.info("### TODO: Load Asset");
        this.asset = new Asset(assetId, "DUMMY", "DUMMY FOR ID " + assetId, null);
        writeToView();

        /* TODO query
        requestService.execute(
            assetMapper,
            requestParams -> assetResource.get(requestParams, assetId),
            200,
            user -> {
                this.user = user;
                this.realm = user.getRealm();
                loadRoles(() -> {
                    writeToView();
                    view.setFormBusy(false);
                    view.enableCreate(false);
                    view.enableUpdate(true);
                    view.enableDelete(true);
                });
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
        */
    }

    protected void writeToView() {
        assetBrowserPresenter.selectAsset(assetId);
        view.setDisplayName(asset.getDisplayName());
        // TODO Write data
    }

}
