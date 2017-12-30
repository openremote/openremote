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

import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextHeader;
import org.openremote.manager.client.style.FormTableStyle;
import org.openremote.components.client.style.WidgetStyle;

public class FormTable<T> extends CellTable<T> {

    final protected WidgetStyle widgetStyle;

    public FormTable(int pageSize, FormTableStyle formTableStyle) {
        super(pageSize, formTableStyle.getCellTableResources());
        this.widgetStyle = formTableStyle.getWidgetStyle();
    }

    protected void applyStyleCellText(Column column) {
        column.setCellStyleNames(widgetStyle.FormTableCellText());
    }

    protected Header createHeader(String text) {
        TextHeader header = new TextHeader(text);
        header.setHeaderStyleNames(widgetStyle.FormTableHeaderCell());
        return header;
    }

}
