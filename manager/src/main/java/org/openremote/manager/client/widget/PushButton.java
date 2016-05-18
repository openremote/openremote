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

import com.google.gwt.user.client.ui.Label;

public class PushButton extends com.google.gwt.user.client.ui.PushButton {

    protected String icon;
    protected Label iconLabel = new Label();

    public PushButton() {
        super();
        getElement().addClassName("or-PushButton");
        getElement().appendChild(iconLabel.getElement());
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
        iconLabel.getElement().removeClassName("or-PushButtonIcon");
        if (icon != null) {
            iconLabel.getElement().addClassName("or-PushButtonIcon fa fa-" + icon);
        }
    }

}
