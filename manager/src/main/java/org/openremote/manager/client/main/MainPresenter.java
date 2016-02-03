package org.openremote.manager.client.main;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import org.openremote.manager.client.main.view.MainView;

import javax.inject.Inject;

public class MainPresenter implements MainView.Presenter {

    final MainView view;
    final EventBus bus;
    final PlaceController placeController;

    @Inject
    public MainPresenter(MainView view,
                         PlaceController placeController,
                         EventBus bus) {
        this.view = view;
        this.placeController = placeController;
        this.bus = bus;

        view.setPresenter(this);
    }

    @Override
    public MainView getView() {
        return view;
    }

    public void goTo(Place place) {
        placeController.goTo(place);
    }

}
