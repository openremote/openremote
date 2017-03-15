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

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.view.client.HasData;
import org.openremote.model.asset.Asset;

/**
 * Browse tenants and assets in a tree view.
 *
 * Listen to {@link AssetBrowserSelection} events to be notified of user selections. Call
 * the methods {@link Presenter#selectAsset(Asset)}, {@link Presenter#selectTenant(String)}
 * and {@link Presenter#clearSelection()} to modify the browser's current selected node.
 */
public interface AssetBrowser extends IsWidget {

    interface Presenter {

        void onViewAttached();

        void onViewDetached();

        void loadNodeChildren(AssetTreeNode parent, HasData<AssetTreeNode> display);

        void onNodeSelected(AssetTreeNode treeNode);

        void selectAsset(Asset asset);

        void selectTenant(String realm);

        void clearSelection();
    }

    void setPresenter(Presenter presenter);

    void showAndSelectNode(String[] path, AssetTreeNode treeNode, boolean scrollIntoView);

    void clearSelection();

    void refreshAssets(boolean isRootRefresh);

}
