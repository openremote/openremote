/*
 * Copyright 2024, OpenRemote Inc.
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
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package org.openremote.manager.rules.flow;

import java.util.Arrays;
import java.util.logging.Logger;

import org.openremote.container.timer.TimerService;
import org.openremote.manager.security.ManagerIdentityService;
import org.openremote.manager.web.ManagerWebResource;
import org.openremote.model.http.RequestParams;
import org.openremote.model.rules.flow.FlowResource;
import org.openremote.model.rules.flow.Node;
import org.openremote.model.rules.flow.NodeType;

public class FlowResourceImpl extends ManagerWebResource implements FlowResource {

  private static final Logger LOG = Logger.getLogger(FlowResourceImpl.class.getName());

  public FlowResourceImpl(TimerService timerService, ManagerIdentityService identityService) {
    super(timerService, identityService);
    for (Node node :
        Arrays.stream(NodeModel.values()).map(NodeModel::getDefinition).toArray(Node[]::new)) {
      LOG.finest("Node found: " + node.getName());
    }
  }

  @Override
  public Node[] getAllNodeDefinitions(RequestParams requestParams) {
    return Arrays.stream(NodeModel.values()).map(NodeModel::getDefinition).toArray(Node[]::new);
  }

  @Override
  public Node[] getAllNodeDefinitionsByType(RequestParams requestParams, NodeType type) {
    return Arrays.stream(NodeModel.values())
        .filter((n) -> n.getDefinition().getType().equals(type))
        .map(NodeModel::getDefinition)
        .toArray(Node[]::new);
  }

  @Override
  public Node getNodeDefinition(RequestParams requestParams, String name) {
    return NodeModel.getDefinitionFor(name);
  }
}
