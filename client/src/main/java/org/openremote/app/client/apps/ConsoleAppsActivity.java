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
package org.openremote.app.client.apps;

import org.openremote.app.client.Environment;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.mvp.AcceptsView;
import org.openremote.app.client.mvp.AppActivity;
import org.openremote.model.apps.ConsoleApp;
import org.openremote.model.apps.ConsoleAppResource;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.Collection;

public class ConsoleAppsActivity
    extends AppActivity<ConsoleAppsPlace>
    implements ConsoleAppsView.Presenter {

    final ConsoleAppsView view;
    final Environment environment;
    final ConsoleAppResource consoleAppResource;
    final ConsoleAppArrayMapper consoleAppArrayMapper;

    @Inject
    public ConsoleAppsActivity(Environment environment,
                               ConsoleAppsView view,
                               ConsoleAppResource consoleAppResource,
                               ConsoleAppArrayMapper consoleAppArrayMapper) {
        this.environment = environment;
        this.view = view;
        this.consoleAppResource = consoleAppResource;
        this.consoleAppArrayMapper = consoleAppArrayMapper;
    }

    @Override
    protected AppActivity<ConsoleAppsPlace> init(ConsoleAppsPlace place) {
        return this;
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        container.setViewComponent(view);
        view.setPresenter(this);

        environment.getApp().getRequestService().sendAndReturn(
            consoleAppArrayMapper,
            consoleAppResource::getInstalledApps,
            200,
            apps -> {
                view.setApps(apps);
                if (getPlace().getRealm() != null) {
                    for (ConsoleApp app : apps) {
                        if (app.getTenant().getRealm().equals(getPlace().getRealm())) {
                            view.openAppUrl(app.getTenant().getRealm(), app.getUrl());
                            environment.getEventBus().dispatch(new ConsoleAppSelection(app.getTenant().getRealm()));
                            break;
                        }
                    }
                }
            }
        );
    }

    @Override
    public ManagerMessages messages() {
        return environment.getMessages();
    }

    @Override
    public void onAppSelected(String realm) {
        environment.getPlaceController().goTo(new ConsoleAppsPlace(realm));
    }
}
