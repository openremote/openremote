package org.openremote.manager.client;

import com.google.gwt.place.shared.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.event.LoginRequestEvent;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.i18n.ManagerConstants;
import org.openremote.manager.client.interop.keycloak.Keycloak;
import org.openremote.manager.client.interop.resteasy.REST;
import org.openremote.manager.client.presenter.ActivityInitialiser;
import org.openremote.manager.client.presenter.HeaderPresenter;
import org.openremote.manager.client.presenter.LoginPresenter;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.view.AppLayout;

import javax.inject.Inject;

public class AppControllerImpl implements AppController, AppLayout.Presenter {
    private final Keycloak keycloak;
    private final AppLayout appLayout;
    private final EventBus eventBus;
    private final PlaceController placeController;
    private final PlaceHistoryHandler placeHistoryHandler;
    private ManagerConstants constants;
    private Provider<LoginPresenter> loginPresenterProvider;
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
        private Keycloak keycloak;

        public AppPlaceController(Keycloak keycloak,
                SecurityService securityService,
                                  PlaceHistoryMapper historyMapper,
                                  EventBus eventBus) {
            super(eventBus);
            this.keycloak = keycloak;
            this.securityService = securityService;
            this.historyMapper = historyMapper;
            this.eventBus = eventBus;
        }

        @Override
        public void goTo(Place newPlace) {
            if (keycloak.isTokenExpired(10)) {
                keycloak.login();
                return;
            }
            super.goTo(newPlace);
        }
    }

    @Inject
    public AppControllerImpl(PlaceController placeController,
                             Provider<LoginPresenter> loginPresenterProvider,
                             Provider<HeaderPresenter> headerPresenterProvider,
                             PlaceHistoryHandler placeHistoryHandler,
                             EventBus eventBus,
                             AppLayout appLayout,
                             ManagerConstants constants,
                             Keycloak keycloak,
                             ActivityInitialiser activityInitialiser) {

        // ActivityInitialiser is needed so that activities are mapped to views
        this.keycloak = keycloak;
        this.appLayout = appLayout;
        this.placeController = placeController;
        this.loginPresenterProvider = loginPresenterProvider;
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

        // Monitor login/logout requests
        eventBus.addHandler(LoginRequestEvent.TYPE, event -> {
            Place place = event.getRedirectPlace();
            LoginPresenter loginPresenter = loginPresenterProvider.get();
            loginPresenter.setRedirectTo(place);
            appLayout.showLogin();
        });

        eventBus.addHandler(UserChangeEvent.TYPE, event -> {
            if (event.getUsername() != null) {
                appLayout.hideLogin();
            }
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

        // Initialise Keycloak
        // TODO: Handle keycloak failure
        keycloak.init()
                .success(authenticated -> {
                    if (authenticated) {
                        REST.apiURL = "//" + Window.Location.getHostName() + ":" + Window.Location.getPort() + "/" + keycloak.realm;
                        RootLayoutPanel.get().add(appLayout);
                        appLayout.setPresenter(this);
                        placeHistoryHandler.handleCurrentHistory();
                    } else {
                        keycloak.login();
                    }
                })
                .error(() -> {
                    Window.alert("KEYCLOAK INIT FAILURE");
                });
    }
}
