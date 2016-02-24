package org.openremote.manager.client.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * Created by Richard on 24/02/2016.
 */
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
