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

import org.openremote.model.asset.ThingAttribute;
import org.openremote.container.ContainerService;

import java.util.Collection;
import java.util.logging.Logger;

public interface Protocol extends ContainerService {

    Logger LOG = Logger.getLogger(Protocol.class.getName());

    // TODO: Some of these options should be configurable depending on expected load etc.

    // Message topic for communicating from thing to protocol layer (thing attribute changed, trigger actuator)
    String ACTUATOR_TOPIC = "seda://ActuatorTopic?multipleConsumers=true&concurrentConsumers=10&waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&limitConcurrentConsumers=false&size=1000";

    // Message topic for communicating from protocol to thing layer (sensor changed, trigger thing attribute update)
    String SENSOR_TOPIC = "seda://SensorTopic?waitForTaskToComplete=NEVER&purgeWhenStopping=true&discardIfNoConsumers=true&size=1000";

    String getProtocolName();

    void linkAttributes(Collection<ThingAttribute> attributes) throws Exception;

    void unlinkAttributes(String entityId) throws Exception;

}
