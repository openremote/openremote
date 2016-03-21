package org.openremote.manager.client.assets;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Activity for Asset Manager
 */
public class AssetDetailActivity
        extends AppActivity<AssetsPlace>
        implements AssetDetailView.Presenter {

    final AssetDetailView view;
    final PlaceController placeController;

    @Inject
    public AssetDetailActivity(AssetDetailView view,
                               PlaceController placeController) {
        this.view = view;
        this.placeController = placeController;
    }

    @Override
    protected void init(AssetsPlace place) {

    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
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
}
