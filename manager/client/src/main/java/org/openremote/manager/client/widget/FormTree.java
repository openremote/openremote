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
package org.openremote.manager.client.widget;

import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.view.client.TreeViewModel;
import org.openremote.manager.client.style.FormTreeStyle;
import org.openremote.manager.client.style.WidgetStyle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class FormTree extends CellTree {

    public abstract static class Search<T, P> {

        /**
         * Looks up every element in <code>path</code> and returns a list of node values, descending
         * and loading/expanding the tree while resolving. The returned node values are the path that
         * was taken while descending the tree, the starting node's value is first.
         */
        public List<T> resolvePath(List<P> path, TreeNode startingNode) {
            List<T> entityPath = new ArrayList<>();
            if (path.size() == 0)
                return entityPath;
            resolvePath(path, entityPath, 0, startingNode);

            // If we have a computed path, we need to...
            if (entityPath.size() > 0) {
                // Check if we actually found all path elements, if not, bail out with empty result
                if (entityPath.size() != path.size()) {
                    entityPath.clear();
                } else {
                    // Otherwise, add the starting node value as the first element
                    //noinspection unchecked
                    entityPath.add(0, (T) startingNode.getValue());
                }
            }
            return entityPath;
        }

        protected void resolvePath(List<P> path, List<T> entityPath, int level, TreeNode node) {
            P pathElement = path.get(level);
            // Look for the path element in the children
            int count = node.getChildCount();
            for (int i = 0; i < count; i++) {
                @SuppressWarnings("unchecked")
                T childEntity = (T) node.getChildValue(i);
                if (isMatchingPathElement(pathElement, childEntity)) {
                    // We found the path element, add the entity to the path list
                    entityPath.add(childEntity);

                    if (path.size() == entityPath.size()) {
                        // Reached the end of our search
                        break;
                    }

                    // Continue searching if we can descent
                    if (!node.isChildLeaf(i)) {
                        // Opening a node is the only way to load/access it
                        TreeNode n = node.setChildOpen(i, true);
                        if (n != null) {
                            resolvePath(path, entityPath, ++level, n);
                        }
                    }
                }
            }
        }

        protected abstract boolean isMatchingPathElement(P pathElement, T value);
    }

    final protected WidgetStyle widgetStyle;

    public <T> FormTree(TreeViewModel viewModel, T rootNode, FormTreeStyle formTreeStyle, CellTreeMessages messages) {
        super(viewModel, rootNode, formTreeStyle.getCellTreeResources(), messages);
        this.widgetStyle = formTreeStyle.getWidgetStyle();
    }

    public void refresh() {
        refresh(null);
    }

    public void refresh(String forceOpenNodeId) {
        Map<String, Boolean> openMap = new HashMap<>();
        TreeNode root = getRootTreeNode();
        getNodeOpenMap(root, openMap);
        openMap.put(getTreeNodeId(root), true);
        if (forceOpenNodeId != null)
            openMap.put(forceOpenNodeId, true);
        refresh(root, openMap);
    }

    public void refresh(TreeNode treeNode, Map<String, Boolean> openMap) {
        if (treeNode == null) {
            return;
        }
        for (int i = 0, n = treeNode.getChildCount(); i < n; ++i) {
            if (null == treeNode.getChildValue(i) ||
                treeNode.isChildLeaf(i)) {
                continue;
            }
            treeNode.setChildOpen(i, false);
            Boolean open = openMap.get(getTreeNodeId(treeNode.getChildValue(i)));
            if (open != null && open) {
                TreeNode childNode = treeNode.setChildOpen(i, true);
                refresh(childNode, openMap);
            }
        }
    }

    public void getNodeOpenMap(TreeNode treeNode, Map<String, Boolean> openMap) {
        if (treeNode == null) {
            return;
        }
        for (int i = 0, n = treeNode.getChildCount(); i < n; ++i) {
            if (null == treeNode.getChildValue(i) ||
                treeNode.isChildLeaf(i)) {
                continue;
            }

            openMap.put(getTreeNodeId(treeNode.getChildValue(i)), treeNode.isChildOpen(i));

            // This gets the child node, but doesn't change the open status (there's no other way to get the child)
            TreeNode childNode = treeNode.setChildOpen(i, treeNode.isChildOpen(i));

            getNodeOpenMap(childNode, openMap);
        }
    }

    abstract protected String getTreeNodeId(Object treeNodeValue);
}
