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

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.*;
import org.openremote.manager.client.assets.asset.Asset;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTreeStyle;
import org.openremote.manager.client.widget.PushButton;

import javax.inject.Inject;
import java.util.List;
import java.util.logging.Logger;

public class AssetBrowserImpl extends Composite implements AssetBrowser {

    private static final Logger LOG = Logger.getLogger(AssetBrowserImpl.class.getName());

    interface UI extends UiBinder<HTMLPanel, AssetBrowserImpl> {
    }

    @UiField
    ManagerMessages managerMessages;

    @UiField
    SimplePanel assetTreeContainer;

    @UiField
    TextBox searchInput;

    @UiField
    PushButton filterButton;

/*
    @UiField
    PushButton expandButton;
*/

    final FormTreeStyle formTreeStyle;

    Presenter presenter;
    AssetTree assetTree;

    @Inject
    public AssetBrowserImpl(FormTreeStyle formTreeStyle) {
        this.formTreeStyle = formTreeStyle;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        assetTree = new AssetTree(
            new AssetTreeModel(presenter),
            new Asset(Asset.ROOT_ID, Asset.ROOT_TYPE, Asset.ROOT_LABEL, Asset.ROOT_LOCATION),
            formTreeStyle,
            new CellTree.CellTreeMessages() {
                @Override
                public String showMore() {
                    return managerMessages.showMoreAssets();
                }

                @Override
                public String emptyTree() {
                    return managerMessages.emptyCompositeAsset();
                }
            }
        );

        // TODO Page size and paging is not good, do something with onhover autoscroll
        assetTree.setDefaultNodeSize(1000);

        assetTreeContainer.clear();
        assetTreeContainer.add(assetTree);
    }

    @Override
    public void showAndSelectAsset(List<String> path, String selectedAssetId) {
        List<Asset> selectedPath = new AssetTree.IdSearch().resolvePath(path, assetTree.getRootTreeNode());
        if (selectedPath.size() > 0) {
            Asset selectedAsset = selectedPath.get(selectedPath.size()-1);
            assetTree.getTreeViewModel().getSelectionModel().setSelected(selectedAsset, true);
        }
    }

    @Override
    public void deselectAssets() {
        assetTree.getTreeViewModel().getSelectionModel().clear();
    }
}
