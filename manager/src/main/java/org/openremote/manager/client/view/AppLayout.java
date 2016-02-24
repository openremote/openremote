package org.openremote.manager.client.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.IsWidget;

public interface AppLayout extends IsWidget {

    interface Presenter {
        AppLayout getView();

        void goTo(Place place);
    }

    void setPresenter(Presenter presenter);

    AcceptsOneWidget getMainContentPanel();

    AcceptsOneWidget getLeftSidePanel();

    AcceptsOneWidget getHeaderPanel();

    LoginView getLoginView();

    void showLogin();

    void hideLogin();

    void updateLayout(Place place);
}
