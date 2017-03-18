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

import org.openremote.manager.client.style.FormTreeStyle;
import org.openremote.manager.client.widget.FormTree;

public class AssetTree extends FormTree {

    public static class AssetIdSearch extends Search<BrowserTreeNode, String> {

        @Override
        protected boolean isMatchingPathElement(String pathElement, BrowserTreeNode value) {
            return value.getId().equals(pathElement);
        }
    }

    public AssetTree(AssetTreeModel viewModel, BrowserTreeNode rootNode, FormTreeStyle formTreeStyle, CellTreeMessages messages) {
        super(viewModel, rootNode, formTreeStyle, messages);
    }

    @Override
    public AssetTreeModel getTreeViewModel() {
        return (AssetTreeModel) super.getTreeViewModel();
    }

    @Override
    protected String getTreeNodeId(Object treeNodeValue) {
        if (treeNodeValue instanceof BrowserTreeNode) {
            BrowserTreeNode node = (BrowserTreeNode) treeNodeValue;
            return node.getId();
        }
        return treeNodeValue.toString();
    }
}
