package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import elemental.json.Json;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.Resource;
import org.fusesource.restygwt.client.TextCallback;
import org.openremote.manager.client.view.MapView;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapActivity
    extends AbstractActivity<MapPlace>
    implements MapView.Presenter {

    private static final Logger LOG = Logger.getLogger(MapActivity.class.getName());

    final MapView view;
    final PlaceController placeController;
    final EventBus bus;

    @Inject
    public MapActivity(MapView view,
                       PlaceController placeController,
                       EventBus bus) {
        this.view = view;
        this.placeController = placeController;
        this.bus = bus;
    }

    @Override
    protected void init(MapPlace place) {

    }

    @Override
    public void start(AcceptsOneWidget container, com.google.gwt.event.shared.EventBus activityBus) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        // TODO What's the lifecycle of the map?
        resource("/map").get().send(new TextCallback() {
            @Override
            public void onSuccess(Method method, String response) {
                view.showMap(Json.parse(response));
            }

            @Override
            public void onFailure(Method method, Throwable exception) {
                LOG.log(Level.SEVERE, "Error retrieving map settings", exception);
            }
        });
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    protected static String hostname() {
        return Window.Location.getHostName();
    }

    protected static String port() {
        return Window.Location.getPort();
    }

    protected static String realmPath() {
        return Window.Location.getPath();
    }

    protected Resource resource(String... pathElement) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://").append(hostname()).append(":").append(port());
        sb.append(realmPath());
        if (pathElement != null) {
            for (String pe : pathElement) {
                if (!pe.startsWith("/"))
                    sb.append("/");
                sb.append(pe);
            }
        }
        return new Resource(sb.toString());
    }

}
