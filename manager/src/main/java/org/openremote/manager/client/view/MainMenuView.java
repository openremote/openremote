package org.openremote.manager.client.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * Created by Richard on 12/02/2016.
 */
public interface MainMenuView extends IsWidget {

    interface Presenter {
        MainMenuView getView();

        void goTo(Place place);
    }

    void setPresenter(Presenter presenter);
}
