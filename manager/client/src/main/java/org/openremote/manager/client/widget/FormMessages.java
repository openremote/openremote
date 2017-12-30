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
package org.openremote.manager.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import org.openremote.components.client.style.WidgetStyle;

public class FormMessages extends FlowPanel {

    FlowPanel panel = new FlowPanel();

    public FormMessages(WidgetStyle widgetStyle, boolean success) {
        setStyleName("flex-none layout horizontal");
        addStyleName(widgetStyle.FormMessages());
        addStyleName(success ? "success" : "error");

        IconLabel iconLabel = new IconLabel();
        add(iconLabel);
        iconLabel.addStyleName(widgetStyle.MessagesIcon());
        iconLabel.setIcon(success ? "check" : "warning");

        add(panel);

        setVisible(false);
    }

    @Override
    public void clear() {
        panel.clear();
        setVisible(false);
    }

    public void insertInto(Form form) {
        if (form == null)
            return;
        removeFromParent();
        form.insert(this, 0);
    }

    public void appendMessage(Form form, String message) {
        if (getParent() == null)
            insertInto(form);
        panel.add(new InlineLabel(message));
        panel.getElement().appendChild(Document.get().createBRElement());
        setVisible(true);
    }

}
