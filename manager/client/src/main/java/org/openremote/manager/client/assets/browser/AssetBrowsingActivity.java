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

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.assets.asset.Asset;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.interop.mapbox.LngLat;
import org.openremote.manager.client.mvp.AppActivity;

import java.util.Collection;
import java.util.logging.Logger;

abstract public class AssetBrowsingActivity<V extends AssetBrowsingView, T extends AssetBrowsingPlace>
    extends AppActivity<T>
    implements AssetBrowsingView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetBrowsingActivity.class.getName());

    final protected V view;
    final protected AssetBrowser.Presenter assetBrowserPresenter;

    protected EventRegistration<AssetSelectedEvent> assetSelectionRegistration;
    protected String assetId;
    protected Asset asset;

    public AssetBrowsingActivity(V view, AssetBrowser.Presenter assetBrowserPresenter) {
        this.view = view;
        this.assetBrowserPresenter = assetBrowserPresenter;
    }

    public V getView() {
        return view;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:assets"};
    }

    @Override
    protected AppActivity<T> init(T place) {
        this.assetId = place.getAssetId();
        return this;
    }


    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        //noinspection unchecked
        view.setPresenter(this);

        container.setWidget(view.asWidget());

        assetSelectionRegistration = assetBrowserPresenter.onSelection(
            event -> {
                if (event.getAsset() == null) {
                    onAssetsDeselected();
                } else {
                    if (assetId == null || !assetId.equals(event.getAsset().getId())) {
                        onAssetSelectionChange(event.getAsset());
                    }
                }

                // Put on global event bus so interested 3rd party can get state (e.g. header presenter)
                eventBus.dispatch(event);
            }
        );

        asset = null;
        if (assetId != null) {
            loadAsset();
        } else {
            startCreateAsset();
        }
    }

    public String getAssetId() {
        return assetId;
    }

    public Asset getAsset() {
        return asset;
    }

    @Override
    public void onStop() {
        if (assetSelectionRegistration != null) {
            assetBrowserPresenter.removeRegistration(assetSelectionRegistration);
        }
        super.onStop();
    }

    protected void loadAsset() {
        onBeforeAssetLoad();
        new Timer() {
            public void run() {
                asset = new Asset(assetId, "DUMMY", "DUMMY FOR ID " + assetId, new LngLat(5.460315214821094, 51.44541688237109));
                assetBrowserPresenter.selectAsset(asset);
                onAssetReady();
            }
        }.schedule(100);

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
                });
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
        */
    }

    /**
     * Noop by default
     */
    protected void onBeforeAssetLoad() {

    };

    abstract protected void onAssetReady();

    abstract protected void onAssetsDeselected();

    abstract protected void onAssetSelectionChange(Asset newSelection);

    /**
     * Noop by default
     */
    protected void startCreateAsset() {
    }

}
