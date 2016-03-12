package org.openremote.manager.client;

import com.google.gwt.place.shared.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.inject.Provider;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.i18n.ManagerConstants;
import org.openremote.manager.client.presenter.ActivityInitialiser;
import org.openremote.manager.client.presenter.HeaderPresenter;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.view.AppLayout;

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


/* TODO something is broken here when compiling real GWT (no super dev)
function $start(this$static){
  var lastArg;
  $doc.title = 'OpenRemote Manager';
  (lastArg = this$static.securityService , makeLambdaFunction(AppControllerImpl$lambda$2$Type.prototype.accept, new AppControllerImpl$lambda$2$Type(this$static)) , lastArg).keycloak.init();
  null.$_nullMethod();
  null.$_nullMethod();
}
*/

        Window.setTitle(constants.appTitle());

        // Initialise security service
        securityService.init(
                authenticated -> {
                    if (authenticated) {
                        // Add event handlers for security service
//                        securityService.onAuthLogout(() -> {
//                            eventBus.fireEvent(new UserChangeEvent(null));
//                        });
//
//                        securityService.onAuthSuccess(() -> {
//                            eventBus.fireEvent(new UserChangeEvent(securityService.getUsername()));
//                        });

                        RootLayoutPanel.get().add(appLayout);
                        appLayout.setPresenter(this);
                        placeHistoryHandler.handleCurrentHistory();
                    } else {
                        securityService.login();
                    }
                },
                () -> {
                    // TODO: Handle keycloak failure
                    Window.alert("KEYCLOAK INIT FAILURE");
                });
    }
}
