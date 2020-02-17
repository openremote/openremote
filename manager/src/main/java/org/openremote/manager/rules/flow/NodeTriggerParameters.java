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

    public NodeTriggerParameters(String ruleName, RulesFacts facts, FlowRulesBuilder builder, NodeCollection collection, Node node) {
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
