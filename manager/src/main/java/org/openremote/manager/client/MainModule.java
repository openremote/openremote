package org.openremote.manager.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import org.openremote.manager.client.presenter.AssetsActivity;
import org.openremote.manager.client.presenter.MapActivity;
import org.openremote.manager.client.presenter.MapPlace;
import org.openremote.manager.client.view.*;

public class MainModule extends AbstractGinModule {

    @Override
    protected void configure() {
        // App Wiring
        bind(EventBus.class).to(SimpleEventBus.class).in(Singleton.class);
        bind(PlaceHistoryMapper.class).to(HistoryMapper.class).in(Singleton.class);
        bind(MainController.class).to(MainControllerImpl.class).in(Singleton.class);

        // Views
        bind(MainView.class).to(MainViewImpl.class).in(Singleton.class);
        bind(MainMenuView.class).to(MainMenuViewImpl.class).in(Singleton.class);
        bind(MapView.class).to(MapViewImpl.class).in(Singleton.class);
        bind(AssetsView.class).to(AssetsViewImpl.class).in(Singleton.class);

        // Activities
        bind(AssetsActivity.class);
        bind(MapActivity.class);
    }

    @Provides
    @Singleton
    @Named("MainContentManager")
    public ActivityManager getMainContentActivityMapper(
            MainContentActivityMapper activityMapper, EventBus eventBus) {
        return new ActivityManager(activityMapper, eventBus);
    }

    @Provides
    @Singleton
    public PlaceController getPlaceController(EventBus eventBus) {
        return new PlaceController(eventBus);
    }

    @Provides
    @Singleton
    public PlaceHistoryHandler getHistoryHandler(PlaceController placeController,
                                                 PlaceHistoryMapper historyMapper,
                                                 EventBus eventBus) {
        PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
        historyHandler.register(placeController, eventBus, new MapPlace());
        return historyHandler;
    }
}
