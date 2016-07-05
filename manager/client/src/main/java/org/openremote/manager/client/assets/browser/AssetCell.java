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
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import org.openremote.manager.client.assets.Asset;

class AssetCell extends AbstractSafeHtmlCell<Asset> {

    static final SafeHtmlRenderer<Asset> assetRenderer = new SafeHtmlRenderer<Asset>() {
        public SafeHtml render(Asset object) {
            return (object == null)
                ? SafeHtmlUtils.EMPTY_SAFE_HTML
                : SafeHtmlUtils.fromString(object.getDisplayName()
            );
        }

        public void render(Asset object, SafeHtmlBuilder appendable) {
            appendable.append(SafeHtmlUtils.fromString(object.getDisplayName()));
        }
    };

    public AssetCell() {
        super(assetRenderer);
    }

    @Override
    public void render(Context context, SafeHtml value, SafeHtmlBuilder sb) {
        if (value != null) {
            sb.append(value);
        }
    }
}


