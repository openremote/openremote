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

import org.openremote.manager.client.Environment;
import org.openremote.manager.client.mvp.AcceptsView;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.Collection;

public class UserAccountActivity extends AppActivity<UserAccountPlace> implements UserAccountView.Presenter {

    final protected Environment environment;
    final UserAccountView view;

    @Inject
    public UserAccountActivity(Environment environment,
                               UserAccountView view) {
        this.environment = environment;
        this.view = view;
    }

    @Override
    protected AppActivity<UserAccountPlace> init(UserAccountPlace place) {
        return this;
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        view.setRealm(environment.getAppSecurity().getAuthenticatedRealm());
        container.setWidget(view.asWidget());
    }
}
