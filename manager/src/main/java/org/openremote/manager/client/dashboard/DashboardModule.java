package org.openremote.manager.client.dashboard;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;
import org.openremote.manager.client.dashboard.view.DashboardView;
import org.openremote.manager.client.dashboard.view.DashboardViewImpl;

public class DashboardModule extends AbstractGinModule {

    @Override
    protected void configure() {

        bind(DashboardView.class)
                .to(DashboardViewImpl.class)
                .in(Singleton.class);

        bind(DashboardActivity.class);

    }
}
