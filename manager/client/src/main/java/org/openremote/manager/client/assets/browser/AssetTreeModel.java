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

import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import org.openremote.manager.shared.asset.AssetInfo;
import org.openremote.manager.shared.asset.AssetType;

import java.util.logging.Logger;

class AssetTreeModel implements TreeViewModel {

    private static final Logger LOG = Logger.getLogger(AssetTreeModel.class.getName());

    // This type is used when we have to stick a temporary asset into the tree for whatever reason,
    // e.g. an asset that is really only a loading message or some other UI signal for the user
    public static final String TEMPORARY_ASSET_TYPE = "TMP";

    final protected AssetCell.Renderer renderer;
    final AssetBrowser.Presenter presenter;
    final SingleSelectionModel<AssetInfo> selectionModel = new SingleSelectionModel<>();

    public AssetTreeModel(AssetBrowser.Presenter presenter, AssetCell.Renderer renderer) {
        this.presenter = presenter;
        this.renderer = renderer;
        selectionModel.addSelectionChangeHandler(selectionChangeEvent -> {
            if (selectionModel.getSelectedObject() != null) {
                presenter.onAssetSelected(selectionModel.getSelectedObject());
            }
        });
    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
        return new DefaultNodeInfo<>(
            new AssetDataProvider(presenter, (AssetInfo) value),
            new AssetCell(renderer),
            selectionModel,
            null);
    }

    public SingleSelectionModel<AssetInfo> getSelectionModel() {
        return selectionModel;
    }

    public boolean isLeaf(Object value) {
        if (value instanceof AssetInfo) {
            AssetInfo asset = (AssetInfo) value;
            if (asset.getType() != null) {
                if (TEMPORARY_ASSET_TYPE.equals(asset.getType()))
                    return true;

                // And another exception are types which we know to be a leaf
                if (AssetType.isLeaf(asset.getWellKnownType()))
                    return true;
            }
        }
        return false;
    }
}
