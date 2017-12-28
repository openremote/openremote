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

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;

public class Hyperlink extends com.google.gwt.user.client.ui.Hyperlink {

    protected static HyperlinkImpl impl = GWT.create(HyperlinkImpl.class);

    protected String icon;
    protected Label iconLabel = new Label();
    protected Runnable simpleClickHandler;

    public Hyperlink() {
        this(null, null);
    }

    public Hyperlink(String icon, String text) {
        super();

        getElement().addClassName("or-Hyperlink");
        Element anchor = getElement().getFirstChildElement();
        // TODO Clickable icon?!
        DOM.insertBefore(getElement(), iconLabel.getElement(), anchor);

        setIcon(icon);
        setText(text);
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
        iconLabel.getElement().removeClassName("or-HyperlinkIcon");
        if (icon != null) {
            iconLabel.getElement().addClassName("or-HyperlinkIcon fa fa-" + icon);
        }
    }

    public Runnable getSimpleClickHandler() {
        return simpleClickHandler;
    }

    public void setSimpleClickHandler(Runnable simpleClickHandler) {
        this.simpleClickHandler = simpleClickHandler;
    }

    @Override
    public void onBrowserEvent(Event event) {
        if (simpleClickHandler != null && DOM.eventGetType(event) == Event.ONCLICK && impl.handleAsClick(event)) {
            simpleClickHandler.run();
            event.preventDefault();
        } else {
            super.onBrowserEvent(event);
        }
    }
}
