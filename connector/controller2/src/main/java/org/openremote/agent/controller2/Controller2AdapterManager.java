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
package org.openremote.agent.controller2;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lifecycle of a controller "session". The first endpoint in a route will open
 * the adapter. If all routes with endpoints are stopped, the adapter will be closed.
 */
public interface Controller2AdapterManager {

    /**
     * This is a reference counting manager, one adapter for each adapter URL.
     */
    Controller2AdapterManager DEFAULT_MANAGER = new Controller2AdapterManager() {
        final protected List<Controller2Adapter> adapters = new ArrayList<>();

        @Override
        synchronized public Controller2Adapter openAdapter(URL url, String username, String password) {
            // If adapter exists, increment reference count and return
            for (Controller2Adapter adapter : adapters) {
                if (adapter.getUrl().equals(url)) {
                    adapter.incrementReferenceCount();
                    return adapter;
                }
            }
            Controller2Adapter adapter = new Controller2Adapter(url, username, password);
            adapters.add(adapter);
            return adapter;
        }

        @Override
        synchronized public void closeAdapter(Controller2Adapter adapter) {
            Iterator<Controller2Adapter> it = adapters.iterator();
            while (it.hasNext()) {
                Controller2Adapter next = it.next();
                if (next.getUrl().equals(adapter.getUrl())) {
                    // Count references down, if zero, close and remove
                    int referenceCount = adapter.decrementReferenceCount();
                    if (referenceCount == 0) {
                        adapter.close();
                        it.remove();
                    }
                    break;
                }
            }
        }
    };

    Controller2Adapter openAdapter(URL url, String username, String password); // TODO: Add more options if needed

    void closeAdapter(Controller2Adapter adapter);
}