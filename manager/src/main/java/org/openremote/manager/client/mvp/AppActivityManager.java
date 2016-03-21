package org.openremote.manager.client.mvp;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.web.bindery.event.shared.UmbrellaException;
import org.openremote.manager.client.event.GoToPlaceEvent;
import org.openremote.manager.client.event.WillGoToPlaceEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
    private class ProtectedDisplay implements AcceptsOneWidget {
        private final AppActivity appActivity;

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

    private static final AppActivity NULL_ACTIVITY = new AppActivity() {
        @Override
        protected void init(Place place) {

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

        Throwable caughtOnStop = null;
        Throwable caughtOnCancel = null;
        Throwable caughtOnStart = null;

        if (nextActivity == null) {
            nextActivity = NULL_ACTIVITY;
        }

        if (currentActivity.equals(nextActivity)) {
            if (LOG.isLoggable(Level.FINE))
                LOG.fine(name + " - activity unchanged");
            return;
        }

        if (startingNext) {
            // The place changed again before the new current activity showed its
            // widget
            caughtOnCancel = tryStopOrCancel(false);
            currentActivity = NULL_ACTIVITY;
            startingNext = false;
        } else if (!currentActivity.equals(NULL_ACTIVITY)) {
            showWidget(null);
          /*
           * Kill off the activity's handlers, so it doesn't have to worry about
           * them accidentally firing as a side effect of its tear down
           */
            if (LOG.isLoggable(Level.FINE))
                LOG.fine(name + " - removing current activity registrations: " + activityRegistrations);
            eventBus.removeAll(activityRegistrations);
            activityRegistrations.clear();

            caughtOnStop = tryStopOrCancel(true);
        }

        currentActivity = nextActivity;

        if (currentActivity.equals(NULL_ACTIVITY)) {
            showWidget(null);
        } else {
            startingNext = true;
            caughtOnStart = tryStart();
        }

        if (caughtOnStart != null || caughtOnCancel != null || caughtOnStop != null) {
            Set<Throwable> causes = new LinkedHashSet<Throwable>();
            if (caughtOnStop != null) {
                causes.add(caughtOnStop);
            }
            if (caughtOnCancel != null) {
                causes.add(caughtOnCancel);
            }
            if (caughtOnStart != null) {
                causes.add(caughtOnStart);
            }

            throw new UmbrellaException(causes);
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

    private AppActivity getNextActivity(Place newPlace) {
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

    private void showWidget(IsWidget view) {
        if (display != null) {
            display.setWidget(view);
        }
    }

    private Throwable tryStart() {
        Throwable caughtOnStart = null;
        try {
      /*
       * Wrap the actual display with a per-call instance that protects the
       * display from canceled or stopped activities, and which maintains our
       * startingNext state.
       */
            if (LOG.isLoggable(Level.FINE))
                LOG.fine(name + " - starting activity: " + currentActivity);
            currentActivity.start(new ProtectedDisplay(currentActivity), eventBus, activityRegistrations);
        } catch (Throwable t) {
            caughtOnStart = t;
        }
        return caughtOnStart;
    }

    private Throwable tryStopOrCancel(boolean stop) {
        Throwable caughtOnStop = null;
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
        } catch (Throwable t) {
            caughtOnStop = t;
        } finally {
            // Activity might have added registrations in onStop or onCancel
            if (!activityRegistrations.isEmpty()) {
                if (LOG.isLoggable(Level.FINE))
                    LOG.fine(name + " - removing current activity registrations: " + activityRegistrations);
                eventBus.removeAll(activityRegistrations);
                activityRegistrations.clear();
            }
        }
        return caughtOnStop;
    }
}