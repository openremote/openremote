package org.openremote.manager.client.presenter;

import com.google.web.bindery.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import org.openremote.manager.client.view.LoginView;

/**
 * Created by Richard on 11/02/2016.
 */
public class LoginActivity extends AbstractActivity<LoginPlace> implements LoginView.Presenter {
    private LoginView view;
    private PlaceController placeController;
    private EventBus eventBus;

    @Inject
    public LoginActivity(LoginView view,
                         PlaceController placeController,
                         EventBus eventBus) {
        this.view = view;
        this.placeController = placeController;
        this.eventBus = eventBus;
    }

    @Override
    protected void init(LoginPlace place) {

    }

    @Override
    public void start(AcceptsOneWidget container, com.google.gwt.event.shared.EventBus eventBus) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());
    }

    @Override
    public LoginView getView() {
        return null;
    }

    @Override
    public void goTo(Place place) {

    }
}
