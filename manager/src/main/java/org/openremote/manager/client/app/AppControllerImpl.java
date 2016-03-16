package org.openremote.manager.client.app;

import com.google.gwt.place.shared.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.i18n.ManagerConstants;
import org.openremote.manager.client.service.SecurityService;

import javax.inject.Inject;

public class AppControllerImpl implements AppController, AppLayout.Presenter {
    private final SecurityService securityService;
    private final AppLayout appLayout;
    private final EventBus eventBus;
    private final PlaceController placeController;
    private final PlaceHistoryHandler placeHistoryHandler;
    private ManagerConstants constants;
    private Provider<HeaderPresenter> headerPresenterProvider;

    /**
     * This provides the ability to intercept the place change outside of an activity
     * mapper and to change the place before anything else happens
     */
    public static class AppPlaceController extends PlaceController {
        private SecurityService securityService;
        private PlaceHistoryMapper historyMapper;
        private Place redirectPlace;
        private EventBus eventBus;

        public AppPlaceController(SecurityService securityService,
                                  PlaceHistoryMapper historyMapper,
                                  EventBus eventBus) {
            super(eventBus);
            this.securityService = securityService;
            this.historyMapper = historyMapper;
            this.eventBus = eventBus;
        }

        @Override
        public void goTo(Place newPlace) {
            if (securityService.isTokenExpired(10)) {
                securityService.login();
                return;
            }
            super.goTo(newPlace);
        }
    }

    @Inject
    public AppControllerImpl(PlaceController placeController,
                             Provider<HeaderPresenter> headerPresenterProvider,
                             PlaceHistoryHandler placeHistoryHandler,
                             EventBus eventBus,
                             AppLayout appLayout,
                             ManagerConstants constants,
                             SecurityService securityService,
                             ActivityInitialiser activityInitialiser) {

        // ActivityInitialiser is needed so that activities are mapped to views
        this.securityService = securityService;
        this.appLayout = appLayout;
        this.placeController = placeController;
        this.headerPresenterProvider = headerPresenterProvider;
        this.placeHistoryHandler = placeHistoryHandler;
        this.eventBus = eventBus;
        this.constants = constants;

        // Configure the header as not using activity mapper for header (it's static)
        HeaderPresenter headerPresenter = headerPresenterProvider.get();
        appLayout.getHeaderPanel().setWidget(headerPresenter.getView());

        // Monitor place changes to reconfigure the UI
        eventBus.addHandler(PlaceChangeEvent.TYPE, event -> {
            Place newPlace = event.getNewPlace();
            appLayout.updateLayout(newPlace);
            headerPresenter.onPlaceChange(newPlace);
        });

        // Monitor user change events
        eventBus.addHandler(UserChangeEvent.TYPE, event -> {
            headerPresenter.setUsername(event.getUsername());
        });
    }

    @Override
    public AppLayout getView() {
        return appLayout;
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    @Override
    public void start() {
        Window.setTitle(constants.appTitle());
        RootLayoutPanel.get().add(appLayout);
        appLayout.setPresenter(this);
        placeHistoryHandler.handleCurrentHistory();
    }
}
