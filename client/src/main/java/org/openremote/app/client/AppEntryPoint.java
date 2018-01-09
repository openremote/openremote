/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.app.client;

import com.google.gwt.core.client.GWT;

public class AppEntryPoint implements com.google.gwt.core.client.EntryPoint {

    protected final ManagerGinjector injector = GWT.create(ManagerGinjector.class);

    protected void startManager() {
        injector.getAppController().start();
    }

    protected void stopManager() {
        injector.getAppController().stop();
    }

    @Override
    public native void onModuleLoad() /*-{
        console.log("App GWT module loaded")

        // Add a one-time event listener that starts the manager
        var startEventListener = function(e) {
            if (!$wnd.openremote.INSTANCE.error) {
                this.@org.openremote.app.client.AppEntryPoint::startManager()();
            }
            e.currentTarget.removeEventListener("StartManager", startEventListener);
        }.bind(this);
        $wnd.addEventListener("StartManager", startEventListener);

        // Add a one-time event listener that stops the manager
        var stopEventListener = function(e) {
            this.@org.openremote.app.client.AppEntryPoint::stopManager()();
            e.currentTarget.removeEventListener("AppError", stopEventListener);
        }.bind(this);
        $wnd.addEventListener("AppError", stopEventListener);

        // Let others know jsinterop components are loaded
        $wnd.dispatchEvent(new CustomEvent("AppLoaded"));
    }-*/;


}
