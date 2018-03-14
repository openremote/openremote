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
package org.openremote.app.client.admin.syslog;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.model.syslog.SyslogEvent;

public class SyslogItems extends FlowPanel {

    final protected IsWidget emptyLabel;
    boolean empty = true;

    public SyslogItems(IsWidget emptyLabel) {
        this.emptyLabel = emptyLabel;
        setStyleName("flex layout vertical or-MainContent or-FormList");
        getElement().getStyle().setOverflow(Style.Overflow.AUTO);
        add(emptyLabel);
    }

    public void addItem(SyslogEvent event) {
        if (empty)
            remove(emptyLabel);
        empty = false;
        getElement().appendChild(new SyslogItem(event).getElement());
        scrollToBottom();
    }

    public int getItemCount() {
        return getElement().getChildCount();
    }

    public void removeFirstItem() {
        if (getItemCount() > 0) {
            getElement().removeChild(getElement().getFirstChild());
        }
        if (getItemCount() == 0) {
            empty = true;
            add(emptyLabel);
        }
    }

    @Override
    public void clear() {
        super.clear();
        empty = true;
        add(emptyLabel);
    }

    protected void scrollToBottom() {
        getElement().setScrollTop(getElement().getScrollHeight());
    }

}
