package org.openremote.manager.client.dashboard.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

public interface DashboardView extends IsWidget {

    interface Presenter {
        void goTo(Place place);

        void getHelloText();
    }

    void setPresenter(Presenter presenter);

    void setHelloText(String text);

}
