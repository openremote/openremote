/*
 * Copyright 2026, OpenRemote Inc.
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

import java.util.function.Consumer;

import org.openremote.agent.protocol.tradfri.device.Device;
import org.openremote.agent.protocol.tradfri.device.Plug;
import org.openremote.agent.protocol.tradfri.device.event.EventHandler;
import org.openremote.agent.protocol.tradfri.device.event.PlugChangeOnEvent;
import org.openremote.model.asset.AssetDescriptor;
import org.openremote.model.asset.impl.PlugAsset;
import org.openremote.model.attribute.AttributeEvent;

import jakarta.persistence.Entity;

@Entity
public class TradfriPlugAsset extends PlugAsset implements TradfriAsset {

  public static final AssetDescriptor<TradfriPlugAsset> DESCRIPTOR =
      new AssetDescriptor<>("plug", "e6688a", TradfriPlugAsset.class);

  /** For use by hydrators (i.e. JPA/Jackson) */
  protected TradfriPlugAsset() {}

  public TradfriPlugAsset(String name) {
    super(name);
  }

  @Override
  public void addEventHandlers(Device device, Consumer<AttributeEvent> attributeEventConsumer) {
    Plug plug = device.toPlug();
    if (plug == null) {
      return;
    }

    EventHandler<PlugChangeOnEvent> plugOnOffEventHandler =
        new EventHandler<PlugChangeOnEvent>() {
          @Override
          public void handle(PlugChangeOnEvent event) {
            attributeEventConsumer.accept(
                new AttributeEvent(getId(), ON_OFF.getName(), plug.getOn()));
          }
        };
    device.addEventHandler(plugOnOffEventHandler);
  }

  @Override
  public void initialiseAttributes(Device device) {
    Plug plug = device.toPlug();
    if (plug == null) {
      return;
    }

    getAttributes().get(ON_OFF).ifPresent(attribute -> attribute.setValue(plug.getOn()));
  }
}
