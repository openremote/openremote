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
package org.openremote.app.client.assets.browser;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.view.client.HasData;
import org.openremote.model.asset.Asset;
import org.openremote.model.interop.Consumer;

/**
 * Browse tenants and assets in a tree view.
 *
 * Listen to {@link AssetBrowserSelection} events to be notified of user selections. Call
 * the methods {@link Presenter#selectAsset(Asset)} and {@link Presenter#clearSelection}
 * to modify the browser's current selected node.
 */
public interface AssetBrowser extends IsWidget {

    interface Presenter {

        void onViewAttached();

        void onViewDetached();

        void setCreateAsset(boolean createAsset);

        void loadAsset(String id, Consumer<Asset> assetConsumer);

        void loadNodeChildren(BrowserTreeNode parent, HasData<BrowserTreeNode> display);

        void onNodeSelected(BrowserTreeNode treeNode);

        void selectAsset(Asset asset);

        void selectAssetById(String assetId);

        void selectRealm(String realm);

        void clearSelection();

        BrowserTreeNode getSelectedNode();

        /**
         * If not-null, the presenter will send selection change events to this instead of the event bus.
         */
        void useSelector(AssetSelector assetSelector);
    }

    void setPresenter(Presenter presenter);

    Presenter getPresenter();

    void setCreateAsset(boolean createAsset);

    void showAndSelectNode(String[] path, BrowserTreeNode treeNode, boolean scrollIntoView);

    void clearSelection();

    void refresh(String modifiedNodeId);

    void refresh(String modifiedNodeId, String forceNodeOpenId);
}
