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

import com.google.gwt.user.client.ui.InlineLabel;

public class IconLabel extends InlineLabel {

    protected String icon;

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        if (this.icon != null) {
            removeStyleName("fa");
            removeStyleName("fa-" + this.icon);
        }
        this.icon = icon;
        addStyleName("fa");
        addStyleName("fa-" + icon);
    }

}
