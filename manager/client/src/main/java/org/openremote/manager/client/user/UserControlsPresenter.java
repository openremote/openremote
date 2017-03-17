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
package org.openremote.manager.client.user;

import com.google.inject.Inject;
import org.openremote.manager.client.Environment;
import org.openremote.manager.client.ManagerHistoryMapper;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.event.ShowFailureEvent;

import java.util.logging.Logger;

public class UserControlsPresenter implements UserControls.Presenter {

    private static final Logger LOG = Logger.getLogger(UserControlsPresenter.class.getName());

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

        environment.getEventBus().register(UserChangeEvent.class, event -> {
            if (event.getUsername() == null) {
                environment.getEventBus().dispatch(
                    new ShowFailureEvent(environment.getMessages().sessionTimedOut())
                );
            } else {
                updateView();
            }
        });
    }

    @Override
    public UserControls getView() {
        return view;
    }

    @Override
    public void doLogout() {
        environment.getSecurityService().logout();
    }

    protected void updateView() {
        view.setUserDetails(
            environment.getSecurityService().getParsedToken().getPreferredUsername(),
            environment.getSecurityService().getParsedToken().getName(),
            managerHistoryMapper.getToken(new UserAccountPlace()),
            environment.getSecurityService().hasResourceRole("manage-account", "account")
        );
    }

}
