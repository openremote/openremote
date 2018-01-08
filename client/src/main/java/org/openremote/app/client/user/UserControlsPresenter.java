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
package org.openremote.app.client.user;

import com.google.inject.Inject;
import org.openremote.app.client.Environment;
import org.openremote.app.client.ManagerHistoryMapper;

public class UserControlsPresenter implements UserControls.Presenter {

    final protected Environment environment;
    final protected ManagerHistoryMapper managerHistoryMapper;
    final protected UserControls view;

    @Inject
    public UserControlsPresenter(Environment environment,
                                 ManagerHistoryMapper managerHistoryMapper,
                                 UserControls view) {
        this.environment = environment;
        this.managerHistoryMapper = managerHistoryMapper;
        this.view = view;

        view.setPresenter(this);

        updateView();

    }

    @Override
    public UserControls getView() {
        return view;
    }

    @Override
    public void doLogout() {
        environment.getApp().getSecurity().logout();
    }

    protected void updateView() {
        view.setUserDetails(
            environment.getApp().getSecurity().getUser(),
            environment.getApp().getSecurity().getFullName(),
            managerHistoryMapper.getToken(new UserAccountPlace()),
            environment.getApp().getSecurity().hasResourceRole("manage-account", "account")
                && environment.getApp().getSecurity().isUserTenantAdminEnabled()
        );
    }

}
