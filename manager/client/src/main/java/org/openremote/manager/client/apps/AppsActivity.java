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
package org.openremote.manager.client.apps;

import org.openremote.manager.client.Environment;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.mvp.AcceptsView;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.apps.ConsoleApp;
import org.openremote.manager.shared.apps.ConsoleAppResource;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.bus.EventRegistration;

import javax.inject.Inject;
import java.util.Collection;

import static org.openremote.manager.client.http.RequestExceptionHandler.handleRequestException;

public class AppsActivity
    extends AppActivity<AppsPlace>
    implements ConsoleAppsView.Presenter {

    final ConsoleAppsView view;
    final Environment environment;
    final ConsoleAppResource consoleAppResource;
    final ConsoleAppArrayMapper consoleAppArrayMapper;

    @Inject
    public AppsActivity(Environment environment,
                        ConsoleAppsView view,
                        ConsoleAppResource consoleAppResource,
                        ConsoleAppArrayMapper consoleAppArrayMapper) {
        this.environment = environment;
        this.view = view;
        this.consoleAppResource = consoleAppResource;
        this.consoleAppArrayMapper = consoleAppArrayMapper;
    }

    @Override
    protected AppActivity<AppsPlace> init(AppsPlace place) {
        return this;
    }

    @Override
    public void start(AcceptsView container, EventBus eventBus, Collection<EventRegistration> registrations) {
        container.setViewComponent(view);
        view.setPresenter(this);

        environment.getRequestService().execute(
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
            },
            ex -> handleRequestException(ex, environment)
        );
    }

    @Override
    public ManagerMessages messages() {
        return environment.getMessages();
    }

    @Override
    public void onAppSelected(String realm) {
        environment.getPlaceController().goTo(new AppsPlace(realm));
    }
}
