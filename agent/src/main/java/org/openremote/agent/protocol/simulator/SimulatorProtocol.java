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
package org.openremote.agent.protocol.simulator;

import org.openremote.agent.protocol.AbstractProtocol;
import org.openremote.agent.protocol.ConnectionStatus;
import org.openremote.model.simulator.element.ColorSimulatorElement;
import org.openremote.model.simulator.element.NumberSimulatorElement;
import org.openremote.model.simulator.SimulatorElement;
import org.openremote.model.simulator.element.SwitchSimulatorElement;
import org.openremote.model.AbstractValueHolder;
import org.openremote.model.ValidationFailure;
import org.openremote.model.asset.AssetAttribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.attribute.AttributeState;
import org.openremote.model.simulator.SimulatorState;
import org.openremote.model.value.Value;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.openremote.model.Constants.PROTOCOL_NAMESPACE;
import static org.openremote.model.asset.AssetMeta.RANGE_MAX;
import static org.openremote.model.asset.AssetMeta.RANGE_MIN;

public class SimulatorProtocol extends AbstractProtocol {

    /**
     * Controls how and when sensor update is called after an actuator write.
     */
    public enum Mode {

        /**
         * Send to actuator values are written through to sensor immediately.
         */
        WRITE_THROUGH_IMMEDIATE,

        /**
         * Send to actuator values are written through to sensor after configured delay.
         */
        WRITE_THROUGH_DELAYED,

        /**
         * Producer of send to actuator will have to manually update the sensor by calling {@link #updateSensor)}.
         */
        MANUAL
    }

    /**
     * Stores protocol config parameters.
     */
    public static class Instance {
        protected Mode mode;
        protected int delayMilliseconds;
        protected boolean enabled;

        public Instance(Mode mode, int delayMilliseconds, boolean enabled) {
            this.enabled = enabled;
            this.mode = mode;
            this.delayMilliseconds = delayMilliseconds;
        }

        public Mode getMode() {
            return mode;
        }

        public int getDelayMilliseconds() {
            return delayMilliseconds;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    private static final Logger LOG = Logger.getLogger(SimulatorProtocol.class.getName());

    public static final int DEFAULT_WRITE_DELAY = 1000;

    public static final String PROTOCOL_NAME = PROTOCOL_NAMESPACE + ":simulator";

    /**
     * Required meta item, the simulator element that should be used, see subclasses of {@link SimulatorElement}.
     */
    public static final String SIMULATOR_ELEMENT = PROTOCOL_NAME + ":element";

    /**
     * Optional (defaults to {@link Mode#WRITE_THROUGH_IMMEDIATE}) determines how sensor updates occur after actuator write.
     */
    public static final String CONFIG_MODE = PROTOCOL_NAME + ":mode";

    /**
     * Optional (defaults to {@link #DEFAULT_WRITE_DELAY}) used in {@link Mode#WRITE_THROUGH_DELAYED} mode to control
     * delay between actuator write and sensor update
     */
    public static final String CONFIG_WRITE_DELAY_MILLISECONDS = PROTOCOL_NAME + ":delayMilliseconds";

    static final protected Map<AttributeRef, Instance> instances = new HashMap<>();
    static final protected Map<AttributeRef, AttributeRef> attributeInstanceMap = new HashMap<>();
    static final protected Map<AttributeRef, SimulatorElement> elements = new HashMap<>();

    // TODO This is not nice, find a better way how the protocol can talk to the service (through message bus?)
    protected Consumer<AttributeRef> protocolConfigurationValuesChangedHandler;

    @Override
    public String getProtocolName() {
        return PROTOCOL_NAME;
    }

    @Override
    protected void doLinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeRef protocolRef = protocolConfiguration.getReferenceOrThrow();

        synchronized (instances) {
            instances
                .computeIfAbsent(
                    protocolRef,
                    ref -> {
                        Mode mode = protocolConfiguration
                            .getMetaItem(CONFIG_MODE)
                            .map(item ->
                                item.getValueAsString()
                                    .map(value -> {
                                        try {
                                            return Mode.valueOf(value);
                                        } catch (Exception e) {
                                            LOG.fine("Invalid Mode value '" + item + "' provided");
                                            return null;
                                        }
                                    })
                                    .orElse(null)
                            )
                            .orElse(Mode.WRITE_THROUGH_IMMEDIATE);

                        int writeDelay = protocolConfiguration.getMetaItem(CONFIG_WRITE_DELAY_MILLISECONDS)
                            .flatMap(AbstractValueHolder::getValueAsInteger)
                            .orElse(DEFAULT_WRITE_DELAY);

                        updateStatus(protocolRef, protocolConfiguration.isEnabled() ? ConnectionStatus.CONNECTED : ConnectionStatus.DISABLED);
                        return new Instance(mode, writeDelay, protocolConfiguration.isEnabled());
                    }
                );
        }
    }

    @Override
    protected void doUnlinkProtocolConfiguration(AssetAttribute protocolConfiguration) {
        AttributeRef configRef = protocolConfiguration.getReferenceOrThrow();

        synchronized (instances) {
            instances.remove(configRef);
        }
    }

