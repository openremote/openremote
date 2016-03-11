package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.view.MapView;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.rest.RestService;

import javax.inject.Inject;
import java.util.logging.Logger;

public class MapActivity
    extends AbstractActivity<OverviewPlace>
    implements MapView.Presenter {

    private static final Logger LOG = Logger.getLogger(MapActivity.class.getName());

    private final MapView view;
    private final MapResource mapResource;
    private final PlaceController placeController;
    private final EventBus bus;
    private final SecurityService securityService;
    private final RestService restService;

    @Inject
    public MapActivity(MapView view,
                       SecurityService securityService,
                       RestService restService,
                       MapResource mapResource,
                       PlaceController placeController,
                       EventBus bus) {
        this.view = view;
        this.securityService = securityService;
        this.restService = restService;
        this.mapResource = mapResource;
        this.placeController = placeController;
        this.bus = bus;
    }

    @Override
    protected void init(OverviewPlace place) {

    }

    @Override
    public void start(AcceptsOneWidget container, com.google.gwt.event.shared.EventBus activityBus) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        if (!view.isMapInitialised()) {
            restService.request(mapResource::getSettings)
                    .onSuccess((jsonObject) -> {
                        view.initialiseMap(jsonObject);
                    })
                    .onError((e) -> {
                        // TODO: Handle map settings failure
                        Window.alert(e.getMessage());
                    })
                    .execute();
        } else {
            //TODO: Reconfigure the map
        }
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }
}
