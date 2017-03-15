/*
 * Copyright 2017, OpenRemote Inc.
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

import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;

class AssetTreeModel implements TreeViewModel {

    final protected AssetTreeCell.Renderer renderer;
    final AssetBrowser.Presenter presenter;
    final SingleSelectionModel<AssetTreeNode> selectionModel = new SingleSelectionModel<>();

    public AssetTreeModel(AssetBrowser.Presenter presenter, AssetTreeCell.Renderer renderer) {
        this.presenter = presenter;
        this.renderer = renderer;
        selectionModel.addSelectionChangeHandler(selectionChangeEvent -> {
            presenter.onNodeSelected(selectionModel.getSelectedObject());
        });
    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
        return new DefaultNodeInfo<>(
            new AssetTreeDataProvider(presenter, (AssetTreeNode) value),
            new AssetTreeCell(renderer),
            selectionModel,
            null);
    }

    public SingleSelectionModel<AssetTreeNode> getSelectionModel() {
        return selectionModel;
    }

    public boolean isLeaf(Object value) {
        if (value instanceof AssetTreeNode) {
            AssetTreeNode node = (AssetTreeNode) value;
            return node.isLeaf();
        }
        return false;
    }
}
