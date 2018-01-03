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

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.openremote.components.client.OpenRemoteApp;
import org.openremote.components.client.rest.REST;
import org.openremote.components.client.AppSecurity;
import org.openremote.components.client.style.WidgetStyle;
import org.openremote.manager.client.event.*;
import org.openremote.manager.client.http.ConstraintViolationReportMapper;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.mvp.AppActivityManager;
import org.openremote.manager.client.mvp.AppPlaceController;
import org.openremote.manager.client.service.EventService;
import org.openremote.manager.client.service.EventServiceImpl;
import org.openremote.manager.shared.security.Tenant;
import org.openremote.manager.shared.security.TenantResource;
import org.openremote.model.event.bus.EventBus;
import org.openremote.model.http.RequestService;

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
    public static AppSecurity getAppSecurity() {
        return getApp().getAppSecurity();
    }

    @Provides
    @Singleton
    public AppActivityManager getActivityManager(ManagerActivityMapper activityMapper, EventBus eventBus) {
        return new AppActivityManager("AppActivityManager", activityMapper, eventBus);
    }

    @Provides
    @Singleton
    public Environment createEnvironment(AppSecurity appSecurity,
                                         RequestService requestService,
                                         EventService eventService,
                                         PlaceController placeController,
                                         PlaceHistoryMapper placeHistoryMapper,
                                         EventBus eventBus,
                                         ManagerMessages managerMessages,
                                         WidgetStyle widgetStyle) {
        return Environment.create(
            appSecurity,
            requestService,
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
    public EventService getEventService(AppSecurity appSecurity,
                                        EventBus eventBus,
                                        SharedEventMapper sharedEventMapper,
                                        SharedEventArrayMapper sharedEventArrayMapper,
                                        EventSubscriptionMapper eventSubscriptionMapper,
                                        CancelEventSubscriptionMapper cancelEventSubscriptionMapper,
                                        UnauthorizedEventSubscriptionMapper unauthorizedEventSubscriptionMapper) {
        EventService eventService = EventServiceImpl.create(
            appSecurity,
            eventBus,
            sharedEventMapper,
            sharedEventArrayMapper,
            eventSubscriptionMapper,
            cancelEventSubscriptionMapper,
            unauthorizedEventSubscriptionMapper
        );
        eventService.connect();
        return eventService;
    }

    @Provides
    @Singleton
    public RequestService getRequestService(AppSecurity appSecurity,
                                            ConstraintViolationReportMapper constraintViolationReportMapper) {
        REST.Configuration.setDefaults(appSecurity.getAuthenticatedRealm());
        return new RequestService(appSecurity::setCredentialsOnRequestParams, constraintViolationReportMapper);
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
    public Tenant getCurrentTenant(Environment environment,
                                   TenantResource tenantResource,
                                   TenantMapper tenantMapper) {
        // Load tenant from server on startup, blocking
        final Tenant[] currentTenant = new Tenant[1];
        environment.getRequestService().sendAndReturn(
            tenantMapper,
            params -> {
                params.setAsync(false);
                tenantResource.get(params, environment.getAppSecurity().getAuthenticatedRealm());
            },
            200,
            tenant -> currentTenant[0] = tenant,
            ex -> {
                environment.getEventBus().dispatch(new ShowFailureEvent(
                    environment.getMessages().errorLoadingTenant(ex.getStatusCode())
                ));
            }
        );
        return currentTenant[0];
    }

    @Provides
    @Singleton
    public native TenantResource getTenantResource()  /*-{
        return $wnd.openremote.REST.TenantResource;
    }-*/;

}
