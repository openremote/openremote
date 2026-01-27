/*
 * Copyright 2017, OpenRemote Inc.
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
package org.openremote.agent.protocol;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.openremote.model.ContainerService;
import org.openremote.model.attribute.AttributeRef;
import org.openremote.model.datapoint.ValueDatapoint;

public interface ProtocolPredictedDatapointService extends ContainerService {

  void updateValue(AttributeRef attributeRef, Object value, LocalDateTime timestamp);

  void updateValue(String assetId, String attributeName, Object value, LocalDateTime timestamp);

  void updateValues(
      String assetId, String attributeName, List<ValueDatapoint<?>> valuesAndTimestamps);

  void purgeValues(String assetId, String attributeName);

  void purgeValuesBefore(String assetId, String attributeName, Instant timestamp);
}
