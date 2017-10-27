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

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class SecondaryNavigation extends FlowPanel {

    public SecondaryNavigation() {
        setHorizontal();
    }

    protected void setHorizontal() {
        setStyleName("layout horizontal center end-justified or-SecondaryNav");
    }

    public void setVertical(boolean vertical) {
        if (vertical) {
            setStyleName("layout vertical or-SecondaryNav");
        } else {
            setHorizontal();
        }
    }

    @Override
    public void add(Widget w) {
        super.add(w);
        w.addStyleName("or-SecondaryNavItem");
    }

    protected void reset(Hyperlink hyperlink) {
        hyperlink.removeStyleName("active");
        hyperlink.setVisible(false);
        hyperlink.setTargetHistoryToken("");
    }
}
