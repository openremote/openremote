package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import com.workingflows.js.jscore.client.api.promise.Promise;
import elemental.json.JsonObject;
import org.openremote.manager.client.security.KeycloakUtil;
import org.openremote.manager.client.view.MapView;
import org.openremote.manager.shared.ClientInvocation;
import org.openremote.manager.shared.map.MapResource;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapActivity
    extends AbstractActivity<OverviewPlace>
    implements MapView.Presenter {

    private static final Logger LOG = Logger.getLogger(MapActivity.class.getName());

    private final MapView view;
    private final MapResource mapResource;
    private final PlaceController placeController;
    private final EventBus bus;

    @Inject
    public MapActivity(MapView view,
                       MapResource mapResource,
                       PlaceController placeController,
                       EventBus bus) {
        this.view = view;
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
            Promise p = new Promise((resolve, reject) -> {

                ClientInvocation<JsonObject> clientInvocation =
                    new ClientInvocation<JsonObject>(KeycloakUtil.getAccessToken())
                    .onResponse(200, resolve::resolve, reject::rejected);
                mapResource.getSettings(clientInvocation);

            });

            p.then(obj -> {
                view.initialiseMap((JsonObject) obj);
                return null;
            }).catchException(obj -> {
                LOG.log(Level.SEVERE, "Error retrieving map settings", (Exception) obj);
                return null;
            });
        } else {
            //TODO: Reconfigure the map
        }
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }
}
