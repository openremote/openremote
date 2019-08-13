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
package org.openremote.app.client;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.openremote.app.client.event.*;
import org.openremote.app.client.i18n.ManagerMessages;
import org.openremote.app.client.mvp.AppActivityManager;
import org.openremote.app.client.mvp.AppPlaceController;
import org.openremote.app.client.event.EventService;
import org.openremote.app.client.event.EventServiceImpl;
import org.openremote.app.client.style.WidgetStyle;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.event.shared.SharedEvent;
import org.openremote.model.security.TenantResource;

public class ManagerModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(PlaceHistoryMapper.class).to(ManagerHistoryMapper.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    public static native OpenRemoteApp getApp() /*-{
        return $wnd.openremote.INSTANCE;
    }-*/;

    @Provides
    @Singleton
    public AppActivityManager getActivityManager(ManagerActivityMapper activityMapper, EventBus eventBus) {
        return new AppActivityManager("AppActivityManager", activityMapper, eventBus);
    }

    @Provides
    @Singleton
    public Environment createEnvironment(OpenRemoteApp app,
                                         EventService eventService,
                                         PlaceController placeController,
                                         PlaceHistoryMapper placeHistoryMapper,
                                         EventBus eventBus,
                                         ManagerMessages managerMessages,
                                         WidgetStyle widgetStyle) {
        return Environment.create(
            app,
            eventService,
            placeController,
            placeHistoryMapper,
            eventBus,
            managerMessages,
            widgetStyle
        );
    }

    @Provides
    @Singleton
    public PlaceController getPlaceController(EventBus eventBus,
                                              com.google.web.bindery.event.shared.EventBus legacyEventBus,
                                              PlaceController.Delegate delegate) {
        return new AppPlaceController(eventBus, legacyEventBus, delegate);
    }

    @Provides
    @Singleton
    public EventService getEventService(OpenRemoteApp app,
                                        EventBus eventBus,
                                        SharedEventMapper sharedEventMapper,
                                        SharedEventArrayMapper sharedEventArrayMapper,
                                        EventSubscriptionMapper eventSubscriptionMapper,
                                        CancelEventSubscriptionMapper cancelEventSubscriptionMapper,
                                        UnauthorizedEventSubscriptionMapper unauthorizedEventSubscriptionMapper,
                                        TriggeredEventSubscriptionMapper triggeredEventSubscriptionMapper) {
        return new EventServiceImpl(
            app,
            eventBus,
            sharedEventMapper,
            sharedEventArrayMapper,
            eventSubscriptionMapper,
            cancelEventSubscriptionMapper,
            unauthorizedEventSubscriptionMapper,
            triggeredEventSubscriptionMapper
        );
    }

    @Provides
    @Singleton
    public PlaceHistoryHandler getHistoryHandler(PlaceController placeController,
                                                 PlaceHistoryMapper historyMapper,
                                                 com.google.web.bindery.event.shared.EventBus legacyEventBus) {
        PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
        historyHandler.register(placeController, legacyEventBus, Place.NOWHERE); // TODO See AppPlaceController for default start page
        return historyHandler;
    }

    @Provides
    @Singleton
    public native TenantResource getTenantResource()  /*-{
        return $wnd.openremote.REST.TenantResource;
    }-*/;

}
