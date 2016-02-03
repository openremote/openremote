package org.openremote.manager.client;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.dashboard.DashboardActivity;
import org.openremote.manager.client.dashboard.DashboardPlace;
import org.openremote.manager.client.main.MainPresenter;

import javax.inject.Inject;

public class NavigationMapper implements ActivityMapper {

    @WithTokenizers(
        {
            DashboardPlace.Tokenizer.class,
        }
    )
    public interface History extends PlaceHistoryMapper {
    }

    final MainPresenter mainPresenter;
    final Provider<DashboardActivity> dashboardActivityProvider;

    @Inject
    public NavigationMapper(MainPresenter mainPresenter,
                            Provider<DashboardActivity> dashboardActivityProvider,
                            EventBus bus) {
        super();
        this.mainPresenter = mainPresenter;
        this.dashboardActivityProvider = dashboardActivityProvider;
    }

    @Override
    public Activity getActivity(Place place) {

        if (place instanceof DashboardPlace) {
            return dashboardActivityProvider.get().init((DashboardPlace) place);
        }

        return null;
    }


}
