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

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.assets.AssetArrayMapper;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetResource;

import java.util.Collection;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

abstract public class AssetBrowsingActivity<V extends AssetBrowsingView, T extends AssetBrowsingPlace>
    extends AppActivity<T>
    implements AssetBrowsingView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetBrowsingActivity.class.getName());

    final protected EventBus eventBus;
    final protected ManagerMessages managerMessages;
    final protected RequestService requestService;
    final protected V view;
    final protected AssetBrowser.Presenter assetBrowserPresenter;
    final protected AssetResource assetResource;
    final protected AssetArrayMapper assetArrayMapper;
    final protected AssetMapper assetMapper;

    protected EventRegistration<AssetSelectedEvent> assetSelectionRegistration;
    protected String assetId;
    protected Asset asset;

    public AssetBrowsingActivity(EventBus eventBus,
                                 ManagerMessages managerMessages,
                                 RequestService requestService,
                                 V view,
                                 AssetBrowser.Presenter assetBrowserPresenter,
                                 AssetResource assetResource,
                                 AssetArrayMapper assetArrayMapper,
                                 AssetMapper assetMapper) {
        this.eventBus = eventBus;
        this.managerMessages = managerMessages;
        this.requestService = requestService;
        this.view = view;
        this.assetBrowserPresenter = assetBrowserPresenter;
        this.assetResource = assetResource;
        this.assetArrayMapper = assetArrayMapper;
        this.assetMapper = assetMapper;
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
        requestService.execute(
            assetMapper,
            requestParams -> assetResource.get(requestParams, assetId),
            200,
            asset -> {
                this.asset = asset;
                assetBrowserPresenter.selectAsset(asset);
                onAssetReady();
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    /**
     * Noop by default
     */
    protected void onBeforeAssetLoad() {

    };

    abstract protected void onAssetReady();

    abstract protected void onAssetsDeselected();

    abstract protected void onAssetSelectionChange(Asset newSelection);

    protected void startCreateAsset() {
        assetBrowserPresenter.selectAsset(null);
    }

}
