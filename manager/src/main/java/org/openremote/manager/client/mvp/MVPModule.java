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
package org.openremote.manager.client.mvp;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.place.shared.PlaceController;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.openremote.manager.client.ManagerActivityMapper;
import org.openremote.manager.client.event.EventMapper;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.service.EventService;
import org.openremote.manager.client.service.EventServiceImpl;
import org.openremote.manager.client.service.SecurityService;

public class MVPModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(com.google.web.bindery.event.shared.EventBus.class).to(com.google.web.bindery.event.shared.SimpleEventBus.class).in(Singleton.class);
        bind(EventBus.class).in(Singleton.class);
    }



    @Provides
    @Singleton
    public AppActivityManager getAppActivityMapper(ManagerActivityMapper activityMapper, EventBus eventBus) {
        return new AppActivityManager("AppActivityManager", activityMapper, eventBus);
    }

    @Provides
    @Singleton
    public EventService getEventBus(SecurityService securityService, EventBus eventBus, EventMapper eventMapper) {
        EventService eventService = EventServiceImpl.create(securityService, eventBus, eventMapper);
        eventService.connect();
        return eventService;
    }

    @Provides
    @Singleton
    public PlaceController getPlaceController(SecurityService securityService,
                                              EventService eventService,
                                              EventBus eventBus,
                                              com.google.web.bindery.event.shared.EventBus legacyEventBus) {
        return new AppPlaceController(securityService, eventBus, legacyEventBus);
    }


}
