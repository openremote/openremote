package org.openremote.manager.client.main;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import org.openremote.manager.client.NavigationMapper;
import org.openremote.manager.client.dashboard.DashboardPlace;
import org.openremote.manager.client.main.view.MainView;
import org.openremote.manager.client.main.view.MainViewImpl;

public class MainModule extends AbstractGinModule {

    @Override
    protected void configure() {

        bind(EventBus.class)
                .to(SimpleEventBus.class)
                .in(Singleton.class);

        bind(PlaceHistoryMapper.class)
                .to(NavigationMapper.History.class)
                .in(Singleton.class);

        bind(ActivityMapper.class)
                .to(NavigationMapper.class)
                .in(Singleton.class);

        bind(MainView.Presenter.class)
                .to(MainPresenter.class)
                .in(Singleton.class);

        bind(MainView.class)
                .to(MainViewImpl.class)
                .in(Singleton.class);

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
                                                 EventBus eventBus,
                                                 ActivityManager activityManager) {
        // Yes, the unused ActivityManager argument here is necessary for init order
        PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
        historyHandler.register(placeController, eventBus, new DashboardPlace());
        return historyHandler;
    }

    @Provides
    @Singleton
    public ActivityManager getActivityManager(ActivityMapper mapper,
                                              EventBus eventBus,
                                              MainView mainView) {
        ActivityManager activityManager = new ActivityManager(mapper, eventBus);
        activityManager.setDisplay(mainView.getContentPanel());
        return activityManager;
    }

}
