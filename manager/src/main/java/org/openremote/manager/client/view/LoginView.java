package org.openremote.manager.client.view;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;
import org.gwtbootstrap3.client.ui.constants.AlertType;

public interface LoginView extends IsWidget {

    interface Presenter {
        LoginView getView();

        void goTo(Place place);

        void doLogin();
    }

    void setPresenter(Presenter presenter);

    void setLoginInProgress(boolean loginInProgress);

    String getUsername();

    String getPassword();

    void show();

    void hide();

    void showAlert(AlertType alertType, String alert);
}
