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
package org.openremote.manager.client.admin.navigation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.LIElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import org.openremote.manager.client.admin.AdminPlace;
import org.openremote.manager.client.admin.realms.AdminRealmPlace;
import org.openremote.manager.client.admin.realms.AdminRealmsPlace;
import org.openremote.manager.client.admin.users.AdminUsersPlace;
import org.openremote.manager.client.widget.Hyperlink;

import javax.inject.Inject;

public class AdminNavigationImpl extends Composite implements AdminNavigation {

    interface UI extends UiBinder<HTMLPanel, AdminNavigationImpl> {
    }

    private UI ui = GWT.create(UI.class);

    Presenter presenter;

    @UiField
    LIElement overview;
    @UiField
    Hyperlink overviewLink;

    @UiField
    LIElement realmsItem;
    @UiField
    Hyperlink realmsLink;

    @UiField
    LIElement usersItem;
    @UiField
    Hyperlink usersLink;

    @Inject
    public AdminNavigationImpl() {
        initWidget(ui.createAndBindUi(this));
    }

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;

        overviewLink.setTargetHistoryToken(presenter.getAdminOverviewPlaceToken());
        realmsLink.setTargetHistoryToken(presenter.getAdminRealmsPlaceToken());
        usersLink.setTargetHistoryToken(presenter.getAdminUsersPlaceToken(""));
    }

    @Override
    public void onPlaceChange(AdminPlace adminPlace) {
        overviewLink.removeStyleName("active");
        realmsLink.removeStyleName("active");
        usersLink.removeStyleName("active");

        if (adminPlace instanceof AdminRealmsPlace) {
            realmsLink.addStyleName("active");
        } else if (adminPlace instanceof AdminRealmPlace) {
            realmsLink.addStyleName("active");
        } else if (adminPlace instanceof AdminUsersPlace) {
            usersLink.addStyleName("active");
        } else {
            overviewLink.addStyleName("active");
        }
    }

}
