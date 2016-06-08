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

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.inject.Inject;
import org.openremote.manager.client.admin.AdminPlace;
import org.openremote.manager.client.admin.overview.AdminOverviewPlace;
import org.openremote.manager.client.admin.tenant.AdminTenantsPlace;
import org.openremote.manager.client.admin.users.AdminUsersPlace;

public class AdminNavigationPresenter implements AdminNavigation.Presenter {

    final protected AdminNavigation view;
    final protected PlaceHistoryMapper historyMapper;

    @Inject
    public AdminNavigationPresenter(AdminNavigation view,
                                    PlaceHistoryMapper historyMapper) {
        this.view = view;
        this.historyMapper = historyMapper;

        view.setPresenter(this);
    }

    @Override
    public AdminNavigation getView() {
        return view;
    }

    @Override
    public String getAdminOverviewPlaceToken() {
        return historyMapper.getToken(new AdminOverviewPlace());
    }

    @Override
    public String getAdminTenantsPlaceToken() {
        return historyMapper.getToken(new AdminTenantsPlace());
    }

    @Override
    public String getAdminUsersPlaceToken() {
        return historyMapper.getToken(new AdminUsersPlace());
    }

    @Override
    public void setActivePlace(AdminPlace place) {
        view.onPlaceChange(place);
    }
}
