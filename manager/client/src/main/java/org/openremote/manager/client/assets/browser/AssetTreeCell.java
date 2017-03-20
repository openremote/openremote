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

import com.google.gwt.cell.client.AbstractSafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import org.openremote.manager.client.util.TextUtil;

class AssetTreeCell extends AbstractSafeHtmlCell<BrowserTreeNode> {

    public static final String CELL_ID_PREFIX = "assetTreeNode";

    public static String CELL_ID_SELECTOR(BrowserTreeNode treeNode) {
        return "#" + CELL_ID_PREFIX + "-" + treeNode.getId().replaceAll(":", "\\\\:");
    }

    public interface AssetTemplates extends SafeHtmlTemplates {
        @Template("<div id=\"" + CELL_ID_PREFIX + "-{0}\"><span class=\"or-FormTreeIcon fa fa-{2}\"></span>{1}</div>")
        SafeHtml treeNode(String id, String label, String icon);
    }

    private static final AssetTemplates TEMPLATES = GWT.create(AssetTemplates.class);

    public static class Renderer implements SafeHtmlRenderer<BrowserTreeNode> {

        final protected int maxLabelLength;

        public Renderer(int maxLabelLength) {
            this.maxLabelLength = maxLabelLength;
        }

        @Override
        public SafeHtml render(BrowserTreeNode node) {
            if (node == null)
                return SafeHtmlUtils.EMPTY_SAFE_HTML;
            SafeHtmlBuilder builder = new SafeHtmlBuilder();
            render(node, builder);
            return builder.toSafeHtml();
        }

        @Override
        public void render(BrowserTreeNode node, SafeHtmlBuilder appendable) {
            appendable.append(TEMPLATES.treeNode(
                node.getId(),
                TextUtil.ellipsize(node.getLabel(), maxLabelLength),
                node.getIcon()
            ));
        }
    }

    public AssetTreeCell(Renderer renderer) {
        super(renderer);
    }

    @Override
    public void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
        if (value != null) {
            sb.append(value);
        }
    }
}


