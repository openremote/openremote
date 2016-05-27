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

import com.google.inject.Inject;
import org.openremote.manager.client.ManagerHistoryMapper;
import org.openremote.manager.client.admin.AdminPlace;
import org.openremote.manager.client.admin.overview.AdminOverviewPlace;
import org.openremote.manager.client.admin.tenant.AdminTenantsPlace;
import org.openremote.manager.client.admin.users.AdminUsersPlace;

public class AdminNavigationPresenter implements AdminNavigation.Presenter {

    final protected AdminNavigation view;
    final protected ManagerHistoryMapper managerHistoryMapper;

    @Inject
    public AdminNavigationPresenter(AdminNavigation view,
                                    ManagerHistoryMapper managerHistoryMapper) {
        this.view = view;
        this.managerHistoryMapper = managerHistoryMapper;

        view.setPresenter(this);
    }

    @Override
    public AdminNavigation getView() {
        return view;
    }

    @Override
    public String getAdminOverviewPlaceToken() {
        return managerHistoryMapper.getToken(new AdminOverviewPlace());
    }

    @Override
    public String getAdminTenantsPlaceToken() {
        return managerHistoryMapper.getToken(new AdminTenantsPlace());
    }

    @Override
    public String getAdminUsersPlaceToken(String userId) {
        return managerHistoryMapper.getToken(new AdminUsersPlace(userId));
    }

    @Override
    public void setActivePlace(AdminPlace place) {
        view.onPlaceChange(place);
    }
}
