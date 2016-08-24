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
import org.openremote.manager.shared.asset.Asset;

import java.util.logging.Logger;

class AssetTreeModel implements TreeViewModel {

    private static final Logger LOG = Logger.getLogger(AssetTreeModel.class.getName());

    // This type is used when we have to stick a temporary asset into the tree for whatever reason,
    // e.g. an asset that is really only a loading message or some other UI signal for the user
    public static final String TEMPORARY_ASSET_TYPE = "TMP";

    final AssetBrowser.Presenter presenter;
    final SingleSelectionModel<Asset> selectionModel = new SingleSelectionModel<>();

    public AssetTreeModel(AssetBrowser.Presenter presenter) {
        this.presenter = presenter;
        selectionModel.addSelectionChangeHandler(selectionChangeEvent -> {
            if (selectionModel.getSelectedObject() != null) {
                presenter.onAssetSelected(selectionModel.getSelectedObject());
            }
        });
    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(T value) {
        return new DefaultNodeInfo<>(
            new AssetDataProvider(presenter, (Asset) value),
            new AssetCell(),
            selectionModel,
            null);
    }

    public SingleSelectionModel<Asset> getSelectionModel() {
        return selectionModel;
    }

    public boolean isLeaf(Object value) {
        // Currently we do not have leaf nodes in the tree, so we always offer the user an option to expand
        // and therefore query/refresh an asset, to see if there are "now" any child assets assigned. The only
        // other choice would be to determine leaf or composite by asset type, which is kinda arbitrary.

        // The exception to this is any temporary asset, which should not be expandable
        if (value instanceof Asset) {
            Asset asset = (Asset) value;
            if (TEMPORARY_ASSET_TYPE.equals(asset.getType()))
                return true;
        }

        return false;
    }
}
