package org.openremote.manager.client.main.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

public interface MainView extends IsWidget {

    interface Presenter {
        MainView getView();

        void goTo(Place place);
    }

    void setPresenter(Presenter presenter);

    AcceptsOneWidget getContentPanel();
}
