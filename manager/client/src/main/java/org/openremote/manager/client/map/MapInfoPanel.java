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
import com.google.gwt.dom.client.Style;
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

import java.util.List;

public class MapInfoPanel extends AbstractAppPanel {

    interface UI extends UiBinder<PopupPanel, MapInfoPanel> {
    }

    @UiField
    ManagerMessages managerMessages;

    @UiField
    WidgetStyle widgetStyle;

    @UiField
    HTMLPanel panel;

    @UiField
    FlowPanel contentPanel;

    public MapInfoPanel() {
        super(GWT.create(UI.class));
    }

    public void setItems(List<Pair<String, String>> infoItems) {
        contentPanel.clear();

        for (Pair<String, String> infoItem : infoItems) {
            FormLabel keyLabel = new FormLabel(infoItem.key);
            keyLabel.addStyleName("flex");

            Label valueLabel = new Label(infoItem.value);
            valueLabel.getElement().getStyle().setFontSize(1.4, Style.Unit.EM);

            FlowPanel itemPanel = new FlowPanel();
            itemPanel.setStyleName("layout horizontal center");
            itemPanel.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);
            itemPanel.getElement().getStyle().setMargin(0.2, Style.Unit.EM);
            itemPanel.add(keyLabel);
            itemPanel.add(valueLabel);
            contentPanel.add(itemPanel);
        }

        // Show up to 6 items, then start scrolling
        panel.setHeight(Math.min((infoItems.size() * 2.5), 15) + "em");

        // If the panel is already shown, "blink" it so users know there is an update
        if (isShowing()) {
            contentPanel.addStyleName(widgetStyle.HighlightBackground());
            Browser.getWindow().setTimeout(() -> {
                contentPanel.removeStyleName(widgetStyle.HighlightBackground());
            }, 250);
        }
    }
}
