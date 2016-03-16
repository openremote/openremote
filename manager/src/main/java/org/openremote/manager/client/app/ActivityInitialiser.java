package org.openremote.manager.client.app;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This maps activity managers to parts of the main view
 * Created by Richard on 12/02/2016.
 */
public class ActivityInitialiser {
    @Inject
    public ActivityInitialiser(AppLayout appLayout,
                               @Named("MainContentManager") ActivityManager mainContentActivityManager,
                               @Named("LeftSideManager") ActivityManager leftSideActivityManager
    ) {
        mainContentActivityManager.setDisplay(appLayout.getMainContentPanel());
        leftSideActivityManager.setDisplay(appLayout.getLeftSidePanel());
    }
}
