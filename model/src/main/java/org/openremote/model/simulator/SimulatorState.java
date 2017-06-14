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
package org.openremote.model.simulator;

import org.openremote.model.attribute.AttributeRef;

import java.util.HashMap;
import java.util.Map;

public class SimulatorState {

    public class SimulatedAssetAttribute {

        final protected String assetName;
        final protected String attributeName;

        public SimulatedAssetAttribute(String assetName, String attributeName) {
            this.assetName = assetName;
            this.attributeName = attributeName;
        }

        public String getAssetName() {
            return assetName;
        }

        public String getAttributeName() {
            return attributeName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SimulatedAssetAttribute that = (SimulatedAssetAttribute) o;

            if (!assetName.equals(that.assetName)) return false;
            return attributeName.equals(that.attributeName);
        }

        @Override
        public int hashCode() {
            int result = assetName.hashCode();
            result = 31 * result + attributeName.hashCode();
            return result;
        }
    }

    protected Map<AttributeRef, SimulatedAssetAttribute> attributeDetails = new HashMap<>();
    protected SimulatorElement[] elements = new SimulatorElement[0];

    protected SimulatorState() {
    }

    public SimulatorState(SimulatorElement... elements) {
        this.elements = elements;
    }

    public SimulatorElement[] getElements() {
        return elements;
    }

    public void setAttributeDetails(Map<AttributeRef, SimulatedAssetAttribute> attributeDetails) {
        this.attributeDetails = attributeDetails;
    }

    public void clearAttributeDetails() {
        this.attributeDetails.clear();
    }
}
