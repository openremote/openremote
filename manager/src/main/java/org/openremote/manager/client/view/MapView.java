package org.openremote.manager.client.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;
import elemental.json.JsonObject;

public interface MapView extends IsWidget {

    interface Presenter {
        void goTo(Place place);
    }

    void setPresenter(Presenter presenter);

    void initialiseMap(JsonObject mapSettings);

    boolean isMapInitialised();
}
