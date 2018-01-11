/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.app.client.app;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.inject.Provider;
import org.openremote.app.client.Environment;
import org.openremote.app.client.event.ShowFailureEvent;
import org.openremote.app.client.event.ShowInfoEvent;
import org.openremote.app.client.event.ShowSuccessEvent;
import org.openremote.app.client.event.SubscriptionFailureEvent;
import org.openremote.app.client.toast.Toast;

import javax.inject.Inject;
import java.util.logging.Logger;

public class AppControllerImpl implements AppController, AppView.Presenter {

    private static final Logger LOG = Logger.getLogger(AppControllerImpl.class.getName());

    protected final AppView appView;
    protected final Environment environment;
    protected final PlaceHistoryHandler placeHistoryHandler;

    @Inject
    public AppControllerImpl(Environment environment,
                             PlaceHistoryHandler placeHistoryHandler,
                             Provider<HeaderPresenter> headerPresenterProvider,
                             Provider<FooterPresenter> footerPresenterProvider,
                             AppView appView,
                             AppInitializer appInitializer) { // AppInitializer is needed so that activities are mapped to views
        this.appView = appView;
        this.environment = environment;
        this.placeHistoryHandler = placeHistoryHandler;

        // Configure layout as not using activity mapper (it's static)
        HeaderPresenter headerPresenter = headerPresenterProvider.get();
        appView.getHeaderPanel().setWidget(headerPresenter.getView());
        FooterPresenter footerPresenter = footerPresenterProvider.get();
        appView.getFooterPanel().setWidget(footerPresenter.getView());

        environment.getEventBus().register(
            ShowInfoEvent.class,
            event -> environment.getApp().getToasts().showToast(
                new Toast(Toast.Type.INFO, event.getText(), Toast.DEFAULT_MAX_AGE)
            )
        );

        environment.getEventBus().register(
            ShowSuccessEvent.class,
            event -> environment.getApp().getToasts().showToast(
                new Toast(Toast.Type.SUCCESS, event.getText(), Toast.DEFAULT_MAX_AGE)
            )
        );

        environment.getEventBus().register(
            ShowFailureEvent.class,
            event -> {
                Toast.Type type = event.getDurationMillis() == ShowFailureEvent.DURABLE
                    ? Toast.Type.DURABLE_FAILURE : Toast.Type.FAILURE;
                environment.getApp().getToasts().showToast(
                    new Toast(type, event.getText(), event.getDurationMillis())
                );
            }
        );

        environment.getEventBus().register(
            SubscriptionFailureEvent.class,
            event -> environment.getEventBus().dispatch(new ShowFailureEvent(
                environment.getMessages().subscriptionFailed(event.getEventType()), 5000
            ))
        );
    }

    @Override
    public AppView getView() {
        return appView;
    }

    @Override
    public void goTo(Place place) {
        environment.getPlaceController().goTo(place);
    }

    @Override
    public void start() {
        LOG.info("Starting manager");
        appView.setPresenter(this);
        RootPanel.get().add(appView);
        placeHistoryHandler.handleCurrentHistory();
    }

    @Override
    public void stop() {
        LOG.info("Stopping manager");
        RootPanel.get().remove(appView);
    }
}
