package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import elemental.json.JsonObject;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.view.MapView;
import org.openremote.manager.shared.map.MapResource;
import org.openremote.manager.shared.rpc.RequestData;
import org.openremote.manager.shared.rpc.RpcService;
import org.openremote.manager.shared.rpc.TestRequestData;

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
    private final RpcService rpcService;

    @Inject
    public MapActivity(MapView view,
                       SecurityService securityService,
                       RpcService rpcService,
                       MapResource mapResource,
                       PlaceController placeController,
                       EventBus bus) {
        this.view = view;
        this.securityService = securityService;
        this.rpcService = rpcService;
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
            // TODO: Remove this as only for demo purposes
            TestRequestData params = new TestRequestData();
            params.zoom = 15;
            params.row = 1;
            params.column = 1;
            rpcService.request(mapResource::getTest, params)
                    .onSuccess(jsonObject -> {
                        Window.alert("MAP ACTIVITY DEMO\n\nRESPONSE = " + jsonObject.toString());
                    })
                    .onError((e) -> {
                        // TODO: Handle map settings failure
                        Window.alert(e.getMessage());
                    })
                    .execute();

            rpcService.request(mapResource::getSettings)
                    .onSuccess(jsonObject -> {
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
