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
package org.openremote.agent.protocol;

import org.openremote.model.ContainerService;
import org.openremote.model.datapoint.ValueDatapoint;

import java.time.LocalDateTime;
import java.util.List;

public interface ProtocolDatapointService extends ContainerService {

    void upsertValue(String assetId, String attributeName, Object value, LocalDateTime timestamp) throws IllegalStateException;

    void upsertValue(String assetId, String attributeName, Object value, long timestamp) throws IllegalStateException;

    void upsertValues(String assetId, String attributeName, List<ValueDatapoint<?>> valuesAndTimestamps) throws IllegalStateException;
}
