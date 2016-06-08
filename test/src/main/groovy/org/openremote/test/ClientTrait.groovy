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
package org.openremote.test

import com.google.gwt.event.logical.shared.ValueChangeHandler
import com.google.gwt.event.shared.HandlerRegistration
import com.google.gwt.place.shared.Place
import com.google.gwt.place.shared.PlaceController
import com.google.gwt.place.shared.PlaceHistoryHandler
import com.google.gwt.place.shared.PlaceHistoryHandler.Historian
import com.google.gwt.place.shared.PlaceHistoryMapper
import com.google.gwt.place.shared.WithTokenizers
import com.google.gwt.user.client.Window
import com.google.gwt.user.client.ui.AcceptsOneWidget
import com.google.web.bindery.event.shared.SimpleEventBus
import org.openremote.manager.client.event.bus.EventBus
import org.openremote.manager.client.mvp.AppActivityManager
import org.openremote.manager.client.mvp.AppActivityMapper
import org.openremote.manager.client.mvp.AppPlaceController
import org.openremote.manager.client.service.SecurityService

trait ClientTrait {

    protected static class MockPlaceControllerDelegate implements PlaceController.Delegate {
        // TODO: Do we need this?
        @Override
        HandlerRegistration addWindowClosingHandler(Window.ClosingHandler handler) {
            return null
        }

        @Override
        boolean confirm(String message) {
            return false
        }
    }

    protected static class MockHistorian implements Historian {
        // TODO: Do we need this?
        @Override
        HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> valueChangeHandler) {
            return null
        }

        @Override
        String getToken() {
            return null
        }

        @Override
        void newItem(String token, boolean issueEvent) {

        }
    }

    static EventBus createEventBus() {
        new EventBus()
    }

    static AppPlaceController createPlaceController(SecurityService securityService, EventBus eventBus) {
        def legacyEventBus = new SimpleEventBus()
        def placeControllerDelegate = new MockPlaceControllerDelegate()
        return new AppPlaceController(securityService, eventBus, legacyEventBus, placeControllerDelegate)

    }

    static PlaceHistoryMapper createPlaceHistoryMapper(WithTokenizers withTokenizers) {
        return new ClientPlaceHistoryMapper(withTokenizers)
    }

    static PlaceHistoryHandler createPlaceHistoryHandler(AppPlaceController placeController, PlaceHistoryMapper placeHistoryMapper, Place defaultPlace) {
        def placeHistoryHandler = new PlaceHistoryHandler(placeHistoryMapper, new MockHistorian());
        placeHistoryHandler.register(placeController, placeController.getLegacyEventBus(), defaultPlace);
        return placeHistoryHandler
    }

    static void startActivityManager(AcceptsOneWidget activityDisplay, AppActivityMapper activityMapper, EventBus eventBus) {
        def activityManager = new AppActivityManager("Test ActivityManager", activityMapper, eventBus)
        activityManager.setDisplay(activityDisplay);
    }
}
