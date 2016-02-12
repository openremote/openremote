package org.openremote.manager.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.openremote.manager.client.view.MainView;

/**
 * This maps activity managers to parts of the main view
 * Created by Richard on 12/02/2016.
 */
public class ActivityInitialiser {
    @Inject
    public ActivityInitialiser(MainView mainView,
            @Named("MainContentManager")ActivityManager mainContentActivityManager) {
        mainContentActivityManager.setDisplay(mainView.getMainContentPanel());
    }
}
