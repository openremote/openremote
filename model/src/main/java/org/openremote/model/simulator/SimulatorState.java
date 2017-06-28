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
import org.openremote.model.event.shared.EventFilter;
import org.openremote.model.event.shared.SharedEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A snapshot of {@link SimulatorElement}s and their values.
 */
public class SimulatorState extends SharedEvent {

    public static class ConfigurationFilter extends EventFilter<SimulatorState> {

        public static final String FILTER_TYPE = "simulator-state-configuration";

        protected AttributeRef[] configurations;

        public ConfigurationFilter() {
        }

        public ConfigurationFilter(AttributeRef... configurations) {
            this.configurations = configurations;
        }

        public AttributeRef[] getConfigurations() {
            return configurations;
        }

        @Override
        public String getFilterType() {
            return FILTER_TYPE;
        }

        @Override
        public boolean apply(SimulatorState event) {
            return event.getProtocolConfigurationRef() != null
                && Arrays.asList(getConfigurations()).contains(event.getProtocolConfigurationRef());
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                "configurations='" + Arrays.toString(configurations) + '\'' +
                '}';
        }
    }

    // This map can be populated to complete the simulator state with user-friendly names for each simulated AttributeRef
    protected Map<String, String> assetIdAndName = new HashMap<>();

    protected AttributeRef protocolConfigurationRef;
    protected SimulatorElement[] elements = new SimulatorElement[0];

    protected SimulatorState() {
    }

    public SimulatorState(long timestamp, AttributeRef protocolConfigurationRef, SimulatorElement... elements) {
        super(timestamp);
        this.protocolConfigurationRef = protocolConfigurationRef;
        this.elements = elements;
    }

    public AttributeRef getProtocolConfigurationRef() {
        return protocolConfigurationRef;
    }

    public SimulatorElement[] getElements() {
        return elements;
    }

    public void updateAssetNames(Consumer<Map<String, String>> updater) {
        clearAssetNames();
        for (SimulatorElement element : elements) {
            assetIdAndName.put(element.getAttributeRef().getEntityId(), null);
        }
        updater.accept(assetIdAndName);
    }

    public void clearAssetNames() {
        this.assetIdAndName.clear();
    }

    public Map<String, String> getAssetIdAndName() {
        return assetIdAndName;
    }

    public String getElementName(SimulatorElement element) {
        AttributeRef ref = element.getAttributeRef();
        return (assetIdAndName.containsKey(ref.getEntityId()) ? assetIdAndName.get(ref.getEntityId()) : ref.getEntityId())
            + ":" + ref.getAttributeName();
    }

}
