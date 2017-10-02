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
import org.openremote.manager.client.assets.AssetArrayMapper;
import org.openremote.manager.client.assets.AssetMapper;
import org.openremote.manager.client.assets.AssetQueryMapper;
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetQuery;
import org.openremote.model.asset.AssetTreeModifiedEvent;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetBrowserPresenter implements AssetBrowser.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetBrowserPresenter.class.getName());

    final Environment environment;
    final Tenant currentTenant;
    final AssetBrowser view;
    final AssetResource assetResource;
    final AssetMapper assetMapper;
    final AssetQueryMapper assetQueryMapper;
    final AssetArrayMapper assetArrayMapper;
    final TenantResource tenantResource;
    final TenantArrayMapper tenantArrayMapper;

    final List<TenantTreeNode> tenantNodes = new ArrayList<>();
    BrowserTreeNode selectedNode;
    String[] selectedNodePath;
    AssetSelector assetSelector;

    @Inject
    public AssetBrowserPresenter(Environment environment,
                                 Tenant currentTenant,
                                 AssetBrowser view,
                                 AssetResource assetResource,
                                 AssetMapper assetMapper,
                                 AssetQueryMapper assetQueryMapper,
                                 AssetArrayMapper assetArrayMapper,
                                 TenantResource tenantResource,
                                 TenantArrayMapper tenantArrayMapper) {
        this.environment = environment;
        this.currentTenant = currentTenant;
        this.view = view;
        this.tenantResource = tenantResource;
        this.tenantArrayMapper = tenantArrayMapper;
        this.assetResource = assetResource;
        this.assetMapper = assetMapper;
        this.assetQueryMapper = assetQueryMapper;
        this.assetArrayMapper = assetArrayMapper;

        view.setPresenter(this);

        environment.getEventBus().register(
            AssetTreeModifiedEvent.class,
            event -> {
                String modifiedNodeId = event.isTenantModified() ? event.getRealmId() : event.getAssetId();
                if (event.isNewAssetChildren()) {
                    LOG.fine("Asset tree modified on server, forcing open due to new child asset: " + modifiedNodeId);
                    view.refresh(event.getAssetId(), modifiedNodeId);
                } else {
                    LOG.fine("Asset tree modified on server, refreshing tree due to modified node: " + modifiedNodeId);
                    view.refresh(modifiedNodeId);
                }
            }
        );
    }

    @Override
    public void onViewAttached() {
        environment.getEventService().subscribe(
            AssetTreeModifiedEvent.class,
            environment.getSecurityService().isSuperUser()
                ? null
                : new AssetTreeModifiedEvent.TenantFilter(currentTenant.getId())
        );
    }

    @Override
    public void onViewDetached() {
        environment.getEventService().unsubscribe(AssetTreeModifiedEvent.class);
    }

    @Override
    public void setCreateAsset(boolean createAsset) {
        view.setCreateAsset(createAsset);
    }

    @Override
    public void loadAsset(String id, Consumer<Asset> assetConsumer) {
        environment.getRequestService().execute(
            assetMapper,
            requestParams -> assetResource.get(requestParams, id),
            200,
            assetConsumer,
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    public void loadNodeChildren(BrowserTreeNode parent, HasData<BrowserTreeNode> display) {
        // If parent is the invisible root of the tree, show a loading message
        if (parent instanceof RootTreeNode) {
            showLoadingMessage(display);
        }

        // If the parent is the invisible root of the tree and this is the superuser, load all tenants
        if (parent instanceof RootTreeNode && environment.getSecurityService().isSuperUser()) {
            loadTenants(display);
        } else {
            loadAssets(parent, display);
        }
    }

    @Override
    public void onNodeSelected(BrowserTreeNode treeNode) {
        if (treeNode == null) {
            // Reset the selected node
            selectedNode = null;
            if (assetSelector == null) {
                environment.getEventBus().dispatch(new AssetBrowserSelectionCleared());
            }
        } else if (selectedNode == null || !selectedNode.getId().equals(treeNode.getId())) {
            // If there was no previous selection, or the new selected node is a different one, set and fire event
            selectedNode = treeNode;
            if (assetSelector != null) {
                assetSelector.setSelectedNode(treeNode);
            } else {
                environment.getEventBus().dispatch(new AssetBrowserSelection(treeNode));
            }
        }
    }

    @Override
    public void selectAsset(Asset asset) {
        onNodeSelected(asset != null ? new AssetTreeNode(asset) : null);
        selectedNodePath = asset != null ? getTenantAdjustedAssetPath(asset) : null;
        if (selectedNode != null) {
            updateViewSelection(true);
        } else {
            view.clearSelection();
        }
    }

    @Override
    public void selectAssetById(String assetId) {
        if (assetId == null) {
            view.clearSelection();
            return;
        }
        loadAsset(assetId, this::selectAsset);
    }

    @Override
    public void selectTenant(String realmId) {
        BrowserTreeNode selectedTenantNode = null;
        for (BrowserTreeNode tenantNode : tenantNodes) {
            if (tenantNode.getId().equals(realmId))
                selectedTenantNode = tenantNode;
        }
        if (selectedTenantNode != null) {
            onNodeSelected(selectedTenantNode);
            selectedNodePath = new String[]{selectedTenantNode.getId()};
            updateViewSelection(true);
        } else {
            view.clearSelection();
        }
    }

    @Override
    public void clearSelection() {
        selectAsset(null);
    }

    @Override
    public BrowserTreeNode getSelectedNode() {
        return selectedNode;
    }

    @Override
    public void useSelector(AssetSelector assetSelector) {
        this.assetSelector = assetSelector;
    }

    protected void showLoadingMessage(HasData<BrowserTreeNode> display) {
        display.setRowData(0, Collections.singletonList(
            new LabelTreeNode(environment.getMessages().loadingAssets())
        ));
        display.setRowCount(1, true);
    }

    protected void loadTenants(HasData<BrowserTreeNode> display) {
        environment.getRequestService().execute(
            tenantArrayMapper,
            requestParams -> {
                // This must be synchronous, so tree selection/searching works
                requestParams.setAsync(false);
                tenantResource.getAll(requestParams);
            },
            200,
            tenants -> {
                tenantNodes.clear();
                for (Tenant tenant : tenants) {
                    tenantNodes.add(new TenantTreeNode(tenant));
                }
                display.setRowData(0, tenantNodes);
                display.setRowCount(tenantNodes.size(), true);
                afterNodeLoadChildren(tenantNodes);
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void loadAssets(BrowserTreeNode parent, HasData<BrowserTreeNode> display) {
        // TODO Pagination?
        // final Range range = display.getVisibleRange();
        environment.getRequestService().execute(
            assetArrayMapper,
            assetQueryMapper,
            requestParams -> {
                // This must be synchronous, so tree selection/searching works
                requestParams.setAsync(false);
                if (parent instanceof TenantTreeNode) {
                    assetResource.queryAssets(
                        requestParams,
                        new AssetQuery()
                            .tenant(new AssetQuery.TenantPredicate(parent.getId()))
                            .parent(new AssetQuery.ParentPredicate(true))
                    );
                } else if (parent instanceof RootTreeNode) {
                    assetResource.getCurrentUserAssets(requestParams);
                } else {
                    assetResource.queryAssets(requestParams, new AssetQuery().parent(parent.getId()));
                }
            },
            200,
            assets -> {
                List<BrowserTreeNode> treeNodes = new ArrayList<>();
                for (Asset asset : assets) {
                    treeNodes.add(new AssetTreeNode(asset));
                }
                display.setRowData(0, treeNodes);
                display.setRowCount(assets.length, true);
                afterNodeLoadChildren(treeNodes);
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void updateViewSelection(boolean scrollIntoView) {
        // Find the last selected node after a data refresh and select it again
        if (selectedNode != null && selectedNodePath != null) {
            view.showAndSelectNode(selectedNodePath, selectedNode, scrollIntoView);
        }
    }

    protected void afterNodeLoadChildren(List<? extends BrowserTreeNode> children) {
        if (selectedNode != null) {
            // Only scroll the view if the selected node was loaded
            boolean scroll = false;
            for (BrowserTreeNode child : children) {
                if (child.getId().equals(selectedNode.getId()))
                    scroll = true;
            }
            updateViewSelection(scroll);
        }
    }

    /**
     * If this is the superuser, we try to find the tenant of the asset and
     * prefix its path array with the tenant ID, since that is the root level
     * of the asset tree and we must use the whole path to identify nodes.
     */
    protected String[] getTenantAdjustedAssetPath(Asset asset) {
        List<String> path = new ArrayList<>();
        if (environment.getSecurityService().isSuperUser()) {
            for (TenantTreeNode tenantNode : tenantNodes) {
                if (tenantNode.getId().equals(asset.getRealmId())) {
                    path.add(tenantNode.getId());
                    break;
                }
            }
        }
        if (asset.getPath() != null) {
            path.addAll(Arrays.asList(asset.getReversePath()));
        }
        return path.toArray(new String[path.size()]);
    }
}
