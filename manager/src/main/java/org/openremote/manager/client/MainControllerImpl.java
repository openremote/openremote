package org.openremote.manager.client;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.view.MainMenuView;
import org.openremote.manager.client.view.MainView;

import javax.inject.Inject;

public class MainControllerImpl implements MainController {

    private final MainView view;
    private final MainMenuView menuView;
    private final EventBus bus;
    private final PlaceController placeController;
    private final PlaceHistoryHandler placeHistoryHandler;

    @Inject
    public MainControllerImpl(PlaceController placeController,
                              PlaceHistoryHandler placeHistoryHandler,
                              EventBus bus,
                              MainView view,
                              MainMenuView menuView,
                              ActivityInitialiser activityInitialiser) {
        // ActivityInitialiser is needed so that activities are mapped to views
        this.view = view;
        this.menuView = menuView;
        this.placeController = placeController;
        this.placeHistoryHandler = placeHistoryHandler;
        this.bus = bus;
        menuView.setPresenter(this);
        view.setMenu(menuView);
    }

    @Override
    public MainMenuView getView() {
        return menuView;
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    @Override
    public void start() {
        RootLayoutPanel.get().add(view);
        placeHistoryHandler.handleCurrentHistory();
    }
}
