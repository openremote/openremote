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

import com.fasterxml.jackson.databind.ObjectMapper
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
import elemental.json.JsonValue
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget
import org.openremote.model.event.Event
import org.openremote.model.event.bus.EventBus
import org.openremote.manager.client.mvp.AppActivityManager
import org.openremote.manager.client.mvp.AppActivityMapper
import org.openremote.manager.client.mvp.AppPlaceController
import org.openremote.manager.client.service.SecurityService
import org.openremote.manager.shared.http.Request
import org.openremote.manager.shared.http.RequestParams
import org.openremote.manager.shared.http.SuccessStatusCode
import org.openremote.model.event.bus.EventListener
import org.openremote.test.GwtClientTrait.CollectingEventListener
import org.spockframework.mock.IMockMethod

import javax.ws.rs.ClientErrorException
import javax.ws.rs.Produces
import javax.ws.rs.core.Response
import java.lang.reflect.Method

import static javax.ws.rs.core.MediaType.APPLICATION_JSON

trait GwtClientTrait {

    static class UnsupportedXMLHttpRequest implements Request.XMLHttpRequest {
        @Override
        String getResponseHeader(String header) {
            throw new UnsupportedOperationException("Can't access response headers in test framework")
        }
    }

    static class ResponseWrapperXMLHttpRequest implements Request.XMLHttpRequest {
        final Response response

        ResponseWrapperXMLHttpRequest(Response response) {
            this.response = response
        }

        @Override
        String getResponseHeader(String header) {
            return response.getHeaderString(header)
        }
    }

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

    static class CollectingEventListener implements EventListener<Event> {
        List[] collectedEvents

        CollectingEventListener(List[] collectedEvents) {
            this.collectedEvents = collectedEvents
        }

        @Override
        void on(Event event) {
            collectedEvents += event
        }
    }

    static EventBus createEventBus(List[] collectedEvents) {
        def eventBus = new EventBus()
        eventBus.register(null, new CollectingEventListener(collectedEvents))
        return eventBus
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

    // This emulates how a GWT client calls a REST service, we intercept and route through Resteasy Client proxy
    static void callResourceProxy(ObjectMapper jsonMapper, ResteasyWebTarget clientTarget, mockInvocation) {
        // If the first parameter of the method we want to call is RequestParams
        List<Object> args = mockInvocation.getArguments()
        if (!(args[0] instanceof RequestParams)) {
            throw new UnsupportedOperationException("Don't know how to handle service API: " + mockInvocation.getMethod)
        }

        RequestParams requestParams = (RequestParams) args[0]
        IMockMethod mockMethod = mockInvocation.getMethod()

        // Get a Resteasy client proxy for the resource
        Class mockedResourceType = mockInvocation.getMockObject().getType()
        Method mockedResourceMethod = mockedResourceType.getDeclaredMethod(
                mockMethod.name,
                mockMethod.parameterTypes.toArray(new Class[mockMethod.parameterTypes.size()])
        )
        def resourceProxy = clientTarget.proxy(mockedResourceType)

        // Try to find out what the expected success status code is
        SuccessStatusCode successStatusCode =
                mockedResourceMethod.getDeclaredAnnotation(SuccessStatusCode.class)
        int statusCode = successStatusCode != null ? successStatusCode.value() : 200

        // Call the proxy
        Object result
        boolean resultPassthrough = false
        // Fake an XMLHttpRequest for later (we want to access headers if there is an exception...)
        Request.XMLHttpRequest xmlHttpRequest = new UnsupportedXMLHttpRequest()
        try {
            result = resourceProxy."$mockMethod.name"(args)
        } catch (ClientErrorException ex) {
            statusCode = ex.getResponse().getStatus()
            xmlHttpRequest = new ResponseWrapperXMLHttpRequest(ex.getResponse())
            try {
                result = ex.getResponse().readEntity(String.class)
            } catch (IllegalStateException ise) {
                // Ignore, this happens when the response is closed already
            }
            resultPassthrough = true
        }

        String responseText = null

        if (result != null) {
            // If the method produces JSON, we need to turn whatever the proxy delivered back into JSON string
            Produces producesAnnotation = mockedResourceMethod.getDeclaredAnnotation(Produces.class)
            if (!resultPassthrough
                    && producesAnnotation != null
                    && Arrays.asList(producesAnnotation.value()).contains(APPLICATION_JSON)) {
                // Handle elemental JsonValue special, don't use Jackson
                if (result instanceof JsonValue) {
                    JsonValue jsonValue = (JsonValue) result
                    responseText = jsonValue.toJson()
                } else {
                    responseText = jsonMapper.writeValueAsString(result)
                }
            } else {
                responseText = result.toString()
            }
        }

        // Pass the result to the callback, so it looks asynchronous for client code
        requestParams.callback.call(statusCode, xmlHttpRequest, responseText)
    }
}
