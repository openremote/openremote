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
package org.openremote.manager.client.assets;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.MessageReceivedEvent;
import org.openremote.manager.client.event.ServerSendEvent;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.event.Message;
import org.openremote.manager.shared.event.ui.ShowFailureEvent;
import org.openremote.manager.shared.event.ui.ShowInfoEvent;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class AssetDetailActivity
        extends AppActivity<AssetsPlace>
        implements AssetDetailView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetDetailActivity.class.getName());

    final AssetDetailView view;
    final PlaceController placeController;
    final EventBus eventBus;

    @Inject
    public AssetDetailActivity(AssetDetailView view,
                               PlaceController placeController,
                               EventBus eventBus) {
        this.view = view;
        this.placeController = placeController;
        this.eventBus = eventBus;
    }

    @Override
    protected AppActivity<AssetsPlace> init(AssetsPlace place) {
        return this;
    }

    @Override
    protected String[] getRequiredRoles() {
        return new String[]{"read:assets"};
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());

        registrations.add(eventBus.register(MessageReceivedEvent.class, event -> {
            view.setMessageText(event.getMessage().getBody());
        }));
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    @Override
    public void sendMessage() {
        eventBus.dispatch(new ServerSendEvent(
            new Message("Hello from client!")
        ));
    }

    @Override
    public void showInfo() {
        eventBus.dispatch(new ShowInfoEvent("Hello World!"));
    }

    @Override
    public void showTempFailure() {
        eventBus.dispatch(new ShowFailureEvent("A temporary error. This will disappear in a few seconds.", 5000));
    }

    @Override
    public void showDurableFailure() {
        eventBus.dispatch(new ShowFailureEvent("The application crashed. This is a durable error."));
    }
}
