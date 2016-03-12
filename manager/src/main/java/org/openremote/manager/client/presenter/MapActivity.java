package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.service.RequestService;
import org.openremote.manager.client.view.MapView;
import org.openremote.manager.shared.map.MapResource;

import javax.inject.Inject;
import java.util.logging.Logger;

public class MapActivity
    extends AbstractActivity<OverviewPlace>
    implements MapView.Presenter {

    private static final Logger LOG = Logger.getLogger(MapActivity.class.getName());

    private final MapView view;
    private final MapResource mapResource;
    private final RequestService requestService;
    private final PlaceController placeController;
    private final EventBus bus;

    @Inject
    public MapActivity(MapView view,
                       MapResource mapResource,
                       RequestService requestService,
                       PlaceController placeController,
                       EventBus bus) {
        this.view = view;
        this.requestService = requestService;
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

            /* TODO This is the generic resteasy client
            Request<JsonObject> request = requestService.createRequest(true);
            request.setURI("http://localhost:8080/master/map");
            request.setMethod("GET");
            request.setAccepts("application/json");
            request.execute((responseCode, xmlHttpRequest, entity) -> {
                view.initialiseMap(entity);
            });
            */

            // This is the strongly typed client
            mapResource.getSettings(
                requestService.createRequestParams(
                    200,
                    view::initialiseMap,
                    ex -> {
                        // TODO: Handle map settings failure
                        Window.alert(ex.getMessage());
                    }
                )
            );

        } else {
            //TODO: Reconfigure the map
        }
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }
}
