package org.openremote.manager.client.assets;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.app.AbstractActivity;

import javax.inject.Inject;

/**
 * Activity for Asset Manager
 */
public class AssetDetailActivity
        extends AbstractActivity<AssetsPlace>
        implements AssetDetailView.Presenter {

    final AssetDetailView view;
    final PlaceController placeController;
    final EventBus bus;

    @Inject
    public AssetDetailActivity(AssetDetailView view,
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
}
