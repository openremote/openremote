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

import org.openremote.manager.rules.FlowRulesBuilder;
import org.openremote.manager.rules.RulesFacts;
import org.openremote.model.rules.flow.Node;
import org.openremote.model.rules.flow.NodeCollection;

public class NodeTriggerParameters {
  private String ruleName;
  private RulesFacts facts;
  private FlowRulesBuilder builder;
  private NodeCollection collection;
  private Node node;

  public NodeTriggerParameters(
      String ruleName,
      RulesFacts facts,
      FlowRulesBuilder builder,
      NodeCollection collection,
      Node node) {
    this.ruleName = ruleName;
    this.facts = facts;
    this.builder = builder;
    this.collection = collection;
    this.node = node;
  }

  public String getRuleName() {
    return ruleName;
  }

  public RulesFacts getFacts() {
    return facts;
  }

  public FlowRulesBuilder getBuilder() {
    return builder;
  }

  public NodeCollection getCollection() {
    return collection;
  }

  public Node getNode() {
    return node;
  }
}
