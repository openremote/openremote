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
package org.openremote.agent.protocol.tradfri.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.openremote.agent.protocol.tradfri.device.event.EventHandler;
import org.openremote.agent.protocol.tradfri.device.event.GatewayEvent;
import org.openremote.agent.protocol.tradfri.util.ApiEndpoint;
import org.openremote.agent.protocol.tradfri.util.CoapClient;

/** The class that observes an IKEA TRÅDFRI gateway to automagically detect changes */
public class GatewayObserver extends Observer {

  /** The IKEA TRÅDFRI gateway to observe */
  private Gateway gateway;

  /** A cache of the devices registered to the IKEA TRÅDFRI gateway */
  private HashMap<Integer, Device> devices;

  /**
   * An object mapper used for mapping JSON responses from the IKEA TRÅDFRI gateway to Java classes
   */
  private ObjectMapper objectMapper;

  /**
   * Construct the GatewayObserver class
   *
   * @param gateway The IKEA TRÅDFRI gateway to observe
   * @param coapClient A CoAP client that can be used to communicate with the device using the IKEA
   *     TRÅDFRI gateway
   */
  public GatewayObserver(Gateway gateway, CoapClient coapClient) {
    super(ApiEndpoint.getUri(ApiEndpoint.DEVICES), coapClient);
    this.gateway = gateway;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Start observing the gateway to automagically detect changes
   *
   * @return True if successfully started observing, false if not
   */
  @Override
  public boolean start() {
    Device[] devices = gateway.getDevices();
    this.devices = new HashMap<>();
    for (Device device : devices) {
      this.devices.put(device.getInstanceId(), device);
    }
    return super.start();
  }

  /**
   * Handles a new response from the CoAP client and calls the appropriate event handlers for the
   * IKEA TRÅDFRI gateway
   *
   * @param payload The payload of the response to the CoAP request
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public void callEventHandlers(String payload) {
    try {
      int[] deviceIds = objectMapper.readValue(payload, int[].class);
      List<EventHandler<?>> called = new ArrayList<>();
      GatewayEvent event = new GatewayEvent(gateway);
      for (EventHandler<?> eventHandler : gateway.getEventHandlers()) {
        if (eventHandler.getEventType().isAssignableFrom(event.getClass())
            && !called.contains(eventHandler)) {
          ((EventHandler) eventHandler).handle(event);
          called.add(eventHandler);
        }
      }
    } catch (JsonProcessingException ignored) {
    }
  }
}
