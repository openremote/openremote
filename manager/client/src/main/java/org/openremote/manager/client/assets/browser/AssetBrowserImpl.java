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
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTreeStyle;
import org.openremote.manager.client.widget.PushButton;
import org.openremote.manager.shared.asset.AssetInfo;

import javax.inject.Inject;
import java.util.Arrays;
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

    final FormTreeStyle formTreeStyle;

    Presenter presenter;
    AssetTree assetTree;
    AssetInfo assetTreeRoot = new AssetInfo();

    @Inject
    public AssetBrowserImpl(FormTreeStyle formTreeStyle) {
        this.formTreeStyle = formTreeStyle;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        createAssetTree();
    }

    @Override
    public void showAndSelectAsset(String[] path, String selectedAssetId, boolean scrollIntoView) {
        List<AssetInfo> selectedPath = new AssetTree.IdSearch().resolvePath(
            Arrays.asList(path), assetTree.getRootTreeNode()
        );

        if (selectedPath.size() > 0) {
            AssetInfo selectedAsset = selectedPath.get(selectedPath.size() - 1);
            assetTree.getTreeViewModel().getSelectionModel().setSelected(selectedAsset, true);

            if (!scrollIntoView)
                return;

            // We place the selected asset in the middle of the tree container
            elemental.dom.Element treeElement = (elemental.dom.Element) assetTree.getElement();
            String selector = "#asset-" + selectedAssetId.replaceAll(":", "\\\\:");
            elemental.dom.Element assetElement = treeElement.querySelector(selector);
            int offsetTop = 0;
            if (assetElement != null && assetElement.getOffsetParent() != null) {
                elemental.dom.Element el = assetElement.getOffsetParent();
                do {
                    offsetTop += el.getOffsetTop();
                } while ((el = el.getOffsetParent()) != null);
                Element treeContainerElement = assetTreeContainer.getElement();
                int middleOffset = offsetTop - treeContainerElement.getClientHeight()/2 - treeContainerElement.getOffsetTop();
                treeContainerElement.setScrollTop(middleOffset);
            }
        }
    }

    @Override
    public void deselectAssets() {
        assetTree.getTreeViewModel().getSelectionModel().clear();
    }

    @Override
    public void refreshAssets(boolean isRootRefresh) {
        if (isRootRefresh) {
            // TODO Horrible but I have no idea how to force a reload of the root node of a CellTree
            createAssetTree();
        } else {
            assetTree.refresh();
        }
    }

    protected void createAssetTree() {
        AssetCell.Renderer assetCellRenderer = new AssetCell.Renderer(44);
        assetTree = new AssetTree(
            new AssetTreeModel(presenter, assetCellRenderer),
            assetTreeRoot,
            formTreeStyle,
            new CellTree.CellTreeMessages() {
                @Override
                public String showMore() {
                    return managerMessages.showMoreAssets();
                }

                @Override
                public String emptyTree() {
                    return managerMessages.emptyAsset();
                }
            }
        );

        // TODO Page size and paging is not good, do something with onhover autoscroll
        assetTree.setDefaultNodeSize(1000);

        assetTreeContainer.clear();
        assetTreeContainer.add(assetTree);
    }
}
