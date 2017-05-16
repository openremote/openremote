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
package org.openremote.manager.client.map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import elemental.client.Browser;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.*;
import org.openremote.manager.client.widget.PopupPanel;
import org.openremote.model.util.TextUtil;
import org.openremote.model.value.Value;
import org.openremote.model.value.Values;

import java.util.List;

public class MapInfoPanel extends AbstractAppPanel {

    public static final int MAX_ITEMS_BEFORE_SCROLLING = 6;

    interface UI extends UiBinder<PopupPanel, MapInfoPanel> {
    }

    public interface Style extends CssResource {

        String infoItemLabel();

        String popup();

        String infoItemValue();

        String infoItem();

        String panel();

        String content();
    }

    @UiField
    ManagerMessages managerMessages;

    @UiField
    WidgetStyle widgetStyle;

    @UiField
    Style style;

    @UiField
    HTMLPanel panel;

    @UiField
    FlowPanel contentPanel;

    public MapInfoPanel() {
        super(GWT.create(UI.class));
    }

    public void setItems(List<MapInfoItem> infoItems) {
        contentPanel.clear();

        int itemLineHeightPixels = 20;
        int itemMarginPixels = 8;
        int itemHeightPixels = itemLineHeightPixels + (itemMarginPixels * 2);
        int totalMaxHeight = itemHeightPixels * MAX_ITEMS_BEFORE_SCROLLING;

        for (MapInfoItem infoItem : infoItems) {
            FormLabel itemLabel = new FormLabel(
                TextUtil.ellipsize(infoItem.getLabel(), 35)
            );
            itemLabel.addStyleName("flex");
            itemLabel.addStyleName(style.infoItemLabel());

            Widget itemValue = createItemValue(infoItem);
            itemValue.addStyleName(style.infoItemValue());

            FlowPanel itemPanel = new FlowPanel();
            itemPanel.addStyleName("flex-none layout horizontal");
            itemPanel.addStyleName(style.infoItem());
            itemPanel.add(itemLabel);
            itemPanel.add(itemValue);
            contentPanel.add(itemPanel);
        }

        // Show up to 6 items, then start scrolling
        panel.setHeight(Math.min((infoItems.size() * itemHeightPixels), totalMaxHeight) + "px");

        // If the panel is already shown, "blink" it so users know there is an update
        if (isShowing()) {
            contentPanel.addStyleName(widgetStyle.HighlightBackground());
            Browser.getWindow().setTimeout(() -> {
                contentPanel.removeStyleName(widgetStyle.HighlightBackground());
            }, 250);
        }
    }

    protected Widget createItemValue(MapInfoItem item) {
        if (!item.getValue().isPresent())
            return new Label("-");
        Value value = item.getValue().get();

        switch (value.getType()) {
            case BOOLEAN:
                return new IconLabel(Values.getBoolean(value).get() ? "check-square" : "square-o");
            default:
                return new Label(
                    item.getFormat()
                        .map(format -> formatValue(format, value.toString()))
                        .orElse(value.toString())
                );
        }
    }

    protected native static String formatValue(String formatString, String value) /*-{
        if (value === null)
            return '-';
        if (formatString === null)
            return value;
        try {
            return $wnd.sprintf(formatString, value);
        } catch (e) {
            console.log(e.message);
            return value;
        }
    }-*/;

}
