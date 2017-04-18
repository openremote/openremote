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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import elemental.client.Browser;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.style.WidgetStyle;
import org.openremote.manager.client.widget.AbstractAppPanel;
import org.openremote.manager.client.widget.FormLabel;
import org.openremote.manager.client.widget.PopupPanel;
import org.openremote.model.Pair;
import org.openremote.model.util.TextUtil;

import java.util.List;

public class MapInfoPanel extends AbstractAppPanel {

    public static final int MAX_ITEMS_BEFORE_SCROLLING = 6;

    interface UI extends UiBinder<PopupPanel, MapInfoPanel> {
    }

    public interface Style extends CssResource {

        String contentItemKeyLabel();

        String popup();

        String contentItemValueLabel();

        String contentItem();

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

    public void setItems(List<Pair<String, String>> infoItems) {
        contentPanel.clear();

        int itemMaxFontSizePixels = 18;
        int itemMarginPixels = 8;
        int itemHeightPixels = itemMaxFontSizePixels + (itemMarginPixels*2);
        int totalMaxHeight = itemHeightPixels * MAX_ITEMS_BEFORE_SCROLLING;

        for (Pair<String, String> infoItem : infoItems) {
            FormLabel keyLabel = new FormLabel(
                TextUtil.ellipsize(infoItem.key, 35)
            );
            keyLabel.addStyleName("flex");
            keyLabel.addStyleName(style.contentItemKeyLabel());

            Label valueLabel = new Label(infoItem.value);
            valueLabel.setStyleName(style.contentItemValueLabel());

            FlowPanel itemPanel = new FlowPanel();
            itemPanel.addStyleName("flex-none layout horizontal center");
            itemPanel.addStyleName(style.contentItem());
            itemPanel.add(keyLabel);
            itemPanel.add(valueLabel);
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
}
