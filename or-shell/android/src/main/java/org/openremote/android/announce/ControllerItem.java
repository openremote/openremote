/*
 * Copyright 2015, OpenRemote Inc.
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

package org.openremote.android.announce;

import org.fourthline.cling.model.meta.Device;

public class ControllerItem {

    Device controller;

    public ControllerItem(Device controller) {
        this.controller = controller;
    }

    public Device getController() {
        return controller;
    }

    public String getPresentationURI() {
        return getController().getDetails().getPresentationURI().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ControllerItem that = (ControllerItem) o;
        return controller.equals(that.controller);
    }

    @Override
    public int hashCode() {
        return controller.hashCode();
    }

    @Override
    public String toString() {
        return getController().getDetails() != null && getController().getDetails().getFriendlyName() != null
                ? getController().getDetails().getFriendlyName()
                : getController().getDisplayString();
    }
}