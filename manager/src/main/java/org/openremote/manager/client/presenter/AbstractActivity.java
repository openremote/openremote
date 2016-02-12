package org.openremote.manager.client.presenter;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.Place;

/**
 * Created by Richard on 11/02/2016.
 */
public abstract class AbstractActivity<T extends Place> extends com.google.gwt.activity.shared.AbstractActivity {
    protected abstract void init(T place);

    public Activity doInit(T place) {
        init(place);
        return this;
    }
}
