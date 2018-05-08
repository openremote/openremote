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
package org.openremote.app.client.widget;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import org.openremote.model.value.ObjectValue;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormOutputLocation extends Composite {

    protected FlowPanel panel = new FlowPanel();
    protected InlineLabel coordinatesLabel = new InlineLabel();
    protected FormButton toggleButton = new FormButton();

    protected boolean reversed = false;
    protected ObjectValue coordinates;

    public FormOutputLocation() {
        initWidget(panel);

        panel.add(coordinatesLabel);
        panel.add(toggleButton);

        panel.setStyleName("layout horizontal center");
        coordinatesLabel.setStyleName("or-FormControl or-FormOutputText");
        toggleButton.setVisible(false);

        toggleButton.addClickHandler(event -> {
            reversed = !reversed;
            update();
        });
    }

    public boolean setCoordinates(String noLocationText, ObjectValue coordinates) {
        if (coordinates != null && coordinates.hasKey("latitude") && coordinates.hasKey("longitude")) {
            this.coordinates = coordinates;
            toggleButton.setVisible(true);
            update();
            return true;
        }
        toggleButton.setVisible(false);
        coordinatesLabel.setText(noLocationText);
        return false;
    }

    protected void update() {
        // TODO: This assumes 0 is Lng and 1 is Lat, which is true for PostGIS backend
        // Rounding to 5 decimals gives us precision of about 1 meter, should be enough
        if (reversed) {
            coordinatesLabel.setText(round(coordinates.getNumber("latitude").orElse(0d), 5) + " " + round(coordinates.getNumber("longitude").orElse(0d), 5));
            toggleButton.getUpFace().setText("Lng | Lat");
            toggleButton.getDownFace().setText("Lng | Lat");
        } else {
            coordinatesLabel.setText(round(coordinates.getNumber("longitude").orElse(0d), 5) + " " + round(coordinates.getNumber("latitude").orElse(0d), 5));
            toggleButton.getUpFace().setText("Lat | Lng");
            toggleButton.getDownFace().setText("Lat | Lng");
        }
    }

    protected String round(double d, int places) {
        return new BigDecimal(d).setScale(places, RoundingMode.HALF_UP).toString();
    }

}