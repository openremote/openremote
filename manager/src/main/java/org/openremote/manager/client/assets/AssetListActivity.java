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
import org.openremote.manager.client.event.bus.EventBus;
import org.openremote.manager.client.event.bus.EventRegistration;
import org.openremote.manager.client.mvp.AppActivity;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Activity for Asset Manager
 */
public class AssetListActivity
        extends AppActivity<AssetsPlace>
        implements AssetListView.Presenter {

    final AssetListView view;
    final PlaceController placeController;

    @Inject
    public AssetListActivity(AssetListView view,
                             PlaceController placeController) {
        this.view = view;
        this.placeController = placeController;
    }

    @Override
    public AppActivity<AssetsPlace> init(AssetsPlace place) {
        return this;
    }

    @Override
    public void start(AcceptsOneWidget container, EventBus eventBus, Collection<EventRegistration> registrations) {
        view.setPresenter(this);
        container.setWidget(view.asWidget());
    }

    @Override
    public void goTo(Place place) {
        placeController.goTo(place);
    }

    @Override
    public void getHelloText() {
//        resource("hello").get().send(new TextCallback() {
//            @Override
//            public void onSuccess(Method method, String response) {
//                view.setHelloText(response);
//            }
//
//            @Override
//            public void onFailure(Method method, Throwable exception) {
//                view.setHelloText("Request failed!");
//            }
//        });
    }

    /* TODO outdated
    protected static String hostname() {
        return Window.Location.getHostName();
    }

    protected static String port() {
        return Window.Location.getPort();
    }

    protected Resource resource(String... pathElement) {
        StringBuilder sb = new StringBuilder();
        sb.append("http://").append(hostname()).append(":").append(port());
        if (pathElement != null) {
            for (String pe : pathElement) {
                sb.append("/").append(pe);
            }
        }
        return new Resource(sb.toString());
    }
    */
}
