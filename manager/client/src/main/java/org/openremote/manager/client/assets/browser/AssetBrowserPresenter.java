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
import com.google.gwt.view.client.HasData;
import org.openremote.manager.client.assets.AssetInfoArrayMapper;
import org.openremote.manager.client.assets.event.AssetsModifiedEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventListener;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.shared.asset.AssetInfo;
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
    final protected AssetInfoArrayMapper assetInfoArrayMapper;

    protected String selectedAssetId;
    protected String[] selectedAssetPath;

    @Inject
    public AssetBrowserPresenter(EventBus eventBus,
                                 ManagerMessages managerMessages,
                                 RequestService requestService,
                                 PlaceController placeController,
                                 AssetBrowser view,
                                 AssetResource assetResource,
                                 AssetInfoArrayMapper assetInfoArrayMapper) {
        this.eventBus = eventBus;
        this.managerMessages = managerMessages;
        this.requestService = requestService;
        this.placeController = placeController;
        this.internalEventBus = new EventBus();
        this.view = view;
        this.assetResource = assetResource;
        this.assetInfoArrayMapper = assetInfoArrayMapper;

        eventBus.register(AssetsModifiedEvent.class, event -> {
            view.refreshAssets(
                event.getAsset() == null || event.getAsset().getParentId() == null
            );
        });

        view.setPresenter(this);
    }

    @Override
    public AssetBrowser getView() {
        return view;
    }

    @Override
    public void loadAssetChildren(AssetInfo parent, HasData<AssetInfo> display) {
        // If parent is the invisible root of the tree, show a loading message
        if (parent.getId() == null) {
            display.setRowData(0, Collections.singletonList(
                new AssetInfo(managerMessages.loadingAssets(), AssetTreeModel.TEMPORARY_ASSET_TYPE)
            ));
            display.setRowCount(1, true);
        }

        // TODO Pagination?
        // final Range range = display.getVisibleRange();
        requestService.execute(
            assetInfoArrayMapper,
            requestParams -> {
                if (parent.getId() == null) {
                    // ROOT of tree
                    assetResource.getRoot(requestParams);
                } else {
                    assetResource.getChildren(requestParams, parent.getId());
                }
            },
            200,
            assetInfos -> {
                display.setRowData(0, Arrays.asList(assetInfos));
                display.setRowCount(assetInfos.length, true);
                onAssetsRefreshed(parent, assetInfos);
            },
            ex -> handleRequestException(ex, eventBus, managerMessages)
        );
    }

    @Override
    public void onAssetSelected(String assetId) {
        if (assetId == null) {
            // Reset the selected asset
            selectedAssetId = null;
            internalEventBus.dispatch(new AssetSelectedEvent(null));
        } else if (selectedAssetId == null || !selectedAssetId.equals(assetId)) {
            // If there was no previous selection, or the new asset is a different one, set and fire event
            selectedAssetId = assetId;
            internalEventBus.dispatch(new AssetSelectedEvent(selectedAssetId));
        }
    }

    @Override
    public void selectAsset(String assetId, String[] path) {
        onAssetSelected(assetId);
        selectedAssetPath = path;
        if (selectedAssetId != null) {
            updateViewSelection(true);
        } else {
            view.deselectAssets();
        }
    }

    @Override
    public void deselectAsset() {
        selectAsset(null, null);
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
        if (selectedAssetId != null && selectedAssetPath != null) {
            view.showAndSelectAsset(selectedAssetPath, selectedAssetId, scrollIntoView);
        }
    }

    protected void onAssetsRefreshed(AssetInfo parent, AssetInfo[] children) {
        if (selectedAssetId != null) {
            // Only scroll the view if the selected asset was loaded
            boolean scroll = false;
            for (AssetInfo childAssetInfo : children) {
                if (childAssetInfo.getId().equals(selectedAssetId))
                    scroll = true;
            }
            updateViewSelection(scroll);
        }
    }
}
