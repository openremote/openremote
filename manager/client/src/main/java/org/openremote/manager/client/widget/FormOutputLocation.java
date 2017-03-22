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
package org.openremote.manager.client.widget;

import com.google.gwt.user.client.ui.Label;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormOutputLocation extends Label {

    public FormOutputLocation() {
        setStyleName("or-FormControl or-FormOutputText");
    }

    public boolean setCoordinates(String noLocationText, double[] coordinates) {
        // Rounding to 5 decimals gives us precision of about 1 meter, should be enough
        return setCoordinates(noLocationText, coordinates, 5);
    }

    public boolean setCoordinates(String noLocationText, double[] coordinates, int roundDecimalPlaces) {
        if (coordinates != null && coordinates.length == 2) {
            // TODO: This assumes 0 is Lng and 1 is Lat, which is true for PostGIS backend
            // TODO: Because Lat/Lng is the 'right way', we flip it here for display
            setText(round(coordinates[1], roundDecimalPlaces) + " " + round(coordinates[0], roundDecimalPlaces) + " Lat|Lng");
            return true;
        }
        setText(noLocationText);
        return false;
    }

    protected String round(double d, int places) {
        return new BigDecimal(d).setScale(places, RoundingMode.HALF_UP).toString();
    }

}