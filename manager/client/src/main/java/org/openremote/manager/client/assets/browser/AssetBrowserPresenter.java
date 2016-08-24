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
import org.openremote.manager.client.assets.AssetArrayMapper;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventListener;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.asset.Asset;
import org.openremote.manager.shared.asset.AssetResource;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetBrowserPresenter implements AssetBrowser.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetBrowserPresenter.class.getName());

    final protected EventBus eventBus;
    final protected ManagerMessages managerMessages;
    final protected RequestService requestService;
    final PlaceController placeController;
    final EventBus internalEventBus;
    final AssetBrowser view;
    final protected AssetResource assetResource;
    final protected AssetArrayMapper assetArrayMapper;

    protected Asset selectedAsset;

    @Inject
    public AssetBrowserPresenter(EventBus eventBus,
                                 ManagerMessages managerMessages,
                                 RequestService requestService,
                                 PlaceController placeController,
                                 AssetBrowser view,
                                 AssetResource assetResource,
                                 AssetArrayMapper assetArrayMapper) {
        this.eventBus = eventBus;
        this.managerMessages = managerMessages;
        this.requestService = requestService;
        this.placeController = placeController;
        this.internalEventBus = new EventBus();
        this.view = view;
        this.assetResource = assetResource;
        this.assetArrayMapper = assetArrayMapper;

        view.setPresenter(this);
    }

    @Override
    public AssetBrowser getView() {
        return view;
    }

    @Override
    public void loadAssetChildren(Asset parent, HasData<Asset> display) {
        // If parent is the invisible root of the tree, show a loading message
        if (parent.getId() == null) {
            display.setRowData(0, Collections.singletonList(
                new Asset(managerMessages.loadingAssets(), AssetTreeModel.TEMPORARY_ASSET_TYPE)
            ));
            display.setRowCount(1, true);
        }

        // TODO Pagination?
        // final Range range = display.getVisibleRange();
        requestService.execute(
            assetArrayMapper,
            requestParams -> {
                if (parent.getId() == null) {
                    // ROOT of tree
                    assetResource.getRoot(requestParams);
                } else {
                    assetResource.getChildren(requestParams, parent.getId());
                }
            },
            200,
            assets -> {
                display.setRowData(0, Arrays.asList(assets));
                display.setRowCount(assets.length, true);
                onAssetsRefreshed(parent, assets);
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    @Override
    public void onAssetSelected(Asset asset) {
        // This is a complicated method. It may be called when the user selects an asset in the
        // tree. It may also be called when an asset has been loaded from the server on page load.

        if (asset == null) {
            // Reset the selected asset
            selectedAsset = null;
            internalEventBus.dispatch(new AssetSelectedEvent(null));
        } else if (asset.getId() != null) {
            // The selected asset was not the invisible root or the loading message
            if (selectedAsset == null || !selectedAsset.getId().equals(asset.getId())) {
                // If there was no previous selection, or the new asset is a different one, set and fire event
                selectedAsset = asset;
                internalEventBus.dispatch(new AssetSelectedEvent(selectedAsset));
            } else if (selectedAsset.getId().equals(asset.getId())) {
                // If there was a selection and the new asset is the "same", override the current selection
                // with the given instance. It might have a path property value, wheres the current one might not.
                // When the user is browsing the tree and assets are expanded, their children are loaded without
                // the path value (this would be expensive to calculate on the server). Only when the user selects
                // an asset, or an asset is loaded on page load, do we get the asset path from the server.
                selectedAsset = asset;
            }
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
        if (selectedAsset != null && selectedAsset.getPath() != null) {
            view.showAndSelectAsset(
                selectedAsset.getPath(),
                selectedAsset.getId(),
                scrollIntoView
            );
        }
    }

    protected void onAssetsRefreshed(Asset parent, Asset[] children) {
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
