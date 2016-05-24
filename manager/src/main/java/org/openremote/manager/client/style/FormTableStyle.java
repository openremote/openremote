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

import javax.inject.Inject;

/**
 * Adapter for cell table styling.
 */
public class FormTableStyle implements CellTable.Style {

    final protected StyleClientBundle styleClientBundle;
    final protected WidgetStyle widgetStyle;
    final protected ThemeStyle themeStyle;

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
    public FormTableStyle(StyleClientBundle styleClientBundle, WidgetStyle widgetStyle, ThemeStyle themeStyle) {
        this.styleClientBundle = styleClientBundle;
        this.widgetStyle = widgetStyle;
        this.themeStyle = themeStyle;
    }

    public CellTable.Resources getCellTableResources() {
        return cellTableResources;
    }

    public WidgetStyle getWidgetStyle() {
        return widgetStyle;
    }

    public ThemeStyle getThemeStyle() {
        return themeStyle;
    }

    @Override
    public String cellTableCell() {
        return widgetStyle.FormTableCell() + " " + themeStyle.FormTableCell();
    }

    @Override
    public String cellTableEvenRow() {
        return widgetStyle.FormTableEvenRow() + " " + themeStyle.FormTableEvenRow();
    }

    @Override
    public String cellTableEvenRowCell() {
        return widgetStyle.FormTableEvenRowCell() + " " + themeStyle.FormTableEvenRowCell();
    }

    @Override
    public String cellTableFirstColumn() {
        return widgetStyle.FormTableFirstColumn() + " " + themeStyle.FormTableFirstColumn();
    }

    @Override
    public String cellTableFirstColumnFooter() {
        return widgetStyle.FormTableFirstColumnFooter() + " " + themeStyle.FormTableFirstColumnFooter();
    }

    @Override
    public String cellTableFirstColumnHeader() {
        return widgetStyle.FormTableFirstColumnHeader() + " " + themeStyle.FormTableFirstColumnHeader();
    }

    @Override
    public String cellTableFooter() {
        return widgetStyle.FormTableFooter() + " " + themeStyle.FormTableFooter();
    }

    @Override
    public String cellTableHeader() {
        return widgetStyle.FormTableHeader() + " " + themeStyle.FormTableHeader();
    }

    @Override
    public String cellTableHoveredRow() {
        return widgetStyle.FormTableHoveredRow() + " " + themeStyle.FormTableHoveredRow();
    }

    @Override
    public String cellTableHoveredRowCell() {
        return widgetStyle.FormTableHoveredRowCell() + " " + themeStyle.FormTableHoveredRowCell();
    }

    @Override
    public String cellTableKeyboardSelectedCell() {
        return widgetStyle.FormTableKeyboardSelectedCell() + " " + themeStyle.FormTableKeyboardSelectedCell();
    }

    @Override
    public String cellTableKeyboardSelectedRow() {
        return widgetStyle.FormTableKeyboardSelectedRow() + " " + themeStyle.FormTableKeyboardSelectedRow();
    }

    @Override
    public String cellTableKeyboardSelectedRowCell() {
        return widgetStyle.FormTableKeyboardSelectedRowCell() + " " + themeStyle.FormTableKeyboardSelectedRowCell();
    }

    @Override
    public String cellTableLastColumn() {
        return widgetStyle.FormTableLastColumn() + " " + themeStyle.FormTableLastColumn();
    }

    @Override
    public String cellTableLastColumnFooter() {
        return widgetStyle.FormTableLastColumnFooter() + " " + themeStyle.FormTableLastColumnFooter();
    }

    @Override
    public String cellTableLastColumnHeader() {
        return widgetStyle.FormTableLastColumnHeader() + " " + themeStyle.FormTableLastColumnHeader();
    }

    @Override
    public String cellTableLoading() {
        return widgetStyle.FormTableLoading() + " " + themeStyle.FormTableLoading();
    }

    @Override
    public String cellTableOddRow() {
        return widgetStyle.FormTableOddRow() + " " + themeStyle.FormTableOddRow();
    }

    @Override
    public String cellTableOddRowCell() {
        return widgetStyle.FormTableOddRowCell() + " " + themeStyle.FormTableOddRowCell();
    }

    @Override
    public String cellTableSelectedRow() {
        return widgetStyle.FormTableSelectedRow() + " " + themeStyle.FormTableSelectedRow();
    }

    @Override
    public String cellTableSelectedRowCell() {
        return widgetStyle.FormTableSelectedRowCell() + " " + themeStyle.FormTableSelectedRowCell();
    }

    @Override
    public String cellTableSortableHeader() {
        return widgetStyle.FormTableSortableHeader() + " " + themeStyle.FormTableSortableHeader();
    }

    @Override
    public String cellTableSortedHeaderAscending() {
        return widgetStyle.FormTableSortedHeaderAscending() + " " + themeStyle.FormTableSortedHeaderAscending();
    }

    @Override
    public String cellTableSortedHeaderDescending() {
        return widgetStyle.FormTableSortedHeaderDescending() + " " + themeStyle.FormTableSortedHeaderDescending();
    }

    @Override
    public String cellTableWidget() {
        return widgetStyle.FormTableWidget() + " " + themeStyle.FormTableWidget();
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
