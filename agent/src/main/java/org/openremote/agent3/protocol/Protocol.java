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
package org.openremote.agent3.protocol;

import org.openremote.container.ContainerService;
import org.openremote.model.asset.ThingAttribute;

import java.util.Collection;

/**
 * A Protocol implementation must have a unique name in a VM. TODO This is not enforced.
 * <p>
 * OpenRemote protocols are in {@link org.openremote.Constants#PROTOCOL_NAMESPACE}, third-party protocols
 * should use their own URN namespace identifier.
 * <p>
 * The protocol implementation can receive {@link org.openremote.model.AttributeValueChange} messages on the
 * {@link #ACTUATOR_TOPIC}. It can produce {@link org.openremote.model.AttributeValueChange} messages on the
 * {@link #SENSOR_TOPIC}.
 * <p>
 * The linked attributes of a protocol provide the model for the protocol to perform these operations. How
 * attributes and value changes map to actual device and service calls is up to the implementation.
 * <p>
 * The linked protocol handles south-bound read and write of the attribute value: If the user writes
 * a new value into the Thing attribute, the protocol translates this value change into a
 * device (or service) action. If the actual state of the device (or service) changes, the linked
 * protocol writes the new state into the attribute value and the asset system user is notified of the change.
 * <p>
 * Data type conversion is also delegated to the Protocol implementation: If an attribute has a particular
 * AttributeType and therefore a certain JsonValue, the Protocol must receive and send value change messages
 * with values of that type.
 * TODO: Some kind of converter system can be introduced later, although I find JsonValue#asXXX() is already a good utility for protocol implementators.
 * <p>
 * Protocol-specific meta items required for the link can be added to Thing attributes, such as the
 * Hue light identifier or the ZWave node and command.
 */
public interface Protocol extends ContainerService {

    // TODO: Some of these options should be configurable depending on expected load etc.

    // Message topic for communicating from thing to protocol layer (thing attribute changed, trigger actuator)
    String ACTUATOR_TOPIC = "seda://ActuatorTopic?multipleConsumers=true&concurrentConsumers=10&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

    // Message topic for communicating from protocol to thing layer (sensor changed, trigger thing attribute update)
    String SENSOR_TOPIC = "seda://SensorTopic?waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&size=1000";

    String getProtocolName();

    void linkAttributes(Collection<ThingAttribute> attributes) throws Exception;

    void unlinkAttributes(String thingId) throws Exception;

}
