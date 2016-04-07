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

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import org.openremote.manager.client.event.GoToPlaceEvent;
import org.openremote.manager.client.event.WillGoToPlaceEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unfortunately a re-implementation of the very very private class ActivityManager to get rid of legacy event bus.
 */
public class AppActivityManager {

    private static final Logger LOG = Logger.getLogger(AppActivityManager.class.getName());

    /**
     * Wraps our real display to prevent an Activity from taking it over if it is
     * not the currentActivity.
     */
    protected class ProtectedDisplay implements AcceptsOneWidget {
        protected final AppActivity appActivity;

        ProtectedDisplay(AppActivity appActivity) {
            this.appActivity = appActivity;
        }

        public void setWidget(IsWidget view) {
            if (this.appActivity == AppActivityManager.this.currentActivity) {
                startingNext = false;
                showWidget(view);
            }
        }
    }

    protected static final AppActivity NULL_ACTIVITY = new AppActivity() {

        @Override
        public AppActivity init(Place place) {
            return this;
        }

        @Override
        public void start(AcceptsOneWidget container, EventBus eventBus, Collection collection) {
        }
    };

    protected final String name;
    protected final AppActivityMapper mapper;
    protected final EventBus eventBus;
    protected final Set<EventRegistration> managerRegistrations = new HashSet<>();
    protected final Set<EventRegistration> activityRegistrations = new HashSet<>();
    protected AppActivity currentActivity = NULL_ACTIVITY;
    protected AcceptsOneWidget display;
    protected boolean startingNext = false;

    public AppActivityManager(String name, AppActivityMapper mapper, EventBus eventBus) {
        this.name = name;
        this.mapper = mapper;
        this.eventBus = eventBus;
    }

    /**
     * Deactivate the current activity, find the next one from our ActivityMapper,
     * and start it.
     * <p>
     * The current activity's widget will be hidden immediately, which can cause
     * flicker if the next activity provides its widget asynchronously. That can
     * be minimized by decent caching. Perenially slow activities might mitigate
     * this by providing a widget immediately, with some kind of "loading"
     * treatment.
     */
    public void onPlaceChange(Place newPlace) {
        if (LOG.isLoggable(Level.FINE))
            LOG.fine(name + " - on place change: " + newPlace);

        AppActivity nextActivity = getNextActivity(newPlace);

        if (LOG.isLoggable(Level.FINE))
            LOG.fine(name + " - next activity is: " + nextActivity);

        if (nextActivity == null) {
            nextActivity = NULL_ACTIVITY;
        }

        if (currentActivity.equals(nextActivity)) {
            if (LOG.isLoggable(Level.FINE))
                LOG.fine(name + " - activity unchanged");
            return;
        } else {
            if (LOG.isLoggable(Level.FINE))
                LOG.fine(name + " - activity changed, transitioning");
        }

        if (startingNext) {
            // The place changed again before the new current activity showed its
            // widget
            tryStopOrCancel(false);
            currentActivity = NULL_ACTIVITY;
            startingNext = false;
        } else if (!currentActivity.equals(NULL_ACTIVITY)) {
            showWidget(null);
            if (LOG.isLoggable(Level.FINE))
                LOG.fine(name + " - removing current activity registrations: " + activityRegistrations);
            eventBus.removeAll(activityRegistrations);
            activityRegistrations.clear();
            tryStopOrCancel(true);
        }

        currentActivity = nextActivity;

        if (currentActivity.equals(NULL_ACTIVITY)) {
            showWidget(null);
        } else {
            startingNext = true;
            tryStart();
        }
    }

    /**
     * Sets the display for the receiver, and has the side effect of starting or
     * stopping its monitoring the event bus for place change events.
     * <p>
     * If you are disposing of an ActivityManager, it is important to call
     * setDisplay(null) to get it to deregister from the event bus, so that it can
     * be garbage collected.
     *
     * @param display an instance of AcceptsOneWidget
     */
    public void setDisplay(AcceptsOneWidget display) {
        boolean wasActive = (null != this.display);
        boolean willBeActive = (null != display);
        this.display = display;
        if (wasActive != willBeActive) {
            if (willBeActive) {
                managerRegistrations.add(eventBus.register(
                    GoToPlaceEvent.class,
                    event -> onPlaceChange(event.getNewPlace()))
                );
                // Reject the place change if the current activity is not willing to stop.
                managerRegistrations.add(eventBus.register(
                    WillGoToPlaceEvent.class,
                    event -> event.setWarning(currentActivity.mayStop()))
                );
            } else {
                eventBus.removeAll(managerRegistrations);
                managerRegistrations.clear();
            }
        }
    }

    protected AppActivity getNextActivity(Place newPlace) {
        if (display == null) {
      /*
       * Display may have been nulled during PlaceChangeEvent dispatch. Don't
       * bother the mapper, just return a null to ensure we shut down the
       * current activity
       */
            return null;
        }
        return mapper.getActivity(newPlace);
    }

    protected void showWidget(IsWidget view) {
        if (display != null) {
            display.setWidget(view);
        }
    }

    protected void tryStart() {
        Throwable caughtOnStart = null;
        if (LOG.isLoggable(Level.FINE))
            LOG.fine(name + " - starting activity: " + currentActivity);
        currentActivity.start(new ProtectedDisplay(currentActivity), eventBus, activityRegistrations);
    }

    protected void tryStopOrCancel(boolean stop) {
        try {
            if (stop) {
                if (LOG.isLoggable(Level.FINE))
                    LOG.fine(name + " - stopping current activity: " + currentActivity);
                currentActivity.onStop();
            } else {
                if (LOG.isLoggable(Level.FINE))
                    LOG.fine(name + " - cancelling current activity: " + currentActivity);
                currentActivity.onCancel();
            }
        } finally {
            // Activity might have added registrations in onStop or onCancel
            if (!activityRegistrations.isEmpty()) {
                if (LOG.isLoggable(Level.FINE))
                    LOG.fine(name + " - removing current activity registrations: " + activityRegistrations);
                eventBus.removeAll(activityRegistrations);
                activityRegistrations.clear();
            }
        }
    }
}