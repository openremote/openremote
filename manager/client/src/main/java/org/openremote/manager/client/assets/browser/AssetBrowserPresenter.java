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

import com.google.gwt.view.client.HasData;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.admin.TenantArrayMapper;
import org.openremote.manager.client.assets.AssetInfoArrayMapper;
import org.openremote.manager.client.event.CancelRepeatingServerSendEvent;
import org.openremote.manager.client.event.RepeatingServerSendEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventListener;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.shared.Constants;
import org.openremote.manager.shared.asset.*;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetInfo;
import org.openremote.model.asset.AssetType;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetBrowserPresenter implements AssetBrowser.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetBrowserPresenter.class.getName());

    final protected Environment environment;
    final EventBus internalEventBus;
    final AssetBrowser view;
    final protected AssetResource assetResource;
    final protected AssetInfoArrayMapper assetInfoArrayMapper;
    final protected TenantResource tenantResource;
    final protected TenantArrayMapper tenantArrayMapper;

    protected List<AssetInfo> tenantAssetInfos = new ArrayList<>();
    protected String selectedAssetId;
    protected String[] selectedAssetPath;

    @Inject
    public AssetBrowserPresenter(Environment environment,
                                 AssetBrowser view,
                                 AssetResource assetResource,
                                 AssetInfoArrayMapper assetInfoArrayMapper,
                                 TenantResource tenantResource,
                                 TenantArrayMapper tenantArrayMapper) {
        this.environment = environment;
        this.tenantResource = tenantResource;
        this.tenantArrayMapper = tenantArrayMapper;
        this.internalEventBus = new EventBus();
        this.view = view;
        this.assetResource = assetResource;
        this.assetInfoArrayMapper = assetInfoArrayMapper;

        environment.getEventBus().register(AssetModifiedEvent.class, event -> {
            LOG.fine("Asset modified event received: " + event);
            if (event.getCause() == AssetModifiedEvent.Cause.CHILDREN_MODIFIED) {

                boolean forceRootRefresh = event.getAssetInfo().getParentId() == null;

                // If we are in the master realm, we never refresh the root of
                // the asset tree, these are "immutable" tenants
                // TODO: Notifications when tenants are modified?
                String currentRealm = environment.getSecurityService().getRealm();
                if (currentRealm.equals(Constants.MASTER_REALM)) {
                    forceRootRefresh = false;
                }

                view.refreshAssets(forceRootRefresh);
            }
        });

        view.setPresenter(this);
    }

    @Override
    public AssetBrowser getView() {
        return view;
    }

    @Override
    public void onViewAttached() {
        LOG.fine("Asset browser attached, subscribing to asset changes on the server");
        environment.getEventBus().dispatch(new RepeatingServerSendEvent(
            new SubscribeAssetModified(),
            AssetBrowserPresenter.class.getSimpleName(),
            30000
        ));
    }

    @Override
    public void onViewDetached() {
        LOG.fine("Asset browser detached, unsubscribing from asset changes on the server");
        environment.getEventBus().dispatch(new CancelRepeatingServerSendEvent(
            new UnsubscribeAssetModified(),
            AssetBrowserPresenter.class.getSimpleName()
        ));
    }

    @Override
    public void loadAssetChildren(AssetInfo parent, HasData<AssetInfo> display) {
        // If parent is the invisible root of the tree, show a loading message
        if (parent.getId() == null) {
            showLoadingMessage(display);
        }

        // If the parent is the invisible root of the tree and we are in the master realm, show all realms
        String currentRealm = environment.getSecurityService().getRealm();
        if (parent.getId() == null && currentRealm.equals(Constants.MASTER_REALM)) {
            loadTenants(parent, display);
        } else {
            loadChildren(parent, display);
        }
    }

    @Override
    public void onAssetSelected(AssetInfo assetInfo) {
        if (assetInfo == null) {
            // Reset the selected asset
            selectedAssetId = null;
            internalEventBus.dispatch(new AssetSelectedEvent(null));
        } else if (selectedAssetId == null || !selectedAssetId.equals(assetInfo.getId())) {
            // If there was no previous selection, or the new asset is a different one, set and fire event
            selectedAssetId = assetInfo.getId();
            internalEventBus.dispatch(new AssetSelectedEvent(assetInfo));
        }
    }

    @Override
    public void selectAsset(Asset asset) {
        onAssetSelected(asset != null ? new AssetInfo(asset) : null);
        selectedAssetPath = asset != null ? getTenantAdjustedAssetPath(asset) : null;
        if (selectedAssetId != null) {
            updateViewSelection(true);
        } else {
            view.deselectAssets();
        }
    }

    @Override
    public void deselectAsset() {
        selectAsset(null);
    }

    @Override
    public EventRegistration<AssetSelectedEvent> onSelection(EventListener<AssetSelectedEvent> listener) {
        return internalEventBus.register(AssetSelectedEvent.class, listener);
    }

    @Override
    public void removeRegistration(EventRegistration registration) {
        internalEventBus.remove(registration);
    }

    protected void showLoadingMessage(HasData<AssetInfo> display) {
        display.setRowData(0, Collections.singletonList(
            new AssetInfo(
                environment.getMessages().loadingAssets(),
                AssetTreeModel.TEMPORARY_ASSET_TYPE
            )
        ));
        display.setRowCount(1, true);
    }

    protected void loadTenants(AssetInfo parent, HasData<AssetInfo> display) {
        environment.getRequestService().execute(
            tenantArrayMapper,
            requestParams -> {
                // This must be async, so tree selection/searching works
                requestParams.setAsync(false);
                tenantResource.getAll(requestParams);
            },
            200,
            tenants -> {
                tenantAssetInfos.clear();
                for (Tenant tenant : tenants) {
                    tenantAssetInfos.add(new AssetInfo(
                        tenant.getId(),
                        0,
                        tenant.getDisplayName(),
                        tenant.getRealm(),
                        AssetType.TENANT.getValue(),
                        null,
                        null
                    ));
                }
                display.setRowData(0, tenantAssetInfos);
                display.setRowCount(tenantAssetInfos.size(), true);
                onAssetsRefreshed(parent, tenantAssetInfos.toArray(new AssetInfo[tenantAssetInfos.size()]));
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void loadChildren(AssetInfo parent, HasData<AssetInfo> display) {
        String realm = parent.getId() != null ? parent.getRealm() : environment.getSecurityService().getRealm();

        // TODO Pagination?
        // final Range range = display.getVisibleRange();
        environment.getRequestService().execute(
            assetInfoArrayMapper,
            requestParams -> {
                // This must be async, so tree selection/searching works
                requestParams.setAsync(false);
                if (parent.isWellKnownType(AssetType.TENANT)) {
                    LOG.fine("Loading the root assets of tenant: " + realm);
                    assetResource.getRoot(requestParams, realm);
                } else if (parent.getId() == null) {
                    LOG.fine("Loading the root assets of authenticated tenant");
                    assetResource.getRoot(requestParams, null);
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
            ex -> handleRequestException(ex, environment)
        );
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

    protected String[] getTenantAdjustedAssetPath(Asset asset) {
        List<String> path = new ArrayList<>();
        // If we are in the master realm, we try to find the tenant and prefix the
        // path array with the tenant asset ID, since that is the root level of
        // the asset tree
        String currentRealm = environment.getSecurityService().getRealm();
        if (currentRealm.equals(Constants.MASTER_REALM)) {
            for (AssetInfo tenantAssetInfo : tenantAssetInfos) {
                if (tenantAssetInfo.getRealm().equals(asset.getRealm())) {
                    path.add(tenantAssetInfo.getId());
                    break;
                }
            }
        }
        if (asset.getPath() != null) {
            path.addAll(Arrays.asList(asset.getPath()));
        }
        return path.toArray(new String[path.size()]);
    }
}
