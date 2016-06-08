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
package org.openremote.manager.client.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import org.openremote.manager.client.admin.navigation.AdminNavigation;

import javax.inject.Inject;

public class AdminViewImpl extends Composite implements AdminView {

    interface UI extends UiBinder<HTMLPanel, AdminViewImpl> {
    }

    @UiField(provided = true)
    final AdminNavigation adminNavigation;

    @UiField
    SimplePanel adminContentContainer;

    @Inject
    public AdminViewImpl(AdminNavigation adminNavigation) {
        this.adminNavigation = adminNavigation;

        UI ui = GWT.create(UI.class);
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setContent(AdminContent adminContent) {
        adminContentContainer.clear();
        if (adminContent != null)
            adminContentContainer.add(adminContent);
    }
}
