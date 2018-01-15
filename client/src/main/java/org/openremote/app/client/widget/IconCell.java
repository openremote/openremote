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
package org.openremote.app.client.widget;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class IconCell extends AbstractCell<String> {

    interface Templates extends SafeHtmlTemplates {
        @SafeHtmlTemplates.Template("<span class=\"fa fa-{0}\"></span>")
        SafeHtml cell(String value);
    }

    private static Templates templates = GWT.create(Templates.class);

    @Override
    public void render(Context context, String value, SafeHtmlBuilder sb) {
        if (value == null) {
            return;
        }
        SafeHtml rendered = templates.cell(value);
        sb.append(rendered);
    }
}
