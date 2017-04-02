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

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.admin.navigation.AdminNavigation;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;

import java.util.Collection;

public abstract class AbstractAdminActivity<P extends AdminPlace, AC extends AdminContent>
        extends AppActivity<P> {

    final protected AdminView adminView;
    final protected AdminNavigation.Presenter adminNavigationPresenter;
    final protected AC adminContent;

    public AbstractAdminActivity(AdminView adminView,
                                 AdminNavigation.Presenter adminNavigationPresenter,
                                 AC adminContent) {
        this.adminView = adminView;
        this.adminNavigationPresenter = adminNavigationPresenter;
        this.adminContent = adminContent;
    }

    @Override
    protected AppActivity<P> init(P place) {
        adminNavigationPresenter.setActivePlace(place);
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        container.setWidget(adminView.asWidget());
        adminView.setContent(adminContent);
    }

    @Override
    public void onStop() {
        adminView.setContent(null);
        super.onStop();
    }
}
