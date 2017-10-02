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
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.FormTreeStyle;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.FormInputText;
import org.openremote.manager.client.widget.Hyperlink;
import org.openremote.manager.client.widget.PushButton;

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
    WidgetStyle widgetStyle;

    @UiField
    FormInputText searchInput;

    @UiField
    PushButton filterButton;

    @UiField
    PushButton sortButton;

    @UiField
    Hyperlink createAssetLink;

    @UiField
    SimplePanel assetTreeContainer;

    final FormTreeStyle formTreeStyle;

    Presenter presenter;
    AssetTree assetTree;
    final RootTreeNode rootTreeNode = new RootTreeNode();

    @Inject
    public AssetBrowserImpl(FormTreeStyle formTreeStyle) {
        this.formTreeStyle = formTreeStyle;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));

        addAttachHandler(event -> {
            if (presenter == null)
                return;
            if (event.isAttached()) {
                presenter.onViewAttached();
            } else {
                presenter.onViewDetached();
            }
        });
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        createAssetTree();
    }

    @Override
    public Presenter getPresenter() {
        return presenter;
    }


    @Override
    public void setCreateAsset(boolean createAsset) {
        createAssetLink.removeStyleName("active");
        if (createAsset)
            createAssetLink.addStyleName("active");
    }

    @Override
    public void showAndSelectNode(String[] path, BrowserTreeNode treeNode, boolean scrollIntoView) {
        List<BrowserTreeNode> selectedPath = new AssetTree.AssetIdSearch().resolvePath(
            Arrays.asList(path), assetTree.getRootTreeNode()
        );

        if (selectedPath.size() > 0) {
            BrowserTreeNode selectedNode = selectedPath.get(selectedPath.size() - 1);
            assetTree.getTreeViewModel().getSelectionModel().setSelected(selectedNode, true);

            if (!scrollIntoView)
                return;

            // We place the selected asset in the middle of the tree container
            elemental.dom.Element treeElement = (elemental.dom.Element) assetTree.getElement();
            elemental.dom.Element assetElement = treeElement.querySelector(AssetTreeCell.CELL_ID_SELECTOR(treeNode));
            int offsetTop = 0;
            if (assetElement != null && assetElement.getOffsetParent() != null) {
                elemental.dom.Element el = assetElement.getOffsetParent();
                do {
                    offsetTop += el.getOffsetTop();
                } while ((el = el.getOffsetParent()) != null);
                Element treeContainerElement = assetTreeContainer.getElement();
                treeContainerElement.setAttribute("tabindex", "1");
                int middleOffset = offsetTop - treeContainerElement.getClientHeight() / 2 - treeContainerElement.getOffsetTop();
                treeContainerElement.setScrollTop(middleOffset);
            }
        }
    }

    @Override
    public void clearSelection() {
        assetTree.getTreeViewModel().getSelectionModel().clear();
    }


    @Override
    public void refresh(String modifiedNodeId) {
        refresh(modifiedNodeId, null);
    }

    @Override
    public void refresh(String modifiedNodeId, String forceNodeOpenId) {
        // TODO Horrible but I have no idea how to force a reload of the root node of a CellTree
        boolean forceRebuild = false;
        for (int i = 0, n = assetTree.getRootTreeNode().getChildCount(); i < n; ++i) {
            BrowserTreeNode treeNode = (BrowserTreeNode) assetTree.getRootTreeNode().getChildValue(i);
            if (treeNode.getId().equals(modifiedNodeId)) {
                forceRebuild = true;
                break;
            }
        }
        if (forceRebuild) {
            createAssetTree();
        } else {
            assetTree.refresh(forceNodeOpenId);
        }
    }

    protected void createAssetTree() {
        AssetTreeCell.Renderer assetCellRenderer = new AssetTreeCell.Renderer(44);
        assetTree = new AssetTree(
            new AssetTreeModel(presenter, assetCellRenderer),
            rootTreeNode,
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

        assetTree.addStyleName(widgetStyle.RightGradient());

        assetTreeContainer.clear();
        assetTreeContainer.add(assetTree);
    }
}
