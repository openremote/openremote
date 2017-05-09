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

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.ComplexPanel;

public class FormLabel extends ComplexPanel {

    protected SpanElement iconElement;
    protected String icon;
    protected LabelElement labelElement;
    protected SpanElement requiredElement;
    protected boolean required;
    protected boolean error;

    public FormLabel() {
        this(null);
    }

    public FormLabel(String text) {
        setElement(Document.get().createElement(DivElement.TAG));
        getElement().addClassName("or-FormLabel layout horizontal center");

        iconElement = (SpanElement) Document.get().createElement(SpanElement.TAG);
        iconElement.addClassName("or-FormLabelIcon");
        iconElement.getStyle().setTextAlign(Style.TextAlign.RIGHT);

        labelElement = (LabelElement) Document.get().createElement(LabelElement.TAG);
        labelElement.setHtmlFor(Document.get().createUniqueId());
        getElement().appendChild(labelElement);

        requiredElement= (SpanElement) Document.get().createElement(SpanElement.TAG);
        requiredElement.addClassName("required");
        requiredElement.setInnerText("*");
        requiredElement.getStyle().setVisibility(Style.Visibility.HIDDEN);
        getElement().appendChild(requiredElement);

        setText(text);
    }

    public String getText() {
        return labelElement.getInnerText();
    }

    public void setText(String text) {
        labelElement.setInnerText(text);
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
        requiredElement.getStyle().setVisibility(required ? Style.Visibility.VISIBLE : Style.Visibility.HIDDEN);
    }

    public void setError(boolean error) {
        this.error = error;
        removeStyleName("error");
        if (error) {
            addStyleName("error");
        }
    }

    public boolean isError() {
        return error;
    }

    public String getFormFieldId() {
        return labelElement.getHtmlFor();
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        if (this.icon != null) {
            iconElement.removeClassName("fa");
            iconElement.removeClassName("fa-" + icon);
        } else {
            getElement().insertBefore(iconElement, labelElement);
        }
        this.icon = icon;
        if (icon != null) {
            iconElement.addClassName("fa");
            iconElement.addClassName("fa-" + icon);
        }
    }

}
