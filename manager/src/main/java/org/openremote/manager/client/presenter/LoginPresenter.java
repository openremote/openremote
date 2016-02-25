package org.openremote.manager.client.presenter;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import com.workingflows.js.jscore.client.api.promise.Promise;
import org.fusesource.restygwt.client.Method;
import org.fusesource.restygwt.client.MethodCallback;
import org.gwtbootstrap3.client.ui.constants.AlertType;
import org.openremote.manager.client.event.UserChangeEvent;
import org.openremote.manager.client.i18n.ManagerMessages;
import org.openremote.manager.client.rest.LoginRestService;
import org.openremote.manager.client.service.SecurityService;
import org.openremote.manager.client.view.LoginView;
import org.openremote.manager.shared.model.Credentials;
import org.openremote.manager.shared.model.LoginResult;

/**
 * Created by Richard on 11/02/2016.
 */
public class LoginPresenter extends com.google.gwt.activity.shared.AbstractActivity implements LoginView.Presenter {
    private LoginView view;
    private PlaceController placeController;
    private EventBus eventBus;
    private LoginRestService loginRestService;
    private SecurityService securityService;
    private ManagerMessages messages;
    private Place redirectTo;

    @Inject
    public LoginPresenter(LoginView view,
                          LoginRestService loginRestService,
                          SecurityService securityService,
                          PlaceController placeController,
                          ManagerMessages messages,
                          EventBus eventBus) {
        this.view = view;
        this.placeController = placeController;
        this.messages = messages;
        this.eventBus = eventBus;
        this.loginRestService = loginRestService;
        this.securityService = securityService;
        view.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget container, com.google.gwt.event.shared.EventBus eventBus) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());
    }

    @Override
    public LoginView getView() {
        return view;
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    @Override
    public void doLogin() {
        view.setLoginInProgress(true);

        Promise p = new Promise((resolve, reject) -> {
            Credentials credentials = new Credentials();
            credentials.setUsername(view.getUsername());
            credentials.setPassword(view.getPassword());

            // Make call to server
            loginRestService.login(credentials, new MethodCallback<LoginResult>() {
                @Override
                public void onFailure(Method method, Throwable exception) {
                    reject.rejected(new LoginResult(method.getResponse().getStatusCode(), null));
                }

                @Override
                public void onSuccess(Method method, LoginResult response) {
                    resolve.resolve(response);
                }
            });
        });

        p.then(obj -> {
            LoginResult result = (LoginResult) obj;
            if (result.getHttpResponse() == 200) {
                // Token and XSRF are set as cookies so no need to store manually
                eventBus.fireEvent(new UserChangeEvent(securityService.getUsername()));
                goTo(redirectTo);
            } else {
                view.showAlert(AlertType.DANGER, messages.loginFailed());
            }

            view.setLoginInProgress(false);
            return null;
        }).catchException(obj -> {
            LoginResult result = (LoginResult) obj;
            if (result.getHttpResponse() == 404) {
                view.showAlert(AlertType.DANGER, messages.serverUnavailable());
            } else {
                view.showAlert(AlertType.DANGER, messages.serverError(result.getHttpResponse()));
            }

            view.setLoginInProgress(false);
            return null;
        });
    }

    public void setRedirectTo(Place redirectTo) {
        this.redirectTo = redirectTo;
    }
}
