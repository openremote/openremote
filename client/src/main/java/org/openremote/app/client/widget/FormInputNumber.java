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

import com.google.gwt.dom.client.InputElement;
import com.google.gwt.user.client.ui.TextBoxBase;

public class FormInputNumber extends TextBoxBase {

    public FormInputNumber() {
        super(createInput());
        setStyleName("or-FormControl or-FormInputNumber");
    }

    private static native InputElement createInput() /*-{
        var input = document.createElement("input");
        input.setAttribute("type", "number");
        return input;
    }-*/;

    public void setMin(long min) {
        getElement().setAttribute("min", Long.toString(min));
    }

    public void setMax(long max) {
        getElement().setAttribute("max", Long.toString(max));
    }

    public void setStep(double step) {
        getElement().setAttribute("step", Double.toString(step));
    }
}