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
package org.openremote.manager.client;

import com.google.gwt.place.shared.PlaceController;
import org.openremote.model.event.bus.EventBus;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.model.event.shared.EventService;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.style.WidgetStyle;

/**
 * Bundle all typically needed dependencies of activities/presenters, so
 * only one thing can be injected into constructors.
 */
public interface Environment {

    static Environment create(SecurityService securityService,
                                     RequestService requestService,
                                     EventService eventService,
                                     PlaceController placeController,
                                     EventBus eventBus,
                                     ManagerMessages managerMessages,
                                     WidgetStyle widgetStyle) {
        return new Environment() {
            @Override
            public SecurityService getSecurityService() {
                return securityService;
            }

            @Override
            public RequestService getRequestService() {
                return requestService;
            }

            @Override
            public EventService getEventService() {
                return eventService;
            }

            @Override
            public PlaceController getPlaceController() {
                return placeController;
            }

            @Override
            public EventBus getEventBus() {
                return eventBus;
            }

            @Override
            public ManagerMessages getMessages() {
                return managerMessages;
            }

            @Override
            public WidgetStyle getWidgetStyle() {
                return widgetStyle;
            }
        };
    }

    SecurityService getSecurityService();

    RequestService getRequestService();

    PlaceController getPlaceController();

    /**
     * Subscribe to, unsubscribe from, and dispatch shared events on the server.
     */
    EventService getEventService();

    /**
     * Register on this bus to listen for local and shared events from the server, and to dispatch local events.
     */
    EventBus getEventBus();

    ManagerMessages getMessages();

    WidgetStyle getWidgetStyle();

}
