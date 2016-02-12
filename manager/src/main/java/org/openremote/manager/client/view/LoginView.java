package org.openremote.manager.client.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

public interface LoginView extends IsWidget {

    interface Presenter {
        LoginView getView();

        void goTo(Place place);
    }

    void setPresenter(Presenter presenter);

    AcceptsOneWidget getContentPanel();
}
