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
import org.openremote.manager.shared.asset.AssetResource;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.model.asset.Asset;
import org.openremote.model.asset.AssetInfo;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AssetBrowserPresenter implements AssetBrowser.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetBrowserPresenter.class.getName());

    final Environment environment;
    final AssetBrowser view;
    final AssetResource assetResource;
    final AssetInfoArrayMapper assetInfoArrayMapper;
    final TenantResource tenantResource;
    final TenantArrayMapper tenantArrayMapper;

    final List<AssetTreeNode> tenantNodes = new ArrayList<>();
    AssetTreeNode selectedNode;
    String[] selectedNodePath;

    @Inject
    public AssetBrowserPresenter(Environment environment,
                                 AssetBrowser view,
                                 AssetResource assetResource,
                                 AssetInfoArrayMapper assetInfoArrayMapper,
                                 TenantResource tenantResource,
                                 TenantArrayMapper tenantArrayMapper) {
        this.environment = environment;
        this.view = view;
        this.tenantResource = tenantResource;
        this.tenantArrayMapper = tenantArrayMapper;
        this.assetResource = assetResource;
        this.assetInfoArrayMapper = assetInfoArrayMapper;

        view.setPresenter(this);
    }

    @Override
    public void onViewAttached() {
        // TODO LOG.fine("Asset browser attached, subscribing to asset changes on the server");
    }

    @Override
    public void onViewDetached() {
        // TODO LOG.fine("Asset browser detached, unsubscribing from asset changes on the server");
    }

    @Override
    public void loadNodeChildren(AssetTreeNode parent, HasData<AssetTreeNode> display) {
        // If parent is the invisible root of the tree, show a loading message
        if (parent.isRoot()) {
            showLoadingMessage(display);
        }

        // If the parent is the invisible root of the tree and this is the superuser, load all tenants
        if (parent.isRoot() && environment.getSecurityService().isSuperUser()) {
            loadTenants(display);
        } else {
            loadAssets(parent, display);
        }
    }

    @Override
    public void onNodeSelected(AssetTreeNode treeNode) {
        if (treeNode == null) {
            // Reset the selected node
            selectedNode  = null;
            environment.getEventBus().dispatch(new AssetBrowserSelection());
        } else if (selectedNode == null || !selectedNode.getId().equals(treeNode.getId())) {
            // If there was no previous selection, or the new selected node is a different one, set and fire event
            selectedNode = treeNode;
            environment.getEventBus().dispatch(new AssetBrowserSelection(treeNode));
        }
    }

    @Override
    public void selectAsset(Asset asset) {
        onNodeSelected(asset != null ? new AssetTreeNode(new AssetInfo(asset)) : null);
        selectedNodePath = asset != null ? getTenantAdjustedAssetPath(asset) : null;
        if (selectedNode != null) {
            updateViewSelection(true);
        } else {
            view.clearSelection();
        }
    }

    @Override
    public void selectTenant(String realm) {
        AssetTreeNode selectedTenantNode = null;
        for (AssetTreeNode tenantNode : tenantNodes) {
            if (tenantNode.getRealm().equals(realm))
                selectedTenantNode = tenantNode;
        }
        if (selectedTenantNode != null) {
            onNodeSelected(selectedTenantNode);
            selectedNodePath = new String[] { selectedTenantNode.getId() };
            updateViewSelection(true);
        } else {
            view.clearSelection();
        }
    }

    @Override
    public void clearSelection() {
        selectAsset(null);
    }

    protected void showLoadingMessage(HasData<AssetTreeNode> display) {
        display.setRowData(0, Collections.singletonList(
            new AssetTreeNode(environment.getMessages().loadingAssets())
        ));
        display.setRowCount(1, true);
    }

    protected void loadTenants(HasData<AssetTreeNode> display) {
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
                    tenantNodes.add(new AssetTreeNode(tenant));
                }
                display.setRowData(0, tenantNodes);
                display.setRowCount(tenantNodes.size(), true);
                afterNodeLoadChildren(tenantNodes);
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    protected void loadAssets(AssetTreeNode parent, HasData<AssetTreeNode> display) {
        // TODO Pagination?
        // final Range range = display.getVisibleRange();
        environment.getRequestService().execute(
            assetInfoArrayMapper,
            requestParams -> {
                // This must be synchronous, so tree selection/searching works
                requestParams.setAsync(false);
                if (parent.isTenant()) {
                    LOG.fine("Loading root assets of: " + parent);
                    assetResource.getRoot(requestParams, parent.getRealm());
                } else if (parent.isRoot()) {
                    LOG.fine("Loading user assets of authenticated user");
                    assetResource.getCurrentUserAssets(requestParams);
                } else {
                    LOG.fine("Loading child assets of: " + parent);
                    assetResource.getChildren(requestParams, parent.getId());
                }
            },
            200,
            assetInfos -> {
                List<AssetTreeNode> treeNodes = new ArrayList<>();
                for (AssetInfo assetInfo : assetInfos) {
                    treeNodes.add(new AssetTreeNode(assetInfo));
                }
                display.setRowData(0, treeNodes);
                display.setRowCount(assetInfos.length, true);
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

    protected void afterNodeLoadChildren(List<AssetTreeNode> children) {
        if (selectedNode != null) {
            // Only scroll the view if the selected node was loaded
            boolean scroll = false;
            for (AssetTreeNode child : children) {
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
            for (AssetTreeNode tenantNode : tenantNodes) {
                if (tenantNode.getRealm().equals(asset.getRealm())) {
                    path.add(tenantNode.getId());
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
