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
package org.openremote.manager.client.style;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.CellTable;
import org.openremote.components.client.style.WidgetStyle;

import javax.inject.Inject;

/**
 * Adapter for cell table styling.
 */
public class FormTableStyle implements CellTable.Style {

    final protected StyleClientBundle styleClientBundle;
    final protected WidgetStyle widgetStyle;

    final protected CellTable.Resources cellTableResources = new CellTable.Resources() {
        @Override
        public ImageResource cellTableFooterBackground() {
            return null;
        }

        @Override
        public ImageResource cellTableHeaderBackground() {
            return null;
        }

        @Override
        public ImageResource cellTableLoading() {
            return styleClientBundle.FormTableLoading();
        }

        @Override
        public ImageResource cellTableSelectedBackground() {
            return null;
        }

        @Override
        public ImageResource cellTableSortAscending() {
            return null;
        }

        @Override
        public ImageResource cellTableSortDescending() {
            return null;
        }

        @Override
        public CellTable.Style cellTableStyle() {
            return FormTableStyle.this;
        }
    };

    @Inject
    public FormTableStyle(StyleClientBundle styleClientBundle, WidgetStyle widgetStyle) {
        this.styleClientBundle = styleClientBundle;
        this.widgetStyle = widgetStyle;
    }

    public CellTable.Resources getCellTableResources() {
        return cellTableResources;
    }

    public WidgetStyle getWidgetStyle() {
        return widgetStyle;
    }

    @Override
    public String cellTableCell() {
        return widgetStyle.FormTableCell();
    }

    @Override
    public String cellTableEvenRow() {
        return widgetStyle.FormTableEvenRow();
    }

    @Override
    public String cellTableEvenRowCell() {
        return widgetStyle.FormTableEvenRowCell();
    }

    @Override
    public String cellTableFirstColumn() {
        return widgetStyle.FormTableFirstColumn();
    }

    @Override
    public String cellTableFirstColumnFooter() {
        return widgetStyle.FormTableFirstColumnFooter();
    }

    @Override
    public String cellTableFirstColumnHeader() {
        return widgetStyle.FormTableFirstColumnHeader();
    }

    @Override
    public String cellTableFooter() {
        return widgetStyle.FormTableFooter();
    }

    @Override
    public String cellTableHeader() {
        return widgetStyle.FormTableHeader();
    }

    @Override
    public String cellTableHoveredRow() {
        return widgetStyle.FormTableHoveredRow();
    }

    @Override
    public String cellTableHoveredRowCell() {
        return widgetStyle.FormTableHoveredRowCell();
    }

    @Override
    public String cellTableKeyboardSelectedCell() {
        return widgetStyle.FormTableKeyboardSelectedCell();
    }

    @Override
    public String cellTableKeyboardSelectedRow() {
        return widgetStyle.FormTableKeyboardSelectedRow();
    }

    @Override
    public String cellTableKeyboardSelectedRowCell() {
        return widgetStyle.FormTableKeyboardSelectedRowCell();
    }

    @Override
    public String cellTableLastColumn() {
        return widgetStyle.FormTableLastColumn();
    }

    @Override
    public String cellTableLastColumnFooter() {
        return widgetStyle.FormTableLastColumnFooter();
    }

    @Override
    public String cellTableLastColumnHeader() {
        return widgetStyle.FormTableLastColumnHeader();
    }

    @Override
    public String cellTableLoading() {
        return widgetStyle.FormTableLoading();
    }

    @Override
    public String cellTableOddRow() {
        return widgetStyle.FormTableOddRow();
    }

    @Override
    public String cellTableOddRowCell() {
        return widgetStyle.FormTableOddRowCell();
    }

    @Override
    public String cellTableSelectedRow() {
        return widgetStyle.FormTableSelectedRow();
    }

    @Override
    public String cellTableSelectedRowCell() {
        return widgetStyle.FormTableSelectedRowCell();
    }

    @Override
    public String cellTableSortableHeader() {
        return widgetStyle.FormTableSortableHeader();
    }

    @Override
    public String cellTableSortedHeaderAscending() {
        return widgetStyle.FormTableSortedHeaderAscending();
    }

    @Override
    public String cellTableSortedHeaderDescending() {
        return widgetStyle.FormTableSortedHeaderDescending();
    }

    @Override
    public String cellTableWidget() {
        return widgetStyle.FormTable();
    }

    @Override
    public boolean ensureInjected() {
        return false;
    }

    @Override
    public String getText() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
