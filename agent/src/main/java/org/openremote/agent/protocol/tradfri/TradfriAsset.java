/*
 * Copyright 2025, OpenRemote Inc.
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.agent.protocol.tradfri;

import java.util.Optional;
import java.util.function.Consumer;

import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.model.attribute.Attribute;
import org.openremote.model.attribute.AttributeEvent;
import org.openremote.model.attribute.AttributeMap;
import org.openremote.model.value.AttributeDescriptor;
import org.openremote.model.value.ValueType;

public interface TradfriAsset {

  AttributeDescriptor<Integer> DEVICE_ID = new AttributeDescriptor<>("deviceId", ValueType.INTEGER);

  default Optional<Integer> getDeviceId() {
    return getAttributes().getValue(DEVICE_ID);
  }

  default void setDeviceId(Integer deviceId) {
    getAttributes().get(DEVICE_ID).orElse(new Attribute<>(DEVICE_ID)).setValue(deviceId);
  }

  String getId();

  AttributeMap getAttributes();

  void addEventHandlers(Device device, Consumer<AttributeEvent> attributeEventConsumer);

  void initialiseAttributes(Device device);
}
