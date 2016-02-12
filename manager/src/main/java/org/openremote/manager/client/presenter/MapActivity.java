package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import org.fusesource.restygwt.client.Resource;
import org.openremote.manager.client.view.AssetsView;
import org.openremote.manager.client.view.MapView;

import javax.inject.Inject;

public class MapActivity
    extends AbstractActivity<MapPlace>
    implements MapView.Presenter {

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
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }
}
