package org.openremote.manager.client.mvp;

import com.google.gwt.place.shared.Place;

/**
 * Finds the activity to run for a given {@link Place}, used to configure
 * an {@link AppActivityManager}.
 */
public interface AppActivityMapper {
    /**
     * Returns the activity to run for the given {@link Place}, or null.
     *
     * @param place a Place object
     */
    AppActivity getActivity(Place place);
}