    @Override
    protected void doLinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {

        // Get element type from the attribute meta
        Optional<String> elementType = getElementType(attribute);
        if (!elementType.isPresent()) {
            LOG.warning("Can't configure simulator, missing " + SIMULATOR_ELEMENT + " meta item on: " + attribute);
            return;
        }

        AttributeRef configRef = protocolConfiguration.getReferenceOrThrow();
        AttributeRef attributeRef = attribute.getReferenceOrThrow();

        SimulatorElement element = createElement(elementType.get(), attribute);
        if (element == null) {
            LOG.warning("Can't simulate element '" + elementType + "': " + attribute);
            return;
        }
        if (attribute.getValue().isPresent()) {
            element.setValue(attribute.getValue().get());
            List<ValidationFailure> failures = element.getValidationFailures();
            if (!failures.isEmpty()) {
                element.clearValue();
                LOG.warning("Failed to initialize simulator element, initial value validation failures " + failures + ": " + attribute);
                return;
            }
        }

        LOG.info("Putting element '" + element + "' for: " + attribute);

        synchronized (instances) {
            elements.put(attributeRef, element);
            attributeInstanceMap.put(attributeRef, configRef);
        }
    }

    @Override
    protected void doUnlinkAttribute(AssetAttribute attribute, AssetAttribute protocolConfiguration) {
        AttributeRef attributeRef = attribute.getReferenceOrThrow();

        synchronized (instances) {
            elements.remove(attributeRef);
            attributeInstanceMap.remove(attributeRef);
        }
    }

    @Override
    protected void processLinkedAttributeWrite(AttributeEvent event, AssetAttribute protocolConfiguration) {
        if (putValue(event.getAttributeState())) {
            // Notify listener when write was successful
            if (protocolConfigurationValuesChangedHandler != null)
                protocolConfigurationValuesChangedHandler.accept(protocolConfiguration.getReferenceOrThrow());
        }
    }

    public Consumer<AttributeRef> getProtocolConfigurationValuesChangedHandler() {
        return protocolConfigurationValuesChangedHandler;
    }

    public void setValuesChangedHandler(Consumer<AttributeRef> protocolConfigurationValuesChangedHandler) {
        this.protocolConfigurationValuesChangedHandler = protocolConfigurationValuesChangedHandler;
    }

    /**
     * Call this to simulate an immediate sensor update (it uses the last value supplied from send to actuator).
     */
    public void updateSensor(String entityId, String attributeName) {
        updateSensor(new AttributeRef(entityId, attributeName));
    }

    /**
     * Call this to simulate a sensor update after the specified delay (it uses the last value supplied from send to actuator).
     */
    public void updateSensor(String entityId, String attributeName, int updateSensorDelayMilliseconds) {
        updateSensor(new AttributeRef(entityId, attributeName), updateSensorDelayMilliseconds);
    }

    /**
     * Call this to simulate an immediate sensor update (it uses the last value supplied from send to actuator).
     */
    public void updateSensor(AttributeRef attributeRef) {
        updateSensor(attributeRef, 0);
    }

    /**
     * Call this to simulate a sensor update after the specified delay (it uses the last value supplied from send to actuator).
     */
    public void updateSensor(AttributeRef attributeRef, int updateSensorDelayMilliseconds) {
        Optional<Value> value = getValue(attributeRef);
        if (!value.isPresent())
            return;
        final AttributeState state = new AttributeState(attributeRef, value.get());
        AttributeRef instanceRef = attributeInstanceMap.get(attributeRef);

        if (instanceRef == null) {
            LOG.warning("Attribute is not referenced by an instance:" + attributeRef);
            return;
        }

        Instance instance;

        synchronized (instances) {
            instance = instances.get(instanceRef);
        }

        if (instance == null) {
            LOG.warning("No instance found by name '" + instanceRef + "'");
            return;
        }

        if (!instance.isEnabled()) {
            LOG.fine("Simulator protocol configuration is disabled so cannot process request");
            return;
        }

        if (updateSensorDelayMilliseconds <= 0) {
            updateLinkedAttribute(state);
        } else {
            executorService.schedule(() -> updateLinkedAttribute(state), updateSensorDelayMilliseconds);
        }
    }

    /**
     * Call this to simulate a send to actuator.
     */
    public boolean putValue(String entityId, String attributeName, Value value) {
        return putValue(new AttributeState(new AttributeRef(entityId, attributeName), value));
    }

    /**
     * Call this to simulate a send to actuator.
     */
    protected boolean putValue(AttributeRef attributeRef, Value value) {
        return putValue(new AttributeState(attributeRef, value));
    }

    /**
     * Call this to simulate a send to actuator.
     */
    public boolean putValue(AttributeEvent event) {
        return putValue(event.getAttributeState());
    }

