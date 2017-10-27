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

import com.google.gwt.user.client.ui.TextArea;

public class FormTextArea extends TextArea {

    boolean spellcheck;
    String wrap = "soft";

    public FormTextArea() {
        super();
        setStyleName("flex or-FormControl or-FormTextArea");
    }

    public boolean isSpellcheck() {
        return spellcheck;
    }

    public void setSpellcheck(boolean spellcheck) {
        getElement().setPropertyBoolean("spellcheck", spellcheck);
    }

    public String getWrap() {
        return wrap;
    }

    public void setWrap(String wrap) {
        this.wrap = (wrap != null ? wrap : "soft");
        getElement().setPropertyString("wrap", wrap);
    }

    public void setOpaque(boolean opaque) {
        removeStyleName("opaque");
        if (opaque) {
            addStyleName("opaque");
        }
    }

    public void setResizable(boolean resizable) {
        getElement().getStyle().setProperty("resize", resizable ? "auto" : "none");
    }

    public void setBorder(boolean border) {
        getElement().removeClassName("border");
        if (border)
            getElement().addClassName("border");
    }

}