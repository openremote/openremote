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

public class AppEntryPoint implements com.google.gwt.core.client.EntryPoint {

    @Override
    public void onModuleLoad() {
        // TODO JUL doesn't work, no idea why...
        dispatchReadyEvent();
    }

    public native static void dispatchReadyEvent() /*-{
        console.log("App GWT entry point");
        $wnd.dispatchEvent(new CustomEvent("AppReady"));
    }-*/;

}
