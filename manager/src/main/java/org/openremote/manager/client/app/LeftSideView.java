package org.openremote.manager.client.app;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

public interface LeftSideView extends IsWidget {

    interface Presenter {
        void goTo(Place place);
    }

    void setPresenter(Presenter presenter);
}
