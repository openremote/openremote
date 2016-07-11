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
package org.openremote.manager.client.widget;

import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.view.client.TreeViewModel;
import org.openremote.manager.client.style.FormTreeStyle;
import org.openremote.manager.client.style.ThemeStyle;
import org.openremote.manager.client.style.WidgetStyle;

import java.util.logging.Logger;

public class FormTree extends CellTree {

    private static final Logger LOG = Logger.getLogger(FormTree.class.getName());

    final protected WidgetStyle widgetStyle;
    final protected ThemeStyle themeStyle;

    public <T> FormTree(TreeViewModel viewModel, T rootValue, FormTreeStyle formTreeStyle, CellTreeMessages messages) {
        super(viewModel, rootValue, formTreeStyle.getCellTreeResources(), messages);
        this.widgetStyle = formTreeStyle.getWidgetStyle();
        this.themeStyle = formTreeStyle.getThemeStyle();
    }

    public void expandTreeNode(TreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (!node.isChildLeaf(i)) {
                expandTreeNode(node.setChildOpen(i, true));
            }
        }
    }

    public void collapseTreeNode(TreeNode node) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (!node.isChildLeaf(i)) {
                collapseTreeNode(node.setChildOpen(i, false));
            }
        }
    }

}
