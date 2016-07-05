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

import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;
import org.openremote.manager.shared.Consumer;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class AssetsActivity
    extends AppActivity<AssetsPlace>
    implements AssetsView.Presenter {

    private static final Logger LOG = Logger.getLogger(AssetsActivity.class.getName());

    final AssetsView view;
    final PlaceController placeController;
    final EventBus eventBus;

    @Inject
    public AssetsActivity(AssetsView view,
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
    }

    @Override
    public void loadAssetChildren(AssetsView.Asset parent, Consumer<List<AssetsView.Asset>> consumer) {
        if (parent.getId() == null) {
            consumer.accept(Arrays.asList(
                new AssetsView.Asset("composite:gateways", "Composite", "Gateways", null),
                new AssetsView.Asset("composite:buildings", "Composite", "Buildings", null),
                new AssetsView.Asset("composite:rooms", "Composite", "Rooms", null),
                new AssetsView.Asset("composite:thermostats", "Composite", "Thermostats", null)
            ));
            return;
        } else if (parent.getType().equals("Composite")) {
            if (parent.getId().equals("composite:gateways")) {
                consumer.accept(Arrays.asList(
                    new AssetsView.Asset("1", "Composite", "Gateway A", "123.123"),
                    new AssetsView.Asset("2", "Composite", "Gateway B", "123.123"),
                    new AssetsView.Asset("3", "Composite", "Gateway C", "123.123"),
                    new AssetsView.Asset("4", "Composite", "Gateway D", "123.123"),
                    new AssetsView.Asset("5", "Composite", "Gateway E", "123.123")
                ));
                return;
            }

            if (parent.getId().equals("1")) {
                consumer.accept(Arrays.asList(
                    new AssetsView.Asset("11", "Sensor", "Sensor 1", "123.123"),
                    new AssetsView.Asset("22", "Sensor", "Sensor 2", "123.123"),
                    new AssetsView.Asset("33", "Sensor", "Sensor 3", "123.123")
                ));
                return;
            }
        }
        consumer.accept(new ArrayList<>());
    }

    @Override
    public void onAssetSelected(AssetsView.Asset asset) {
        view.setAssetDisplayName(asset.getDisplayName());
    }
}
