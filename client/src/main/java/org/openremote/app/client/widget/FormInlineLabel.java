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
package org.openremote.app.client.widget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.user.client.ui.ComplexPanel;

public class FormInlineLabel extends ComplexPanel {

    protected SpanElement textElement;
    protected SpanElement iconElement;
    protected String icon;

    public FormInlineLabel() {
        this(null);
    }

    public FormInlineLabel(String text) {
        setElement(Document.get().createElement(SpanElement.TAG));
        getElement().setClassName("or-FormInlineLabel");

        textElement = (SpanElement) Document.get().createElement(SpanElement.TAG);
        getElement().appendChild(textElement);

        iconElement = (SpanElement) Document.get().createElement(SpanElement.TAG);

        setText(text);
    }

    public String getIcon() {
        return icon;
    }

    public void setText(String text) {
        textElement.setInnerText(text);
    }

    public String getText() {
        return textElement.getInnerText();
    }

    public void setIcon(String icon) {
        if (this.icon != null) {
            iconElement.removeClassName("or-Icon");
            iconElement.removeClassName("fa");
            iconElement.removeClassName("fa-" + this.icon);
        } else {
            getElement().insertBefore(iconElement, textElement);
        }
        this.icon = icon;
        if (icon != null) {
            iconElement.addClassName("or-Icon");
            iconElement.addClassName("fa");
            iconElement.addClassName("fa-" + icon);
        }
    }

}
