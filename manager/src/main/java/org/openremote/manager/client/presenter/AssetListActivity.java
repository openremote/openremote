package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.view.AssetListView;

import javax.inject.Inject;

/**
 * Activity for Asset Manager
 */
public class AssetListActivity
        extends AbstractActivity<AssetsPlace>
        implements AssetListView.Presenter {

    final AssetListView view;
    final PlaceController placeController;
    final EventBus bus;

    @Inject
    public AssetListActivity(AssetListView view,
                             PlaceController placeController,
                             EventBus bus) {
        this.view = view;
        this.placeController = placeController;
        this.bus = bus;
    }

    @Override
    protected void init(AssetsPlace place) {

    }

    @Override
    public void start(AcceptsOneWidget container, com.google.gwt.event.shared.EventBus activityBus) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    @Override
    public void getHelloText() {
//        resource("hello").get().send(new TextCallback() {
//            @Override
//            public void onSuccess(Method method, String response) {
//                view.setHelloText(response);
//            }
//
//            @Override
//            public void onFailure(Method method, Throwable exception) {
//                view.setHelloText("Request failed!");
//            }
//        });
    }

    /* TODO outdated
    protected static String hostname() {
        return Window.Location.getHostName();
    }

    protected static String port() {
        return Window.Location.getPort();
    }

    protected Resource resource(String... pathElement) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://").append(hostname()).append(":").append(port());
        if (pathElement != null) {
            for (String pe : pathElement) {
                sb.append("/").append(pe);
            }
        }
        return new Resource(sb.toString());
    }
    */
}
