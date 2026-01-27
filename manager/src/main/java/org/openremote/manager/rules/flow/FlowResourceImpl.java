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
package org.openremote.manager.rules.flow;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.rules.flow.*;

public class FlowResourceImpl extends ManagerWebResource implements FlowResource {

  private static final Logger LOG = Logger.getLogger(FlowResourceImpl.class.getName());

  private final Node[] nodes;

  public FlowResourceImpl(TimerService timerService, ManagerIdentityService identityService) {
    super(timerService, identityService);
    Arrays.stream(NodeModel.values())
        .map(NodeModel::getDefinition)
        .forEach(
            n -> {
              LOG.finest("Node found: " + n.getName());
            });

    nodes =
        Arrays.stream(NodeModel.values())
            .map(NodeModel::getDefinition)
            // Filter out LOG_OUTPUT node
            .filter(definition -> !Objects.equals(definition.getName(), "LOG_OUTPUT"))
            .toArray(Node[]::new);
  }

  @Override
  public Node[] getAllNodeDefinitions(RequestParams requestParams) {
    return nodes;
  }

  @Override
  public Node[] getAllNodeDefinitionsByType(RequestParams requestParams, NodeType type) {
    return Arrays.stream(nodes).filter(n -> n.getType() == type).toArray(Node[]::new);
  }

  @Override
  public Node getNodeDefinition(RequestParams requestParams, String name) {
    return NodeModel.getDefinitionFor(name);
  }
}