    /**
     * Call this to simulate a send to actuator.
     */
    public boolean putValue(AttributeState attributeState) {
        AttributeRef attributeRef = attributeState.getAttributeRef();
        AttributeRef instanceRef = attributeInstanceMap.get(attributeRef);

        if (instanceRef == null) {
            LOG.warning("Attribute is not referenced by an instance:" + attributeRef);
            return false;
        }

        Instance instance;

        synchronized (instances) {
            instance = instances.get(instanceRef);

            if (instance == null) {
                LOG.warning("No instance found by name '" + instanceRef + "'");
                return false;
            }

            if (!instance.isEnabled()) {
                LOG.fine("Simulator protocol configuration is disabled so cannot process request");
                return false;
            }

            LOG.info("Put simulator value: " + attributeState);
            SimulatorElement element = elements.get(attributeRef);
            if (element == null) {
                LOG.warning("No simulated element for: " + attributeRef);
                return false;
            }

            Optional<Value> oldValue = element.getValue();
            element.setValue(attributeState.getCurrentValue().orElse(null));
            List<ValidationFailure> failures =element.getValidationFailures();
            if (!failures.isEmpty()) {
                // Reset to old value
                oldValue.ifPresent(element::setValue);
                LOG.warning("Failed to update simulator element, state validation failures " + failures + ": " + attributeRef);
                return false;
            }
        }

        if (instance.getMode() != Mode.MANUAL) {
            updateSensor(attributeRef, instance.getMode() == Mode.WRITE_THROUGH_IMMEDIATE ? 0 : instance.getDelayMilliseconds());
        }

        return true;
    }

    /**
     * Call this to get the current value of an attribute.
     */
    public Optional<Value> getValue(String entityId, String attributeName) {
        return getValue(new AttributeRef(entityId, attributeName));
    }

    /**
     * Call this to get the current value of an attribute.
     */
    public Optional<Value> getValue(AttributeRef attributeRef) {
        synchronized (instances) {
            SimulatorElement element = elements.get(attributeRef);
            return element != null ? element.getValue() : Optional.empty();
        }
    }

    public List<SimulatorElement> getLinkedElements(AttributeRef protocolConfigurationRef) {
        List<AttributeRef> linkedAttributes = attributeInstanceMap.entrySet().stream()
            .filter(entry -> entry.getValue().equals(protocolConfigurationRef))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        List<SimulatorElement> linkedElements = new ArrayList<>();
        for (AttributeRef linkedAttribute : linkedAttributes) {
            if (elements.containsKey(linkedAttribute)) {
                linkedElements.add(elements.get(linkedAttribute));
            }
        }
        return linkedElements;
    }

    /**
     * Read a state snapshot.
     */
    public Optional<SimulatorState> getSimulatorState(AttributeRef protocolConfigurationRef) {
        synchronized (instances) {
            LOG.info("Getting simulator state for protocol configuration: " + protocolConfigurationRef);
            if (!instances.containsKey(protocolConfigurationRef))
                return Optional.empty();
            List<SimulatorElement> linkedElements = getLinkedElements(protocolConfigurationRef);
            return Optional.of(new SimulatorState(protocolConfigurationRef, linkedElements.toArray(new SimulatorElement[linkedElements.size()])));
        }
    }

    /**
     * Write a state snapshot.
     */
    public void updateSimulatorState(SimulatorState simulatorState) {
        synchronized (instances) {
            AttributeRef protocolConfigurationRef = simulatorState.getProtocolConfigurationRef();
            if (!instances.containsKey(protocolConfigurationRef)) {
                LOG.info("Ignoring simulator update, no instance for protocol configuration: " + protocolConfigurationRef);
                return;
            }
            // Merge from updated simulator state onto existing elements, setting their values
            for (SimulatorElement updatedElement : simulatorState.getElements()) {
                putValue(updatedElement.getAttributeRef(), updatedElement.getValue().orElse(null));
            }
        }
    }

    protected SimulatorElement createElement(String elementType, AssetAttribute attribute) {
        switch (elementType.toLowerCase(Locale.ROOT)) {
            case SwitchSimulatorElement.ELEMENT_NAME:
                return new SwitchSimulatorElement(attribute.getReferenceOrThrow());
            case NumberSimulatorElement.ELEMENT_NAME:
                return new NumberSimulatorElement(attribute.getReferenceOrThrow());
            case NumberSimulatorElement.ELEMENT_NAME_RANGE:
                int min = attribute.getMetaItem(RANGE_MIN).flatMap(AbstractValueHolder::getValueAsInteger).orElse(0);
                int max = attribute.getMetaItem(RANGE_MAX).flatMap(AbstractValueHolder::getValueAsInteger).orElse(100);
                return new NumberSimulatorElement(attribute.getReferenceOrThrow(), min, max);
            case ColorSimulatorElement.ELEMENT_NAME:
                return new ColorSimulatorElement(attribute.getReferenceOrThrow());
            default:
                return null;
        }
    }

    public static Optional<String> getElementType(AssetAttribute attribute) {
        return
            attribute.getMetaItem(SIMULATOR_ELEMENT)
                .flatMap(AbstractValueHolder::getValueAsString);
    }
}
