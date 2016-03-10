package org.openremote.manager.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.core.client.GWT;
import com.google.gwt.inject.client.AbstractGinModule;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import org.openremote.manager.client.i18n.ManagerConstants;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.interop.keycloak.Keycloak;
import org.openremote.manager.client.presenter.*;
import org.openremote.manager.client.service.*;
import org.openremote.manager.client.view.*;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.rest.RestParams;
import org.openremote.manager.shared.rest.RestService;

public class MainModule extends AbstractGinModule {

    @Override
    protected void configure() {
        // App Wiring
        bind(EventBus.class).to(SimpleEventBus.class).in(Singleton.class);
        bind(PlaceHistoryMapper.class).to(HistoryMapper.class).in(Singleton.class);
        bind(AppController.class).to(AppControllerImpl.class).in(Singleton.class);

        // Views
        bind(HeaderView.class).to(HeaderViewImpl.class).in(Singleton.class);
        bind(LoginView.class).to(LoginViewImpl.class).in(Singleton.class);
        bind(AppLayout.class).to(AppLayoutImpl.class).in(Singleton.class);
        bind(MapView.class).to(MapViewImpl.class).in(Singleton.class);
        bind(AssetListView.class).to(AssetListViewImpl.class).in(Singleton.class);
        bind(AssetDetailView.class).to(AssetDetailViewImpl.class).in(Singleton.class);
        bind(LeftSideView.class).to(LeftSideViewImpl.class).in(Singleton.class);

        // Activities
        bind(AssetDetailActivity.class);
        bind(MapActivity.class);

        // Services
        bind(SecurityService.class).to(SecurityServiceImpl.class).in(Singleton.class);
        bind(CookieService.class).to(CookieServiceImpl.class).in(Singleton.class);
        bind(ValidatorService.class).to(ValidatorServiceImpl.class).in(Singleton.class);
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
    @Named("LeftSideManager")
    public ActivityManager getLeftSideActivityMapper(
            LeftSideActivityMapper activityMapper, EventBus eventBus) {
        return new ActivityManager(activityMapper, eventBus);
    }

    @Provides
    @Singleton
    public ManagerConstants getConstants() {
        return GWT.create(ManagerConstants.class);
    }

    @Provides
    @Singleton
    public ManagerMessages getMessages() {
        return GWT.create(ManagerMessages.class);
    }

    @Provides
    @Singleton
    public Keycloak getKeycloak() {
        return new Keycloak("/master/identity/install/or-manager");
    }

    @Provides
    @Singleton
    public RestService getRestService() {
        return new RestService(this::execute);
    }

    private <T> void execute(RestService.RestRequest<T> request) {
        // Client side MapResource implementation uses AJAX callback to async resolve
        // the request so we just construct the request params object and pass the request
        // through and handle the callback
        RestParams<T> requestParams = new RestParams<>();

        if (request.authorization != null) {
            requestParams.authorization = request.authorization;
        }
        requestParams.callback = (responseCode, xmlHttpRequest, result) -> {
            if (responseCode == 0) {
                request.errorCallback.accept(new RestService.Failure(0, "No response"));
            } else if (responseCode == request.expectedStatusCode) {
                request.successCallback.accept(result);
            } else {
                request.errorCallback.accept(new RestService.Failure(responseCode, "Expected status code: " + request.expectedStatusCode + " but received: " + responseCode));
            }
        };
        request.fn.apply(requestParams);
    }

    @Provides
    @Singleton
    public MapResource getMapResource() {
        MapResource mapRestService = getNativeMapResource();
        return mapRestService;
    }

    public static native MapResource getNativeMapResource() /*-{
        return $wnd.MapResource;
    }-*/;

    /* TODO
    @Provides
    @Singleton
    public AssetRestService getAssetRestService() {
        String baseUrl = GWT.getHostPageBaseURL();
        AssetRestService assetRestService = GWT.create(AssetRestService.class);
        ((RestServiceProxy) assetRestService).setResource(new Resource(baseUrl));
        return assetRestService;
    }
    (*/

    @Provides
    @Singleton
    public PlaceController getPlaceController(Keycloak keycloak,SecurityService securityService, PlaceHistoryMapper historyMapper, EventBus eventBus) {
        return new AppControllerImpl.AppPlaceController(keycloak, securityService, historyMapper, eventBus);
    }

    @Provides
    @Singleton
    public PlaceHistoryHandler getHistoryHandler(PlaceController placeController,
                                                 PlaceHistoryMapper historyMapper,
                                                 EventBus eventBus) {
        PlaceHistoryHandler historyHandler = new PlaceHistoryHandler(historyMapper);
        historyHandler.register(placeController, eventBus, new OverviewPlace());
        return historyHandler;
    }
}
