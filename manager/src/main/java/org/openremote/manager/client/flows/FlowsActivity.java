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
package org.openremote.manager.client.flows;

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;

import javax.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;

public class FlowsActivity
        extends AppActivity<FlowsPlace>
        implements FlowsView.Presenter {

    private static final Logger LOG = Logger.getLogger(FlowsActivity.class.getName());

    final FlowsView view;
    final PlaceController placeController;
    final EventBus eventBus;

    @Inject
    public FlowsActivity(FlowsView view,
                         PlaceController placeController,
                         EventBus eventBus) {
        this.view = view;
        this.placeController = placeController;
        this.eventBus = eventBus;
    }

    @Override
    public AppActivity<FlowsPlace> init(FlowsPlace place) {
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        container.setWidget(view.asWidget());
        view.setPresenter(this);
    }
}
