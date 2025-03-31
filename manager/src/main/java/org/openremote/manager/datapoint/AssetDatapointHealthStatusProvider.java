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
package org.openremote.manager.datapoint;

import java.util.Map;

import org.openremote.model.Container;
import org.openremote.model.ContainerService;
import org.openremote.model.system.HealthStatusProvider;

public class AssetDatapointHealthStatusProvider implements HealthStatusProvider {

  public static final String NAME = "datapoints";
  protected AssetDatapointService assetDatapointService;

  @Override
  public int getPriority() {
    return ContainerService.DEFAULT_PRIORITY;
  }

  @Override
  public void init(Container container) throws Exception {
    assetDatapointService = container.getService(AssetDatapointService.class);
  }

  @Override
  public void start(Container container) throws Exception {}

  @Override
  public void stop(Container container) throws Exception {}

  @Override
  public String getHealthStatusName() {
    return NAME;
  }

  @Override
  public Object getHealthStatus() {
    return Map.of("totalDatapoints", assetDatapointService.getDatapointsCount());
  }
}
