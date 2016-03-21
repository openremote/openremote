package org.openremote.manager.client.app;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

public interface HeaderView extends IsWidget {

    interface Presenter {
        HeaderView getView();

        void goTo(Place place);

        void onPlaceChange(Place place);

        void doLogout();

        void setUsername(String username);
    }

    void setPresenter(Presenter presenter);

    void onPlaceChange(Place place);

    void setUsername(String username);
}
