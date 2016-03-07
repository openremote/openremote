package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import com.workingflows.js.jscore.client.api.promise.Promise;
import elemental.json.JsonObject;
import org.openremote.manager.client.auth.KeycloakUtil;
import org.openremote.manager.client.view.MapView;
import org.openremote.manager.shared.rest.ClientInvocation;
import org.openremote.manager.shared.rest.MapResource;

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

//                JsonObject mapSettings = getMapResource().getSettings(clientInvocation);
//                resolve.resolve(mapSettings);

//                RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, GWT.getHostPageBaseURL()+ "master/map");
//                try {
//                    rb.sendRequest(null, new RequestCallback() {
//                        @Override
//                        public void onResponseReceived(Request request, Response response) {
//                            JsonObject mapSettings = Json.parse(response.getText());
//                            resolve.resolve(mapSettings);
//                        }
//
//                        @Override
//                        public void onError(Request request, Throwable exception) {
//                            reject.rejected(exception);
//                        }
//                    });
//                } catch (Exception ex) {
//                    reject.rejected(ex);
//                }

                // Make call to server
//                REST.withCallback(new MethodCallback<JSONObject>() {
//                    @Override
//                    public void onFailure(Method method, Throwable exception) {
//                        reject.rejected(exception);
//                    }
//
//                    @Override
//                    public void onSuccess(Method method, JSONObject response) {
//                        resolve.resolve(response);
//                    }
//                }).call(mapRestService).getOptions();
            });

            p.then(obj -> {
                //JsonObject mapOptions = elemental.js.json.JsJsonObject.create(). obj;
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
