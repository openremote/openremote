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

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openremote.model.Event;

@JsonIgnoreType
public class AssetBrowserSelection extends Event {

    final protected AssetTreeNode selectedNode;

    public AssetBrowserSelection() {
        selectedNode = null;
    }

    public AssetBrowserSelection(AssetTreeNode selectedNode) {
        this.selectedNode = selectedNode;
    }

    public AssetTreeNode getSelectedNode() {
        return selectedNode;
    }

    public boolean isClearedSelection() {
        return getSelectedNode() == null;
    }

    public boolean isTenantSelection() {
        return getSelectedNode() != null
            && getSelectedNode().isTenant();
    }

    public boolean isAssetSelection() {
        return getSelectedNode() != null
            && !getSelectedNode().isTenant()
            && !getSelectedNode().isRoot()
            && !getSelectedNode().isTemporary();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "selectedNode=" + selectedNode +
            '}';
    }
}
