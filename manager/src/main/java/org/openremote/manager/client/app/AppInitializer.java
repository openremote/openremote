package org.openremote.manager.client.app;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.openremote.manager.client.mvp.AppActivityManager;

public class AppInitializer {

    @Inject
    public AppInitializer(AppLayout appLayout,
                          @Named("MainContentManager") AppActivityManager mainContentActivityManager,
                          @Named("LeftSideManager") AppActivityManager leftSideActivityManager) {
        mainContentActivityManager.setDisplay(appLayout.getMainContentPanel());
        leftSideActivityManager.setDisplay(appLayout.getLeftSidePanel());
    }
}
