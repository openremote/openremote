package org.openremote.manager.client;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.dashboard.DashboardModule;
import org.openremote.manager.client.main.MainModule;
import org.openremote.manager.client.main.MainPresenter;

@GinModules(
    {
        MainModule.class,
        DashboardModule.class
    }
)
public interface ManagerGinjector extends Ginjector {

    PlaceHistoryHandler getPlaceHistoryHandler();

    MainPresenter getMainPresenter();

    EventBus getEventBus();
}
